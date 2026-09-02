import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
// MEMBER CLASS
// ============================================================

class Member {

    protected int memberId;
    protected String name;
    protected String type;
    protected int borrowingLimit;

    protected ArrayList<Integer> borrowedBooks =
            new ArrayList<>();

    public Member(
            int memberId,
            String name,
            String type,
            int borrowingLimit) {

        this.memberId = memberId;
        this.name = name;
        this.type = type;
        this.borrowingLimit = borrowingLimit;
    }

    public int getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public int getBorrowingLimit() {
        return borrowingLimit;
    }

    public ArrayList<Integer> getBorrowedBooks() {
        return borrowedBooks;
    }

    // Polymorphic method
    public double calculateFine(int overdueDays) {
        return overdueDays * 2.0;
    }

    public void display() {

        System.out.println(
                "ID: " + memberId +
                " | Name: " + name +
                " | Type: " + type +
                " | Borrowed: " +
                borrowedBooks.size() +
                "/" + borrowingLimit
        );
    }
}

// ============================================================
// PREMIUM MEMBER
// ============================================================

class PremiumMember extends Member {

    public PremiumMember(
            int memberId,
            String name) {

        super(
                memberId,
                name,
                "Premium",
                5
        );
    }

    @Override
    public double calculateFine(int overdueDays) {
        return overdueDays * 1.0;
    }
}

// ============================================================
// BOOK CLASS
// ============================================================

class Book {

    private int bookId;
    private String title;
    private String author;
    private String category;

    private int totalCopies;
    private int availableCopies;

    public Book(
            int bookId,
            String title,
            String author,
            String category,
            int totalCopies) {

        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.category = category;

        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public synchronized int getAvailableCopies() {
        return availableCopies;
    }

    // ========================================================
    // SYNCHRONIZED ISSUE OPERATION
    // ========================================================

    public synchronized boolean issueCopy() {

        if (availableCopies > 0) {

            availableCopies--;

            return true;
        }

        return false;
    }

    // ========================================================
    // SYNCHRONIZED RETURN OPERATION
    // ========================================================

    public synchronized void returnCopy() {

        if (availableCopies < totalCopies) {

            availableCopies++;

            // Notify waiting threads
            notifyAll();
        }
    }

    public void display() {

        System.out.println(
                "ID: " + bookId +
                " | Title: " + title +
                " | Author: " + author +
                " | Category: " + category +
                " | Available: " +
                availableCopies +
                "/" + totalCopies
        );
    }
}

// ============================================================
// RESERVATION CLASS
// ============================================================

class Reservation {

    private int memberId;
    private int bookId;
    private LocalDate reservationDate;

    public Reservation(
            int memberId,
            int bookId) {

        this.memberId = memberId;
        this.bookId = bookId;
        this.reservationDate = LocalDate.now();
    }

    public int getMemberId() {
        return memberId;
    }

    public int getBookId() {
        return bookId;
    }

    public void display() {

        System.out.println(
                "Member ID: " + memberId +
                " | Book ID: " + bookId +
                " | Reservation Date: " +
                reservationDate
        );
    }
}

// ============================================================
// INVENTORY CLASS
// ============================================================

class Inventory {

    // Hashtable is synchronized
    private Hashtable<Integer, Book> books =
            new Hashtable<>();

    public synchronized void addBook(Book book) {

        books.put(
                book.getBookId(),
                book
        );
    }

    public synchronized Book getBook(int bookId) {

        return books.get(bookId);
    }

    public synchronized Collection<Book> getAllBooks() {

        return new ArrayList<>(
                books.values()
        );
    }

    public void displayInventory() {

        System.out.println(
                "\n========== INVENTORY =========="
        );

        for (Book book : getAllBooks()) {

            book.display();
        }
    }
}

// ============================================================
// NOTIFICATION CLASS
// ============================================================

class Notification {

    public void sendDueNotification(
            Member member,
            Book book) {

        System.out.println(
                "[NOTIFICATION] " +
                "Member " +
                member.getName() +
                ": Book \"" +
                book.getTitle() +
                "\" is due soon."
        );
    }

    public void sendOverdueNotification(
            Member member,
            Book book,
            double fine) {

        System.out.println(
                "[OVERDUE NOTIFICATION] " +
                "Member " +
                member.getName() +
                ": Book \"" +
                book.getTitle() +
                "\" is overdue. " +
                "Fine = ₹" +
                String.format("%.2f", fine)
        );
    }
}

// ============================================================
// CIRCULATION RECORD
// ============================================================

class CirculationRecord {

    private int memberId;
    private int bookId;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    private double fine;

    public CirculationRecord(
            int memberId,
            int bookId,
            LocalDate issueDate,
            LocalDate dueDate) {

        this.memberId = memberId;
        this.bookId = bookId;

        this.issueDate = issueDate;
        this.dueDate = dueDate;

        this.fine = 0;
    }

    public void returnBook(
            LocalDate returnDate,
            double fine) {

        this.returnDate = returnDate;
        this.fine = fine;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public double getFine() {
        return fine;
    }

    public int getMemberId() {
        return memberId;
    }

    public int getBookId() {
        return bookId;
    }

    public void display() {

        System.out.println(
                "Member: " + memberId +
                " | Book: " + bookId +
                " | Issue: " + issueDate +
                " | Due: " + dueDate +
                " | Return: " +
                (returnDate == null
                        ? "Not Returned"
                        : returnDate) +
                " | Fine: ₹" +
                String.format("%.2f", fine)
        );
    }
}

// ============================================================
// NOTIFICATION THREAD
// ============================================================

class NotificationTask extends Thread {

    private final Notification notification;
    private final ArrayList<CirculationRecord> records;
    private final HashMap<Integer, Member> members;
    private final Inventory inventory;

    public NotificationTask(
            Notification notification,
            ArrayList<CirculationRecord> records,
            HashMap<Integer, Member> members,
            Inventory inventory) {

        this.notification = notification;
        this.records = records;
        this.members = members;
        this.inventory = inventory;
    }

    @Override
    public void run() {

        System.out.println(
                "\nNotification Thread: " +
                "Checking due dates..."
        );

        LocalDate today =
                LocalDate.now();

        int notificationCount = 0;

        synchronized (records) {

            for (CirculationRecord record :
                    records) {

                if (!record.isReturned()) {

                    Member member =
                            members.get(
                                    record.getMemberId()
                            );

                    Book book =
                            inventory.getBook(
                                    record.getBookId()
                            );

                    if (member != null &&
                            book != null) {

                        if (today.isAfter(
                                record.getDueDate())) {

                            long overdueDays =
                                    ChronoUnit.DAYS.between(
                                            record.getDueDate(),
                                            today
                                    );

                            double fine =
                                    member.calculateFine(
                                            (int) overdueDays
                                    );

                            notification
                                    .sendOverdueNotification(
                                            member,
                                            book,
                                            fine
                                    );

                            notificationCount++;

                        } else {

                            notification
                                    .sendDueNotification(
                                            member,
                                            book
                                    );

                            notificationCount++;
                        }
                    }
                }
            }
        }

        System.out.println(
                "Notification Thread: " +
                notificationCount +
                " notification(s) processed."
        );
    }
}

// ============================================================
// ISSUE THREAD
// ============================================================

class IssueTask extends Thread {

    private final Book book;

    public IssueTask(Book book) {
        this.book = book;
    }

    @Override
    public void run() {

        System.out.println(
                "\nIssue Thread: Checking inventory..."
        );

        synchronized (book) {

            if (book.issueCopy()) {

                System.out.println(
                        "Issue Thread: " +
                        "Book issued successfully."
                );

                System.out.println(
                        "Issue Thread: Available copies = " +
                        book.getAvailableCopies()
                );

            } else {

                System.out.println(
                        "Issue Thread: " +
                        "Book unavailable."
                );
            }
        }
    }
}

// ============================================================
// SMART LIBRARY MAIN CLASS
// ============================================================

public class SmartLibrary {

    private static final Scanner scanner =
            new Scanner(System.in);

    // ========================================================
    // COLLECTIONS
    // ========================================================

    private static final HashMap<Integer, Member> members =
            new HashMap<>();

    private static final ArrayList<Book> books =
            new ArrayList<>();

    private static final ArrayList<Reservation> reservations =
            new ArrayList<>();

    private static final ArrayList<CirculationRecord>
            circulationRecords =
            new ArrayList<>();

    private static final Set<String> reservationKeys =
            new HashSet<>();

    private static final Inventory inventory =
            new Inventory();

    private static final Notification notification =
            new Notification();

    // ========================================================
    // REGISTER MEMBER
    // ========================================================

    private static void registerMember() {

        try {

            System.out.print(
                    "Enter Member ID: "
            );

            int id = scanner.nextInt();
            scanner.nextLine();

            if (members.containsKey(id)) {

                throw new InvalidMemberException(
                        "Member ID already exists."
                );
            }

            System.out.print(
                    "Enter Member Name: "
            );

            String name =
                    scanner.nextLine();

            System.out.print(
                    "Enter Membership Type " +
                    "(Regular/Premium): "
            );

            String type =
                    scanner.nextLine();

            Member member;

            if (type.equalsIgnoreCase(
                    "Premium")) {

                member =
                        new PremiumMember(
                                id,
                                name
                        );

            } else {

                member =
                        new Member(
                                id,
                                name,
                                "Regular",
                                3
                        );
            }

            members.put(id, member);

            System.out.println(
                    "Member registered successfully."
            );

        } catch (InvalidMemberException e) {

            System.out.println(
                    "Error: " +
                    e.getMessage()
            );

        } catch (InputMismatchException e) {

            System.out.println(
                    "Error: Please enter a valid ID."
            );

            scanner.nextLine();
        }
    }

    // ========================================================
    // ADD BOOK
    // ========================================================

    private static void addBook() {

        try {

            System.out.print(
                    "Enter Book ID: "
            );

            int id = scanner.nextInt();
            scanner.nextLine();

            if (inventory.getBook(id) != null) {

                throw new InvalidBookException(
                        "Book ID already exists."
                );
            }

            System.out.print(
                    "Enter Book Title: "
            );

            String title =
                    scanner.nextLine();

            System.out.print(
                    "Enter Author: "
            );

            String author =
                    scanner.nextLine();

            System.out.print(
                    "Enter Category: "
            );

            String category =
                    scanner.nextLine();

            System.out.print(
                    "Enter Number of Copies: "
            );

            int copies =
                    scanner.nextInt();

            if (copies <= 0) {

                throw new InvalidBookException(
                        "Copies must be greater than zero."
                );
            }

            Book book =
                    new Book(
                            id,
                            title,
                            author,
                            category,
                            copies
                    );

            books.add(book);

            inventory.addBook(book);

            System.out.println(
                    "Book added successfully."
            );

        } catch (InvalidBookException e) {

            System.out.println(
                    "Error: " +
                    e.getMessage()
            );

        } catch (InputMismatchException e) {

            System.out.println(
                    "Error: Invalid input."
            );

            scanner.nextLine();
        }
    }

    // ========================================================
    // DISPLAY BOOKS
    // ========================================================

    private static void displayBooks() {

        System.out.println(
                "\n========== BOOK LIST =========="
        );

        if (books.isEmpty()) {

            System.out.println(
                    "No books available."
            );

            return;
        }

        Iterator<Book> iterator =
                books.iterator();

        while (iterator.hasNext()) {

            Book book =
                    iterator.next();

            book.display();
        }
    }

    // ========================================================
    // SEARCH BOOK
    // ========================================================

    private static void searchBook() {

        System.out.print(
                "Enter Book ID: "
        );

        int id = scanner.nextInt();

        Book book =
                inventory.getBook(id);

        if (book == null) {

            System.out.println(
                    "Book not found."
            );

        } else {

            book.display();
        }
    }

    // ========================================================
    // SEARCH MEMBER
    // ========================================================

    private static void searchMember() {

        System.out.print(
                "Enter Member ID: "
        );

        int id = scanner.nextInt();

        Member member =
                members.get(id);

        if (member == null) {

            System.out.println(
                    "Member not found."
            );

        } else {

            member.display();
        }
    }

    // ========================================================
    // ISSUE BOOK
    // ========================================================

    private static void issueBook() {

        try {

            System.out.print(
                    "Enter Member ID: "
            );

            int memberId =
                    scanner.nextInt();

            Member member =
                    members.get(memberId);

            if (member == null) {

                throw new InvalidMemberException(
                        "Invalid Member ID."
                );
            }

            if (member.getBorrowedBooks()
                    .size()
                    >= member.getBorrowingLimit()) {

                throw new BookUnavailableException(
                        "Borrowing limit reached."
                );
            }

            System.out.print(
                    "Enter Book ID: "
            );

            int bookId =
                    scanner.nextInt();

            Book book =
                    inventory.getBook(bookId);

            if (book == null) {

                throw new InvalidBookException(
                        "Invalid Book ID."
                );
            }

            synchronized (book) {

                if (!book.issueCopy()) {

                    throw new BookUnavailableException(
                            "Book is currently unavailable."
                    );
                }

                member.getBorrowedBooks()
                        .add(bookId);

                LocalDate issueDate =
                        LocalDate.now();

                LocalDate dueDate =
                        issueDate.plusDays(14);

                CirculationRecord record =
                        new CirculationRecord(
                                memberId,
                                bookId,
                                issueDate,
                                dueDate
                        );

                synchronized (circulationRecords) {

                    circulationRecords.add(
                            record
                    );
                }

                System.out.println(
                        "\nBook issued successfully."
                );

                System.out.println(
                        "Book: " +
                        book.getTitle()
                );

                System.out.println(
                        "Issue Date: " +
                        issueDate
                );

                System.out.println(
                        "Due Date: " +
                        dueDate
                );

                System.out.println(
                        "Available Copies: " +
                        book.getAvailableCopies()
                );
            }

        } catch (
                InvalidMemberException |
                InvalidBookException |
                BookUnavailableException e) {

            System.out.println(
                    "Error: " +
                    e.getMessage()
            );

        } catch (InputMismatchException e) {

            System.out.println(
                    "Error: Invalid input."
            );

            scanner.nextLine();
        }
    }

    // ========================================================
    // RETURN BOOK
    // ========================================================

    private static void returnBook() {

        System.out.print(
                "Enter Member ID: "
        );

        int memberId =
                scanner.nextInt();

        System.out.print(
                "Enter Book ID: "
        );

        int bookId =
                scanner.nextInt();

        Member member =
                members.get(memberId);

        Book book =
                inventory.getBook(bookId);

        if (member == null) {

            System.out.println(
                    "Invalid Member ID."
            );

            return;
        }

        if (book == null) {

            System.out.println(
                    "Invalid Book ID."
            );

            return;
        }

        if (!member.getBorrowedBooks()
                .contains(bookId)) {

            System.out.println(
                    "This book was not borrowed by the member."
            );

            return;
        }

        synchronized (circulationRecords) {

            for (CirculationRecord record :
                    circulationRecords) {

                if (record.getMemberId()
                        == memberId &&
                        record.getBookId()
                        == bookId &&
                        !record.isReturned()) {

                    LocalDate today =
                            LocalDate.now();

                    long overdueDays =
                            ChronoUnit.DAYS.between(
                                    record.getDueDate(),
                                    today
                            );

                    if (overdueDays < 0) {
                        overdueDays = 0;
                    }

                    double fine =
                            member.calculateFine(
                                    (int) overdueDays
                            );

                    record.returnBook(
                            today,
                            fine
                    );

                    member.getBorrowedBooks()
                            .remove(
                                    Integer.valueOf(
                                            bookId
                                    )
                            );

                    book.returnCopy();

                    System.out.println(
                            "\nBook returned successfully."
                    );

                    System.out.println(
                            "Return Date: " +
                            today
                    );

                    System.out.println(
                            "Overdue Days: " +
                            overdueDays
                    );

                    System.out.println(
                            "Fine: ₹" +
                            String.format(
                                    "%.2f",
                                    fine
                            )
                    );

                    System.out.println(
                            "Available Copies: " +
                            book.getAvailableCopies()
                    );

                    processNextReservation(bookId);

                    return;
                }
            }
        }

        System.out.println(
                "Circulation record not found."
        );
    }

    // ========================================================
    // RESERVE BOOK
    // ========================================================

    private static void reserveBook() {

        try {

            System.out.print(
                    "Enter Member ID: "
            );

            int memberId =
                    scanner.nextInt();

            System.out.print(
                    "Enter Book ID: "
            );

            int bookId =
                    scanner.nextInt();

            if (!members.containsKey(
                    memberId)) {

                throw new InvalidMemberException(
                        "Invalid Member ID."
                );
            }

            Book book =
                    inventory.getBook(bookId);

            if (book == null) {

                throw new InvalidBookException(
                        "Invalid Book ID."
                );
            }

            if (book.getAvailableCopies() > 0) {

                System.out.println(
                        "Book is available. " +
                        "You can issue it directly."
                );

                return;
            }

            String key =
                    memberId + "-" + bookId;

            synchronized (reservationKeys) {

                if (reservationKeys
                        .contains(key)) {

                    throw new DuplicateReservationException(
                            "Duplicate reservation is not allowed."
                    );
                }

                Reservation reservation =
                        new Reservation(
                                memberId,
                                bookId
                        );

                reservations.add(
                        reservation
                );

                reservationKeys.add(
                        key
                );
            }

            System.out.println(
                    "Book unavailable."
            );

            System.out.println(
                    "Member added to reservation waitlist."
            );

            System.out.println(
                    "Queue Position: " +
                    reservations.size()
            );

        } catch (
                InvalidMemberException |
                InvalidBookException |
                DuplicateReservationException e) {

            System.out.println(
                    "Error: " +
                    e.getMessage()
            );
        }
    }

    // ========================================================
    // CANCEL RESERVATION
    // ========================================================

    private static void cancelReservation() {

        System.out.print(
                "Enter Member ID: "
        );

        int memberId =
                scanner.nextInt();

        System.out.print(
                "Enter Book ID: "
        );

        int bookId =
                scanner.nextInt();

        String key =
                memberId + "-" + bookId;

        synchronized (reservations) {

            Iterator<Reservation> iterator =
                    reservations.iterator();

            while (iterator.hasNext()) {

                Reservation reservation =
                        iterator.next();

                if (reservation.getMemberId()
                        == memberId &&
                        reservation.getBookId()
                        == bookId) {

                    iterator.remove();

                    reservationKeys.remove(
                            key
                    );

                    System.out.println(
                            "Reservation cancelled successfully."
                    );

                    return;
                }
            }
        }

        System.out.println(
                "Reservation not found."
        );
    }

    // ========================================================
    // PROCESS NEXT RESERVATION
    // ========================================================

    private static void processNextReservation(
            int bookId) {

        synchronized (reservations) {

            Iterator<Reservation> iterator =
                    reservations.iterator();

            while (iterator.hasNext()) {

                Reservation reservation =
                        iterator.next();

                if (reservation.getBookId()
                        == bookId) {

                    Member member =
                            members.get(
                                    reservation.getMemberId()
                            );

                    Book book =
                            inventory.getBook(
                                    bookId
                            );

                    if (member != null &&
                            book != null) {

                        System.out.println(
                                "\nReservation Alert:"
                        );

                        System.out.println(
                                "Member " +
                                member.getName() +
                                " is next in the waitlist."
                        );

                        System.out.println(
                                "Book \"" +
                                book.getTitle() +
                                "\" is now available."
                        );
                    }

                    return;
                }
            }
        }
    }

    // ========================================================
    // DISPLAY RESERVATIONS
    // ========================================================

    private static void displayReservations() {

        System.out.println(
                "\n========== RESERVATION WAITLIST =========="
        );

        synchronized (reservations) {

            if (reservations.isEmpty()) {

                System.out.println(
                        "No reservations."
                );

                return;
            }

            int position = 1;

            for (Reservation reservation :
                    reservations) {

                System.out.print(
                        "Position " +
                        position +
                        " : "
                );

                reservation.display();

                position++;
            }
        }
    }

    // ========================================================
    // FINE REPORT
    // ========================================================

    private static void fineReport() {

        System.out.println(
                "\n========== FINE REPORT =========="
        );

        double totalFine = 0;

        synchronized (circulationRecords) {

            for (CirculationRecord record :
                    circulationRecords) {

                if (record.getFine() > 0) {

                    record.display();

                    totalFine +=
                            record.getFine();
                }
            }
        }

        System.out.println(
                "Total Fine: ₹" +
                String.format(
                        "%.2f",
                        totalFine
                )
        );
    }

    // ========================================================
    // OVERDUE REPORT
    // ========================================================

    private static void overdueReport() {

        System.out.println(
                "\n========== OVERDUE REPORT =========="
        );

        boolean found = false;

        LocalDate today =
                LocalDate.now();

        synchronized (circulationRecords) {

            for (CirculationRecord record :
                    circulationRecords) {

                if (!record.isReturned() &&
                        today.isAfter(
                                record.getDueDate())) {

                    record.display();

                    found = true;
                }
            }
        }

        if (!found) {

            System.out.println(
                    "No overdue books."
            );
        }
    }

    // ========================================================
    // INVENTORY REPORT
    // ========================================================

    private static void inventoryReport() {

        int total = 0;
        int available = 0;

        for (Book book :
                inventory.getAllBooks()) {

            total +=
                    book.getTotalCopies();

            available +=
                    book.getAvailableCopies();
        }

        int issued =
                total - available;

        double utilization = 0;

        if (total > 0) {

            utilization =
                    ((double) issued /
                            total) * 100;
        }

        System.out.println(
                "\n========== INVENTORY REPORT =========="
        );

        System.out.println(
                "Total Copies     : " +
                total
        );

        System.out.println(
                "Issued Copies    : " +
                issued
        );

        System.out.println(
                "Available Copies : " +
                available
        );

        System.out.printf(
                "Utilization      : %.2f%%%n",
                utilization
        );
    }

    // ========================================================
    // CIRCULATION REPORT
    // ========================================================

    private static void circulationReport() {

        System.out.println(
                "\n========== CIRCULATION REPORT =========="
        );

        synchronized (circulationRecords) {

            if (circulationRecords.isEmpty()) {

                System.out.println(
                        "No circulation records."
                );

                return;
            }

            for (CirculationRecord record :
                    circulationRecords) {

                record.display();
            }
        }
    }

    // ========================================================
    // SEND NOTIFICATIONS
    // ========================================================

    private static void sendNotifications() {

        NotificationTask task =
                new NotificationTask(
                        notification,
                        circulationRecords,
                        members,
                        inventory
                );

        task.setPriority(
                Thread.NORM_PRIORITY
        );

        task.start();

        try {

            task.join();

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            System.out.println(
                    "Notification task interrupted."
            );
        }
    }

    // ========================================================
    // MULTITHREADING DEMONSTRATION
    // ========================================================

    private static void threadingDemo() {

        if (books.isEmpty()) {

            System.out.println(
                    "Add a book first."
            );

            return;
        }

        Book book =
                books.get(0);

        System.out.println(
                "\n========== MULTITHREADING TEST =========="
        );

        System.out.println(
                "Initial Available Copies: " +
                book.getAvailableCopies()
        );

        IssueTask issueThread =
                new IssueTask(book);

        NotificationTask notificationThread =
                new NotificationTask(
                        notification,
                        circulationRecords,
                        members,
                        inventory
                );

        // Thread priorities
        issueThread.setPriority(
                Thread.MAX_PRIORITY
        );

        notificationThread.setPriority(
                Thread.NORM_PRIORITY
        );

        System.out.println(
                "Issue Thread Priority: " +
                issueThread.getPriority()
        );

        System.out.println(
                "Notification Thread Priority: " +
                notificationThread.getPriority()
        );

        issueThread.start();
        notificationThread.start();

        try {

            issueThread.join();
            notificationThread.join();

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            System.out.println(
                    "Thread execution interrupted."
            );
        }

        System.out.println(
                "\nConcurrent tasks completed safely."
        );

        System.out.println(
                "Final Available Copies: " +
                book.getAvailableCopies()
        );
    }

    // ========================================================
    // MAIN MENU
    // ========================================================

    public static void main(String[] args) {

        int choice = 0;

        do {

            System.out.println(
                    "\n========================================"
            );

            System.out.println(
                    "       SMART LIBRARY MANAGEMENT"
            );

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "1. Register Member"
            );

            System.out.println(
                    "2. Add Book"
            );

            System.out.println(
                    "3. Display Books"
            );

            System.out.println(
                    "4. Search Book"
            );

            System.out.println(
                    "5. Search Member"
            );

            System.out.println(
                    "6. Issue Book"
            );

            System.out.println(
                    "7. Return Book"
            );

            System.out.println(
                    "8. Reserve Book"
            );

            System.out.println(
                    "9. Cancel Reservation"
            );

            System.out.println(
                    "10. View Reservations"
            );

            System.out.println(
                    "11. Fine Report"
            );

            System.out.println(
                    "12. Overdue Report"
            );

            System.out.println(
                    "13. Inventory Report"
            );

            System.out.println(
                    "14. Send Notifications"
            );

            System.out.println(
                    "15. Multithreading Demo"
            );

            System.out.println(
                    "16. Circulation Report"
            );

            System.out.println(
                    "17. Exit"
            );

            System.out.println(
                    "========================================"
            );

            System.out.print(
                    "Enter your choice: "
            );

            try {

                choice =
                        scanner.nextInt();

                switch (choice) {

                    case 1:
                        registerMember();
                        break;

                    case 2:
                        addBook();
                        break;

                    case 3:
                        displayBooks();
                        break;

                    case 4:
                        searchBook();
                        break;

                    case 5:
                        searchMember();
                        break;

                    case 6:
                        issueBook();
                        break;

                    case 7:
                        returnBook();
                        break;

                    case 8:
                        reserveBook();
                        break;

                    case 9:
                        cancelReservation();
                        break;

                    case 10:
                        displayReservations();
                        break;

                    case 11:
                        fineReport();
                        break;

                    case 12:
                        overdueReport();
                        break;

                    case 13:
                        inventoryReport();
                        break;

                    case 14:
                        sendNotifications();
                        break;

                    case 15:
                        threadingDemo();
                        break;

                    case 16:
                        circulationReport();
                        break;

                    case 17:
                        System.out.println(
                                "\nThank you for using " +
                                "Smart Library System."
                        );
                        break;

                    default:
                        System.out.println(
                                "Invalid choice. " +
                                "Please try again."
                        );
                }

            } catch (InputMismatchException e) {

                System.out.println(
                        "Error: Please enter a number."
                );

                scanner.nextLine();

                choice = 0;
            }

        } while (choice != 17);

        scanner.close();
    }
}
