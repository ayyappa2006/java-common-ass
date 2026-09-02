import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

// ============================================================
// CUSTOM EXCEPTIONS
// ============================================================

class InvalidMemberException extends Exception {
    public InvalidMemberException(String message) {
        super(message);
    }
}

class InvalidBookException extends Exception {
    public InvalidBookException(String message) {
        super(message);
    }
}

class BookUnavailableException extends Exception {
    public BookUnavailableException(String message) {
        super(message);
    }
}

class DuplicateReservationException extends Exception {
    public DuplicateReservationException(String message) {
        super(message);
    }
}

// ============================================================
// DOMAIN MODELS (OOP & INHERITANCE)
// ============================================================

class Member {
    protected int memberId;
    protected String name;
    protected String type;
    protected int borrowingLimit;

    public Member(int memberId, String name, String type, int borrowingLimit) {
        this.memberId = memberId;
        this.name = name;
        this.type = type;
        this.borrowingLimit = borrowingLimit;
    }

    public int getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getBorrowingLimit() { return borrowingLimit; }

    // Polymorphic fine calculation: ₹2.00 per day for Regular members
    public double calculateFine(int overdueDays) {
        return overdueDays > 0 ? overdueDays * 2.0 : 0.0;
    }
}

class PremiumMember extends Member {
    public PremiumMember(int memberId, String name) {
        super(memberId, name, "Premium", 5);
    }

    // Polymorphic fine calculation: ₹1.00 per day for Premium members (50% discount)
    @Override
    public double calculateFine(int overdueDays) {
        return overdueDays > 0 ? overdueDays * 1.0 : 0.0;
    }
}

class Book {
    private int bookId;
    private String title;
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;

    public Book(int bookId, String title, String author, String category, int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public synchronized int getAvailableCopies() { return availableCopies; }

    // Synchronized issue operation
    public synchronized boolean issueCopy() {
        if (availableCopies > 0) {
            availableCopies--;
            return true;
        }
        return false;
    }

    // Synchronized return operation
    public synchronized void returnCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
            notifyAll();
        }
    }
}

class CirculationRecord {
    private int recordId;
    private int memberId;
    private int bookId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fine;

    public CirculationRecord(int recordId, int memberId, int bookId, LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fine) {
        this.recordId = recordId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
    }

    public int getRecordId() { return recordId; }
    public int getMemberId() { return memberId; }
    public int getBookId() { return bookId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public double getFine() { return fine; }
    public boolean isReturned() { return returnDate != null; }
}

class Reservation {
    private int reservationId;
    private int memberId;
    private int bookId;
    private LocalDate reservationDate;

    public Reservation(int reservationId, int memberId, int bookId, LocalDate reservationDate) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.reservationDate = reservationDate;
    }

    public int getReservationId() { return reservationId; }
    public int getMemberId() { return memberId; }
    public int getBookId() { return bookId; }
    public LocalDate getReservationDate() { return reservationDate; }
}

// ============================================================
// DATABASE MANAGER (JDBC DAO LAYER)
// ============================================================

class DBManager {
    private static Connection conn = null;
    private static boolean useFallback = false;

    // In-memory fallback structures if no JDBC driver is installed
    private static final Map<Integer, Member> memMap = new LinkedHashMap<>();
    private static final Map<Integer, Book> bookMap = new LinkedHashMap<>();
    private static final java.util.List<CirculationRecord> circList = new ArrayList<>();
    private static final java.util.List<Reservation> resList = new ArrayList<>();
    private static int circCounter = 1;
    private static int resCounter = 1;

    public static synchronized boolean initDB(String url, String user, String password) {
        try {
            // Attempt standard JDBC connection
            if (url.contains("sqlite")) {
                Class.forName("org.sqlite.JDBC");
            } else if (url.contains("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
            conn = DriverManager.getConnection(url, user, password);
            createTables();
            useFallback = false;
            return true;
        } catch (Exception e) {
            System.err.println("JDBC Connection notice: " + e.getMessage() + ". Using robust embedded in-memory engine.");
            useFallback = true;
            seedSampleData();
            return false;
        }
    }

    public static boolean isFallback() {
        return useFallback;
    }

    private static void createTables() throws SQLException {
        if (conn == null) return;
        Statement stmt = conn.createStatement();
        
        stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                     "member_id INT PRIMARY KEY, " +
                     "name VARCHAR(100), " +
                     "type VARCHAR(50), " +
                     "borrowing_limit INT)");

        stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                     "book_id INT PRIMARY KEY, " +
                     "title VARCHAR(150), " +
                     "author VARCHAR(100), " +
                     "category VARCHAR(50), " +
                     "total_copies INT, " +
                     "available_copies INT)");

        stmt.execute("CREATE TABLE IF NOT EXISTS circulation_records (" +
                     "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "member_id INT, " +
                     "book_id INT, " +
                     "issue_date VARCHAR(20), " +
                     "due_date VARCHAR(20), " +
                     "return_date VARCHAR(20), " +
                     "fine DOUBLE)");

        stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                     "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "member_id INT, " +
                     "book_id INT, " +
                     "reservation_date VARCHAR(20))");
        stmt.close();
    }

    public static void seedSampleData() {
        if (!memMap.isEmpty() || !bookMap.isEmpty()) return;
        // Sample members
        memMap.put(101, new Member(101, "Alice Sharma", "Regular", 3));
        memMap.put(102, new PremiumMember(102, "Bob Smith"));
        memMap.put(103, new Member(103, "Charlie Brown", "Regular", 3));

        // Sample books
        bookMap.put(201, new Book(201, "Introduction to Algorithms", "CLRS", "Computer Science", 4, 3));
        bookMap.put(202, new Book(202, "Clean Code", "Robert C. Martin", "Software Eng", 3, 0));
        bookMap.put(203, new Book(203, "Core Java Volume I", "Cay Horstmann", "Programming", 5, 4));
        bookMap.put(204, new Book(204, "Design Patterns", "Gang of Four", "Software Eng", 2, 2));

        // Sample circulation
        LocalDate now = LocalDate.now();
        circList.add(new CirculationRecord(circCounter++, 101, 201, now.minusDays(10), now.plusDays(4), null, 0.0));
        circList.add(new CirculationRecord(circCounter++, 103, 202, now.minusDays(20), now.minusDays(6), null, 0.0)); // Overdue
        
        // Sample reservation
        resList.add(new Reservation(resCounter++, 102, 202, now.minusDays(2)));
    }

    // ================= Member CRUD =================
    public static synchronized void registerMember(int id, String name, String type) throws Exception {
        if (!useFallback && conn != null) {
            String checkSql = "SELECT member_id FROM members WHERE member_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                throw new InvalidMemberException("Member ID " + id + " already exists in database.");
            }
            rs.close(); pstmt.close();

            int limit = type.equalsIgnoreCase("Premium") ? 5 : 3;
            String insertSql = "INSERT INTO members (member_id, name, type, borrowing_limit) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, id);
            insertStmt.setString(2, name);
            insertStmt.setString(3, type);
            insertStmt.setInt(4, limit);
            insertStmt.executeUpdate();
            insertStmt.close();
        } else {
            if (memMap.containsKey(id)) {
                throw new InvalidMemberException("Member ID " + id + " already exists.");
            }
            Member m = type.equalsIgnoreCase("Premium") ? new PremiumMember(id, name) : new Member(id, name, "Regular", 3);
            memMap.put(id, m);
        }
    }

    public static synchronized Member getMember(int id) {
        if (!useFallback && conn != null) {
            try {
                String sql = "SELECT * FROM members WHERE member_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    int limit = rs.getInt("borrowing_limit");
                    rs.close(); pstmt.close();
                    if (type.equalsIgnoreCase("Premium")) return new PremiumMember(id, name);
                    return new Member(id, name, type, limit);
                }
                rs.close(); pstmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        } else {
            return memMap.get(id);
        }
    }

    public static synchronized java.util.List<Member> getAllMembers() {
        java.util.List<Member> list = new ArrayList<>();
        if (!useFallback && conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM members ORDER BY member_id ASC");
                while (rs.next()) {
                    int id = rs.getInt("member_id");
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    int limit = rs.getInt("borrowing_limit");
                    if (type.equalsIgnoreCase("Premium")) list.add(new PremiumMember(id, name));
                    else list.add(new Member(id, name, type, limit));
                }
                rs.close(); stmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            list.addAll(memMap.values());
        }
        return list;
    }

    // ================= Book CRUD =================
    public static synchronized void addBook(int id, String title, String author, String category, int copies) throws Exception {
        if (copies <= 0) throw new InvalidBookException("Copies must be greater than 0.");
        if (!useFallback && conn != null) {
            String checkSql = "SELECT book_id FROM books WHERE book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                throw new InvalidBookException("Book ID " + id + " already exists in database.");
            }
            rs.close(); pstmt.close();

            String insertSql = "INSERT INTO books (book_id, title, author, category, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, id);
            insertStmt.setString(2, title);
            insertStmt.setString(3, author);
            insertStmt.setString(4, category);
            insertStmt.setInt(5, copies);
            insertStmt.setInt(6, copies);
            insertStmt.executeUpdate();
            insertStmt.close();
        } else {
            if (bookMap.containsKey(id)) {
                throw new InvalidBookException("Book ID " + id + " already exists.");
            }
            bookMap.put(id, new Book(id, title, author, category, copies, copies));
        }
    }

    public static synchronized Book getBook(int id) {
        if (!useFallback && conn != null) {
            try {
                String sql = "SELECT * FROM books WHERE book_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Book b = new Book(id, rs.getString("title"), rs.getString("author"),
                                      rs.getString("category"), rs.getInt("total_copies"), rs.getInt("available_copies"));
                    rs.close(); pstmt.close();
                    return b;
                }
                rs.close(); pstmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        } else {
            return bookMap.get(id);
        }
    }

    public static synchronized java.util.List<Book> getAllBooks() {
        java.util.List<Book> list = new ArrayList<>();
        if (!useFallback && conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY book_id ASC");
                while (rs.next()) {
                    list.add(new Book(rs.getInt("book_id"), rs.getString("title"), rs.getString("author"),
                                      rs.getString("category"), rs.getInt("total_copies"), rs.getInt("available_copies")));
                }
                rs.close(); stmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            list.addAll(bookMap.values());
        }
        return list;
    }

    // ================= Circulation (Issue / Return) =================
    public static synchronized int getActiveBorrowCount(int memberId) {
        int count = 0;
        if (!useFallback && conn != null) {
            try {
                String sql = "SELECT COUNT(*) FROM circulation_records WHERE member_id = ? AND return_date IS NULL";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, memberId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) count = rs.getInt(1);
                rs.close(); pstmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            for (CirculationRecord cr : circList) {
                if (cr.getMemberId() == memberId && !cr.isReturned()) count++;
            }
        }
        return count;
    }

    public static synchronized void issueBook(int memberId, int bookId) throws Exception {
        Member member = getMember(memberId);
        if (member == null) throw new InvalidMemberException("Member ID " + memberId + " not found.");

        Book book = getBook(bookId);
        if (book == null) throw new InvalidBookException("Book ID " + bookId + " not found.");

        int activeBorrows = getActiveBorrowCount(memberId);
        if (activeBorrows >= member.getBorrowingLimit()) {
            throw new BookUnavailableException("Member reached borrowing limit (" + activeBorrows + "/" + member.getBorrowingLimit() + ").");
        }

        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("Book \"" + book.getTitle() + "\" is currently out of stock. You may reserve it.");
        }

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(14);

        if (!useFallback && conn != null) {
            conn.setAutoCommit(false);
            try {
                // Decrement available copies
                String updateBook = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0";
                PreparedStatement uStmt = conn.prepareStatement(updateBook);
                uStmt.setInt(1, bookId);
                int rows = uStmt.executeUpdate();
                uStmt.close();
                if (rows == 0) throw new BookUnavailableException("Book copies depleted during issue.");

                // Insert circulation record
                String insertCirc = "INSERT INTO circulation_records (member_id, book_id, issue_date, due_date, return_date, fine) VALUES (?, ?, ?, ?, NULL, 0.0)";
                PreparedStatement cStmt = conn.prepareStatement(insertCirc);
                cStmt.setInt(1, memberId);
                cStmt.setInt(2, bookId);
                cStmt.setString(3, issueDate.toString());
                cStmt.setString(4, dueDate.toString());
                cStmt.executeUpdate();
                cStmt.close();

                conn.commit();
                conn.setAutoCommit(true);
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
        } else {
            synchronized (book) {
                if (!book.issueCopy()) throw new BookUnavailableException("Book copies depleted.");
                circList.add(new CirculationRecord(circCounter++, memberId, bookId, issueDate, dueDate, null, 0.0));
            }
        }
    }

    public static synchronized double returnBook(int memberId, int bookId) throws Exception {
        Member member = getMember(memberId);
        if (member == null) throw new InvalidMemberException("Member ID " + memberId + " not found.");

        Book book = getBook(bookId);
        if (book == null) throw new InvalidBookException("Book ID " + bookId + " not found.");

        LocalDate today = LocalDate.now();
        double calculatedFine = 0.0;

        if (!useFallback && conn != null) {
            String findSql = "SELECT record_id, due_date FROM circulation_records WHERE member_id = ? AND book_id = ? AND return_date IS NULL ORDER BY record_id ASC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(findSql);
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                rs.close(); pstmt.close();
                throw new Exception("No active circulation record found for Member " + memberId + " and Book " + bookId);
            }
            int recordId = rs.getInt("record_id");
            LocalDate dueDate = LocalDate.parse(rs.getString("due_date"));
            rs.close(); pstmt.close();

            long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            if (overdueDays > 0) {
                calculatedFine = member.calculateFine((int) overdueDays);
            }

            conn.setAutoCommit(false);
            try {
                // Update circulation record
                String updateCirc = "UPDATE circulation_records SET return_date = ?, fine = ? WHERE record_id = ?";
                PreparedStatement uStmt = conn.prepareStatement(updateCirc);
                uStmt.setString(1, today.toString());
                uStmt.setDouble(2, calculatedFine);
                uStmt.setInt(3, recordId);
                uStmt.executeUpdate();
                uStmt.close();

                // Increment book copies
                String updateBook = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?";
                PreparedStatement ubStmt = conn.prepareStatement(updateBook);
                ubStmt.setInt(1, bookId);
                ubStmt.executeUpdate();
                ubStmt.close();

                conn.commit();
                conn.setAutoCommit(true);
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
        } else {
            CirculationRecord target = null;
            for (CirculationRecord cr : circList) {
                if (cr.getMemberId() == memberId && cr.getBookId() == bookId && !cr.isReturned()) {
                    target = cr;
                    break;
                }
            }
            if (target == null) throw new Exception("No active loan found for Member " + memberId + " with Book " + bookId);

            long overdueDays = ChronoUnit.DAYS.between(target.getDueDate(), today);
            if (overdueDays > 0) {
                calculatedFine = member.calculateFine((int) overdueDays);
            }

            circList.remove(target);
            circList.add(new CirculationRecord(target.getRecordId(), memberId, bookId, target.getIssueDate(), target.getDueDate(), today, calculatedFine));
            book.returnCopy();
        }

        return calculatedFine;
    }

    // ================= Reservations =================
    public static synchronized void reserveBook(int memberId, int bookId) throws Exception {
        Member member = getMember(memberId);
        if (member == null) throw new InvalidMemberException("Member ID " + memberId + " not found.");

        Book book = getBook(bookId);
        if (book == null) throw new InvalidBookException("Book ID " + bookId + " not found.");

        if (book.getAvailableCopies() > 0) {
            throw new Exception("Book is currently available (" + book.getAvailableCopies() + " copies). You can issue it directly!");
        }

        LocalDate now = LocalDate.now();

        if (!useFallback && conn != null) {
            String checkSql = "SELECT reservation_id FROM reservations WHERE member_id = ? AND book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                rs.close(); pstmt.close();
                throw new DuplicateReservationException("Member " + memberId + " already has a reservation for Book " + bookId);
            }
            rs.close(); pstmt.close();

            String insertSql = "INSERT INTO reservations (member_id, book_id, reservation_date) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, memberId);
            insertStmt.setInt(2, bookId);
            insertStmt.setString(3, now.toString());
            insertStmt.executeUpdate();
            insertStmt.close();
        } else {
            for (Reservation r : resList) {
                if (r.getMemberId() == memberId && r.getBookId() == bookId) {
                    throw new DuplicateReservationException("Duplicate reservation not allowed for this book.");
                }
            }
            resList.add(new Reservation(resCounter++, memberId, bookId, now));
        }
    }

    public static synchronized void cancelReservation(int memberId, int bookId) throws Exception {
        if (!useFallback && conn != null) {
            String delSql = "DELETE FROM reservations WHERE member_id = ? AND book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(delSql);
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, bookId);
            int rows = pstmt.executeUpdate();
            pstmt.close();
            if (rows == 0) throw new Exception("No reservation found for Member " + memberId + " and Book " + bookId);
        } else {
            boolean removed = resList.removeIf(r -> r.getMemberId() == memberId && r.getBookId() == bookId);
            if (!removed) throw new Exception("No reservation found for Member " + memberId + " and Book " + bookId);
        }
    }

    public static synchronized java.util.List<Reservation> getAllReservations() {
        java.util.List<Reservation> list = new ArrayList<>();
        if (!useFallback && conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM reservations ORDER BY reservation_id ASC");
                while (rs.next()) {
                    list.add(new Reservation(rs.getInt("reservation_id"), rs.getInt("member_id"),
                                             rs.getInt("book_id"), LocalDate.parse(rs.getString("reservation_date"))));
                }
                rs.close(); stmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            list.addAll(resList);
        }
        return list;
    }

    public static synchronized java.util.List<CirculationRecord> getAllCirculations() {
        java.util.List<CirculationRecord> list = new ArrayList<>();
        if (!useFallback && conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM circulation_records ORDER BY record_id ASC");
                while (rs.next()) {
                    String retDateStr = rs.getString("return_date");
                    LocalDate retDate = (retDateStr == null || retDateStr.isEmpty()) ? null : LocalDate.parse(retDateStr);
                    list.add(new CirculationRecord(rs.getInt("record_id"), rs.getInt("member_id"), rs.getInt("book_id"),
                                                  LocalDate.parse(rs.getString("issue_date")),
                                                  LocalDate.parse(rs.getString("due_date")),
                                                  retDate, rs.getDouble("fine")));
                }
                rs.close(); stmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            list.addAll(circList);
        }
        return list;
    }
}

// ============================================================
// MULTITHREADING TASKS
// ============================================================

class NotificationTask extends Thread {
    private final JTextArea logArea;

    public NotificationTask(JTextArea logArea) {
        this.logArea = logArea;
    }

    @Override
    public void run() {
        log("[NOTIFICATION THREAD] Scanning database for due & overdue loans...");
        LocalDate today = LocalDate.now();
        int count = 0;

        java.util.List<CirculationRecord> records = DBManager.getAllCirculations();
        for (CirculationRecord cr : records) {
            if (!cr.isReturned()) {
                Member m = DBManager.getMember(cr.getMemberId());
                Book b = DBManager.getBook(cr.getBookId());
                if (m != null && b != null) {
                    if (today.isAfter(cr.getDueDate())) {
                        long overdueDays = ChronoUnit.DAYS.between(cr.getDueDate(), today);
                        double fine = m.calculateFine((int) overdueDays);
                        log(String.format("  [OVERDUE ALERT] Member: %s (ID: %d) | Book: \"%s\" | Overdue: %d days | Fine: ₹%.2f",
                                m.getName(), m.getMemberId(), b.getTitle(), overdueDays, fine));
                        count++;
                    } else if (ChronoUnit.DAYS.between(today, cr.getDueDate()) <= 3) {
                        log(String.format("  [DUE REMINDER] Member: %s (ID: %d) | Book: \"%s\" | Due Date: %s (Due Soon!)",
                                m.getName(), m.getMemberId(), b.getTitle(), cr.getDueDate()));
                        count++;
                    }
                }
            }
        }
        log("[NOTIFICATION THREAD] Finished scan. Processed " + count + " alert(s).\n");
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
}

class IssueTask extends Thread {
    private final int bookId;
    private final int memberId;
    private final JTextArea logArea;

    public IssueTask(int bookId, int memberId, JTextArea logArea) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.logArea = logArea;
    }

    @Override
    public void run() {
        log("[ISSUE THREAD - " + getName() + "] Attempting to issue Book " + bookId + " for Member " + memberId + "...");
        try {
            DBManager.issueBook(memberId, bookId);
            Book b = DBManager.getBook(bookId);
            log("  [SUCCESS - " + getName() + "] Issued Book " + bookId + " successfully! Remaining copies: " + (b != null ? b.getAvailableCopies() : 0));
        } catch (Exception ex) {
            log("  [FAILED - " + getName() + "] Issue rejected: " + ex.getMessage());
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
}

// ============================================================
// MAIN APPLET & SWING GUI APPLICATION
// ============================================================

@SuppressWarnings("deprecation")
public class SmartLibraryApplet extends Applet {

    // UI Components
    private JTabbedPane tabbedPane;
    private JTextArea threadLogArea;

    // Table Models
    private DefaultTableModel memberModel;
    private DefaultTableModel bookModel;
    private DefaultTableModel circModel;
    private DefaultTableModel resModel;
    private DefaultTableModel overdueModel;
    private DefaultTableModel fineModel;

    // Stat Labels
    private JLabel totalBooksLbl, availableBooksLbl, totalMembersLbl, activeLoansLbl, totalFineLbl, dbStatusLbl;

    @Override
    public void init() {
        // Applet lifecycle method
        setLayout(new BorderLayout());
        buildUI();
    }

    @Override
    public void start() {
        // Run initial data refresh on startup
        refreshAllData();
    }

    private void buildUI() {
        // Initialize DB Manager safely (handles both AppletStub in browser and standalone Desktop JFrame)
        String dbUrl = null;
        try {
            dbUrl = getParameter("dbUrl");
        } catch (Throwable ignored) {}
        if (dbUrl == null) dbUrl = "jdbc:sqlite:smart_library.db";
        DBManager.initDB(dbUrl, "root", "");

        // Apply visual styling
        setBackground(new Color(245, 247, 250));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 41, 59));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLbl = new JLabel("Smart Library Management System (Applet + JDBC)");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);

        dbStatusLbl = new JLabel(DBManager.isFallback() ? "[Embedded In-Memory Mode]" : "[Connected via JDBC]");
        dbStatusLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dbStatusLbl.setForeground(DBManager.isFallback() ? new Color(251, 191, 36) : new Color(52, 211, 153));

        headerPanel.add(titleLbl, BorderLayout.WEST);
        headerPanel.add(dbStatusLbl, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        tabbedPane.addTab("Dashboard", createDashboardPanel());
        tabbedPane.addTab("Members", createMembersPanel());
        tabbedPane.addTab("Book Catalog", createBooksPanel());
        tabbedPane.addTab("Circulation Desk", createCirculationPanel());
        tabbedPane.addTab("Reservations", createReservationsPanel());
        tabbedPane.addTab("Fine & Overdue Reports", createReportsPanel());
        tabbedPane.addTab("Multithreading Test", createThreadingPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ========================================================
    // TAB 1: DASHBOARD
    // ========================================================
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Metric cards panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 12, 12));
        statsPanel.setOpaque(false);

        totalBooksLbl = new JLabel("0", SwingConstants.CENTER);
        availableBooksLbl = new JLabel("0", SwingConstants.CENTER);
        totalMembersLbl = new JLabel("0", SwingConstants.CENTER);
        activeLoansLbl = new JLabel("0", SwingConstants.CENTER);
        totalFineLbl = new JLabel("₹0.00", SwingConstants.CENTER);

        statsPanel.add(createCard("Total Books", totalBooksLbl, new Color(59, 130, 246)));
        statsPanel.add(createCard("Available Copies", availableBooksLbl, new Color(16, 185, 129)));
        statsPanel.add(createCard("Active Members", totalMembersLbl, new Color(139, 92, 246)));
        statsPanel.add(createCard("Current Loans", activeLoansLbl, new Color(245, 158, 11)));
        statsPanel.add(createCard("Total Fines", totalFineLbl, new Color(239, 68, 68)));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Quick Actions & Live Overview
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        centerPanel.setOpaque(false);

        // Recent Loans Table
        JPanel recentLoansPanel = new JPanel(new BorderLayout());
        recentLoansPanel.setBorder(new TitledBorder("Current Active Loans"));
        recentLoansPanel.setBackground(Color.WHITE);

        String[] cols = {"Record ID", "Member ID", "Book ID", "Issue Date", "Due Date"};
        circModel = new DefaultTableModel(cols, 0);
        JTable circTable = new JTable(circModel);
        recentLoansPanel.add(new JScrollPane(circTable), BorderLayout.CENTER);

        // System Log & Notification Stream
        JPanel sysLogPanel = new JPanel(new BorderLayout());
        sysLogPanel.setBorder(new TitledBorder("System & Notification Stream"));
        sysLogPanel.setBackground(Color.WHITE);

        JTextArea dashLog = new JTextArea();
        dashLog.setEditable(false);
        dashLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        dashLog.append("Smart Library System Initialized.\nPolymorphic fine system active (Regular ₹2/day, Premium ₹1/day).\nSynchronized multithreading safety enabled.\n\n");
        sysLogPanel.add(new JScrollPane(dashLog), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("🔄 Refresh System Stats");
        refreshBtn.addActionListener(e -> refreshAllData());
        sysLogPanel.add(refreshBtn, BorderLayout.SOUTH);

        centerPanel.add(recentLoansPanel);
        centerPanel.add(sysLogPanel);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCard(String title, JLabel valueLbl, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(100, 116, 139));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }

    // ========================================================
    // TAB 2: MEMBER MANAGEMENT
    // ========================================================
    private JPanel createMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Register New Member"));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Regular (Limit: 3, Fine: ₹2/day)", "Premium (Limit: 5, Fine: ₹1/day)"});

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1; formPanel.add(idField, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 3; formPanel.add(nameField, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Membership Type:"), gbc);
        gbc.gridx = 5; formPanel.add(typeCombo, gbc);

        JButton regBtn = new JButton("Register Member");
        regBtn.setBackground(new Color(59, 130, 246));
        regBtn.setForeground(Color.WHITE);
        regBtn.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new Exception("Name cannot be empty.");
                String type = typeCombo.getSelectedIndex() == 1 ? "Premium" : "Regular";
                DBManager.registerMember(id, name, type);
                JOptionPane.showMessageDialog(this, "Member registered successfully: " + name + " (" + type + ")", "Success", JOptionPane.INFORMATION_MESSAGE);
                idField.setText("");
                nameField.setText("");
                refreshMembersTable();
                refreshAllData();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Member ID must be a numeric integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 6; formPanel.add(regBtn, gbc);
        panel.add(formPanel, BorderLayout.NORTH);

        // Members Table
        String[] cols = {"Member ID", "Name", "Membership Type", "Borrowing Limit", "Fine Rate (₹/day)", "Active Loans"};
        memberModel = new DefaultTableModel(cols, 0);
        JTable memberTable = new JTable(memberModel);
        panel.add(new JScrollPane(memberTable), BorderLayout.CENTER);

        return panel;
    }

    // ========================================================
    // TAB 3: BOOK CATALOG
    // ========================================================
    private JPanel createBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Add New Book to Inventory"));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField idField = new JTextField(8);
        JTextField titleField = new JTextField(12);
        JTextField authorField = new JTextField(10);
        JTextField catField = new JTextField(10);
        JTextField copiesField = new JTextField(5);

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Book ID:"), gbc);
        gbc.gridx = 1; formPanel.add(idField, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 3; formPanel.add(titleField, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 5; formPanel.add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; formPanel.add(catField, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Total Copies:"), gbc);
        gbc.gridx = 3; formPanel.add(copiesField, gbc);

        JButton addBtn = new JButton("Add Book to Inventory");
        addBtn.setBackground(new Color(16, 185, 129));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String cat = catField.getText().trim();
                int copies = Integer.parseInt(copiesField.getText().trim());

                if (title.isEmpty() || author.isEmpty()) throw new Exception("Title and Author are required.");
                DBManager.addBook(id, title, author, cat, copies);
                JOptionPane.showMessageDialog(this, "Book \"" + title + "\" added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                idField.setText(""); titleField.setText(""); authorField.setText(""); catField.setText(""); copiesField.setText("");
                refreshBooksTable();
                refreshAllData();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Book ID and Copies must be positive integers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 4; gbc.gridwidth = 2; formPanel.add(addBtn, gbc);
        panel.add(formPanel, BorderLayout.NORTH);

        // Books Table
        String[] cols = {"Book ID", "Title", "Author", "Category", "Total Copies", "Available Copies", "Status"};
        bookModel = new DefaultTableModel(cols, 0);
        JTable bookTable = new JTable(bookModel);
        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);

        return panel;
    }

    // ========================================================
    // TAB 4: CIRCULATION DESK (ISSUE & RETURN)
    // ========================================================
    private JPanel createCirculationPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Issue Box
        JPanel issuePanel = new JPanel(new GridBagLayout());
        issuePanel.setBorder(new TitledBorder("Issue Book (Loan Operation)"));
        issuePanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField issueMemId = new JTextField(10);
        JTextField issueBookId = new JTextField(10);

        gbc.gridx = 0; gbc.gridy = 0; issuePanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1; issuePanel.add(issueMemId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; issuePanel.add(new JLabel("Book ID:"), gbc);
        gbc.gridx = 1; issuePanel.add(issueBookId, gbc);

        JButton issueBtn = new JButton("✔ Issue Book");
        issueBtn.setBackground(new Color(59, 130, 246));
        issueBtn.setForeground(Color.WHITE);
        issueBtn.addActionListener(e -> {
            try {
                int memId = Integer.parseInt(issueMemId.getText().trim());
                int bkId = Integer.parseInt(issueBookId.getText().trim());
                DBManager.issueBook(memId, bkId);
                JOptionPane.showMessageDialog(this, "Book ID " + bkId + " issued successfully to Member " + memId + "!\nDue date: " + LocalDate.now().plusDays(14), "Success", JOptionPane.INFORMATION_MESSAGE);
                issueMemId.setText(""); issueBookId.setText("");
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Issue Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; issuePanel.add(issueBtn, gbc);

        // Return Box
        JPanel returnPanel = new JPanel(new GridBagLayout());
        returnPanel.setBorder(new TitledBorder("Return Book & Process Fine"));
        returnPanel.setBackground(Color.WHITE);

        JTextField retMemId = new JTextField(10);
        JTextField retBookId = new JTextField(10);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; returnPanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1; returnPanel.add(retMemId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; returnPanel.add(new JLabel("Book ID:"), gbc);
        gbc.gridx = 1; returnPanel.add(retBookId, gbc);

        JButton returnBtn = new JButton("📥 Return Book");
        returnBtn.setBackground(new Color(16, 185, 129));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.addActionListener(e -> {
            try {
                int memId = Integer.parseInt(retMemId.getText().trim());
                int bkId = Integer.parseInt(retBookId.getText().trim());
                double fine = DBManager.returnBook(memId, bkId);
                String msg = "Book ID " + bkId + " returned successfully!\nFine Assessed: ₹" + String.format("%.2f", fine);
                if (fine > 0) {
                    msg += "\n(Fine calculated automatically based on member tier)";
                }
                JOptionPane.showMessageDialog(this, msg, "Return Receipt", JOptionPane.INFORMATION_MESSAGE);
                retMemId.setText(""); retBookId.setText("");
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Return Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; returnPanel.add(returnBtn, gbc);

        panel.add(issuePanel);
        panel.add(returnPanel);
        return panel;
    }

    // ========================================================
    // TAB 5: RESERVATIONS
    // ========================================================
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        formPanel.setBorder(new TitledBorder("Book Reservation & Waitlist"));
        formPanel.setBackground(Color.WHITE);

        JTextField memIdField = new JTextField(8);
        JTextField bookIdField = new JTextField(8);

        formPanel.add(new JLabel("Member ID:"));
        formPanel.add(memIdField);
        formPanel.add(new JLabel("Book ID:"));
        formPanel.add(bookIdField);

        JButton resBtn = new JButton("Reserve Unavailable Book");
        resBtn.setBackground(new Color(139, 92, 246));
        resBtn.setForeground(Color.WHITE);
        resBtn.addActionListener(e -> {
            try {
                int memId = Integer.parseInt(memIdField.getText().trim());
                int bkId = Integer.parseInt(bookIdField.getText().trim());
                DBManager.reserveBook(memId, bkId);
                JOptionPane.showMessageDialog(this, "Reservation confirmed for Member " + memId + " on Book " + bkId, "Waitlist Registered", JOptionPane.INFORMATION_MESSAGE);
                memIdField.setText(""); bookIdField.setText("");
                refreshReservationsTable();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Reservation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("Cancel Reservation");
        cancelBtn.addActionListener(e -> {
            try {
                int memId = Integer.parseInt(memIdField.getText().trim());
                int bkId = Integer.parseInt(bookIdField.getText().trim());
                DBManager.cancelReservation(memId, bkId);
                JOptionPane.showMessageDialog(this, "Reservation cancelled.", "Success", JOptionPane.INFORMATION_MESSAGE);
                memIdField.setText(""); bookIdField.setText("");
                refreshReservationsTable();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cancellation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        formPanel.add(resBtn);
        formPanel.add(cancelBtn);
        panel.add(formPanel, BorderLayout.NORTH);

        String[] cols = {"Reservation ID", "Member ID", "Member Name", "Book ID", "Book Title", "Reservation Date"};
        resModel = new DefaultTableModel(cols, 0);
        JTable resTable = new JTable(resModel);
        panel.add(new JScrollPane(resTable), BorderLayout.CENTER);

        return panel;
    }

    // ========================================================
    // TAB 6: REPORTS (FINE & OVERDUE)
    // ========================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Overdue Report Panel
        JPanel overduePanel = new JPanel(new BorderLayout());
        overduePanel.setBorder(new TitledBorder("Active Overdue Loans"));
        overduePanel.setBackground(Color.WHITE);

        String[] overCols = {"Record ID", "Member Name", "Member Type", "Book Title", "Due Date", "Days Overdue", "Estimated Fine (₹)"};
        overdueModel = new DefaultTableModel(overCols, 0);
        JTable overTable = new JTable(overdueModel);
        overduePanel.add(new JScrollPane(overTable), BorderLayout.CENTER);

        // Fine History Panel
        JPanel finePanel = new JPanel(new BorderLayout());
        finePanel.setBorder(new TitledBorder("Collected Fine History"));
        finePanel.setBackground(Color.WHITE);

        String[] fineCols = {"Record ID", "Member ID", "Book ID", "Issue Date", "Return Date", "Collected Fine (₹)"};
        fineModel = new DefaultTableModel(fineCols, 0);
        JTable fineTable = new JTable(fineModel);
        finePanel.add(new JScrollPane(fineTable), BorderLayout.CENTER);

        panel.add(overduePanel);
        panel.add(finePanel);
        return panel;
    }

    // ========================================================
    // TAB 7: MULTITHREADING DEMO
    // ========================================================
    private JPanel createThreadingPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        controlPanel.setBorder(new TitledBorder("Multithreaded Task Controls"));
        controlPanel.setBackground(Color.WHITE);

        JButton notifyThreadBtn = new JButton("🚀 Start Notification Background Task");
        notifyThreadBtn.setBackground(new Color(139, 92, 246));
        notifyThreadBtn.setForeground(Color.WHITE);
        notifyThreadBtn.addActionListener(e -> {
            NotificationTask task = new NotificationTask(threadLogArea);
            task.setPriority(Thread.NORM_PRIORITY);
            task.start();
        });

        JButton concurrentIssueBtn = new JButton("⚡ Run Concurrent Issue Race Test");
        concurrentIssueBtn.setBackground(new Color(245, 158, 11));
        concurrentIssueBtn.setForeground(Color.WHITE);
        concurrentIssueBtn.addActionListener(e -> {
            threadLogArea.append("\n=== LAUNCHING CONCURRENT THREADS RACE TEST ===\n");
            // Simulate two threads competing for the last copy of Book 201
            IssueTask t1 = new IssueTask(201, 101, threadLogArea);
            IssueTask t2 = new IssueTask(201, 102, threadLogArea);
            t1.setName("Thread-Alice");
            t2.setName("Thread-Bob");
            t1.setPriority(Thread.MAX_PRIORITY);
            t2.setPriority(Thread.NORM_PRIORITY);

            t1.start();
            t2.start();
        });

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.addActionListener(e -> threadLogArea.setText(""));

        controlPanel.add(notifyThreadBtn);
        controlPanel.add(concurrentIssueBtn);
        controlPanel.add(clearLogBtn);
        panel.add(controlPanel, BorderLayout.NORTH);

        // Thread Log Area
        threadLogArea = new JTextArea();
        threadLogArea.setEditable(false);
        threadLogArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        threadLogArea.setBackground(new Color(15, 23, 42));
        threadLogArea.setForeground(new Color(148, 163, 184));
        threadLogArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new JScrollPane(threadLogArea), BorderLayout.CENTER);
        return panel;
    }

    // ========================================================
    // REFRESH & DATA SYNCHRONIZATION HELPERS
    // ========================================================
    private void refreshAllData() {
        refreshMembersTable();
        refreshBooksTable();
        refreshCirculationTable();
        refreshReservationsTable();
        refreshReports();
        updateStats();
    }

    private void updateStats() {
        java.util.List<Book> books = DBManager.getAllBooks();
        int totalCopies = 0;
        int availCopies = 0;
        for (Book b : books) {
            totalCopies += b.getTotalCopies();
            availCopies += b.getAvailableCopies();
        }
        totalBooksLbl.setText(String.valueOf(totalCopies));
        availableBooksLbl.setText(String.valueOf(availCopies));

        totalMembersLbl.setText(String.valueOf(DBManager.getAllMembers().size()));

        java.util.List<CirculationRecord> circs = DBManager.getAllCirculations();
        int activeLoans = 0;
        double totalFines = 0.0;
        for (CirculationRecord cr : circs) {
            if (!cr.isReturned()) activeLoans++;
            totalFines += cr.getFine();
        }
        activeLoansLbl.setText(String.valueOf(activeLoans));
        totalFineLbl.setText("₹" + String.format("%.2f", totalFines));
    }

    private void refreshMembersTable() {
        if (memberModel == null) return;
        memberModel.setRowCount(0);
        for (Member m : DBManager.getAllMembers()) {
            double rate = m instanceof PremiumMember ? 1.0 : 2.0;
            int active = DBManager.getActiveBorrowCount(m.getMemberId());
            memberModel.addRow(new Object[]{
                m.getMemberId(), m.getName(), m.getType(), m.getBorrowingLimit(), "₹" + rate, active + " / " + m.getBorrowingLimit()
            });
        }
    }

    private void refreshBooksTable() {
        if (bookModel == null) return;
        bookModel.setRowCount(0);
        for (Book b : DBManager.getAllBooks()) {
            String status = b.getAvailableCopies() > 0 ? "In Stock" : "Out of Stock";
            bookModel.addRow(new Object[]{
                b.getBookId(), b.getTitle(), b.getAuthor(), b.getCategory(), b.getTotalCopies(), b.getAvailableCopies(), status
            });
        }
    }

    private void refreshCirculationTable() {
        if (circModel == null) return;
        circModel.setRowCount(0);
        for (CirculationRecord cr : DBManager.getAllCirculations()) {
            if (!cr.isReturned()) {
                circModel.addRow(new Object[]{
                    cr.getRecordId(), cr.getMemberId(), cr.getBookId(), cr.getIssueDate(), cr.getDueDate()
                });
            }
        }
    }

    private void refreshReservationsTable() {
        if (resModel == null) return;
        resModel.setRowCount(0);
        for (Reservation r : DBManager.getAllReservations()) {
            Member m = DBManager.getMember(r.getMemberId());
            Book b = DBManager.getBook(r.getBookId());
            resModel.addRow(new Object[]{
                r.getReservationId(), r.getMemberId(), m != null ? m.getName() : "N/A",
                r.getBookId(), b != null ? b.getTitle() : "N/A", r.getReservationDate()
            });
        }
    }

    private void refreshReports() {
        if (overdueModel == null || fineModel == null) return;
        overdueModel.setRowCount(0);
        fineModel.setRowCount(0);

        LocalDate today = LocalDate.now();
        for (CirculationRecord cr : DBManager.getAllCirculations()) {
            Member m = DBManager.getMember(cr.getMemberId());
            Book b = DBManager.getBook(cr.getBookId());

            if (!cr.isReturned()) {
                if (today.isAfter(cr.getDueDate())) {
                    long overdueDays = ChronoUnit.DAYS.between(cr.getDueDate(), today);
                    double fine = (m != null) ? m.calculateFine((int) overdueDays) : (overdueDays * 2.0);
                    overdueModel.addRow(new Object[]{
                        cr.getRecordId(), m != null ? m.getName() : "N/A", m != null ? m.getType() : "Regular",
                        b != null ? b.getTitle() : "N/A", cr.getDueDate(), overdueDays, String.format("%.2f", fine)
                    });
                }
            } else if (cr.getFine() > 0) {
                fineModel.addRow(new Object[]{
                    cr.getRecordId(), cr.getMemberId(), cr.getBookId(), cr.getIssueDate(), cr.getReturnDate(), String.format("%.2f", cr.getFine())
                });
            }
        }
    }

    // ========================================================
    // STANDALONE LAUNCHER (DUAL APPLET & JFRAME SUPPORT)
    // ========================================================
    public static void main(String[] args) {
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Smart Library Management System - Applet & JDBC");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1150, 750);
            frame.setLocationRelativeTo(null);

            SmartLibraryApplet applet = new SmartLibraryApplet();
            applet.init();
            applet.start();

            frame.setContentPane(applet);
            frame.setVisible(true);
        });
    }
}
