-- ============================================================
-- SMART LIBRARY MANAGEMENT SYSTEM - DATABASE SCHEMA
-- Compatible with MySQL, PostgreSQL, SQLite, and MariaDB
-- ============================================================

CREATE TABLE IF NOT EXISTS members (
    member_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    borrowing_limit INT NOT NULL DEFAULT 3
);

CREATE TABLE IF NOT EXISTS books (
    book_id INT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL
);

CREATE TABLE IF NOT EXISTS circulation_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    issue_date VARCHAR(20) NOT NULL,
    due_date VARCHAR(20) NOT NULL,
    return_date VARCHAR(20) DEFAULT NULL,
    fine DOUBLE DEFAULT 0.0,
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    reservation_date VARCHAR(20) NOT NULL,
    UNIQUE (member_id, book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);
