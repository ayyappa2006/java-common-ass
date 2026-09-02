# Smart Library Management System (Java)

A comprehensive, multi-featured Library Management System implemented in Java, featuring Object-Oriented Design, Multithreading, Custom Exception Handling, In-Memory and SQL Database Management, and a Graphical User Interface (GUI) via Applet / Swing components.

---

## 📚 Features

1. **Object-Oriented Architecture**:
   - Member management (`Member`, `PremiumMember` with dynamic borrowing limits).
   - Book cataloging and inventory control (`Book`, `Inventory`).
   - Circulation tracking (`CirculationRecord`) with fine calculation for overdue items.
   - Reservation queues (`Reservation`) preventing duplicates.

2. **Custom Exception Handling**:
   - `InvalidMemberException`
   - `InvalidBookException`
   - `BookUnavailableException`
   - `DuplicateReservationException`

3. **Multithreading & Concurrency**:
   - Asynchronous book issuing tasks (`IssueTask`).
   - Automated periodic notification and fine alert engine (`NotificationTask`).

4. **Dual Interface**:
   - **CLI Console Application** (`SmartLibrary.java`) with interactive menus.
   - **GUI Interface / Applet** (`SmartLibraryApplet.java`, `applet.html`) for interactive visual management.

5. **Database Integration**:
   - Ready-to-use SQL schema (`schema.sql`) for MySQL, PostgreSQL, SQLite, and MariaDB persistence.
   - Database manager (`DBManager`) for executing CRUD operations.

---

## 📂 Project Structure

```
├── SmartLibrary.java         # Main Core CLI Application & Business Logic
├── SmartLibraryApplet.java   # Applet / GUI Visual Interface
├── applet.html               # Web Runner for Java Applet Viewer
├── schema.sql                # Relational Database Schema definition
├── .gitignore                # Git ignore rules for Java build artifacts
└── README.md                 # Project Documentation
```

---

## 🚀 How to Run

### 1. Compile the Project
Make sure you have JDK installed (Java 8+ recommended):
```bash
javac SmartLibrary.java
javac SmartLibraryApplet.java
```

### 2. Run the CLI Application
```bash
java SmartLibrary
```

### 3. Run the GUI Applet Application
Using JDK `appletviewer`:
```bash
appletviewer applet.html
```
Or run directly if modern Swing launcher is configured.

---

## 🗄️ Database Setup (Optional)

To initialize the database schema in MySQL:
```bash
mysql -u <username> -p <database_name> < schema.sql
```

---

## 👨‍💻 Author
- **G. Ayyappa Venkata Sai** ([@ayyappa2006](https://github.com/ayyappa2006))
