-- Digital Library Management System - Database Schema
-- Import this file in phpMyAdmin under your lib_sys_db database

CREATE DATABASE IF NOT EXISTS lib_sys_db;
USE lib_sys_db;

CREATE TABLE IF NOT EXISTS librarians (
    librarian_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    password VARCHAR(100) NOT NULL,
    fine_due DECIMAL(10,2) DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS books (
    book_id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    category VARCHAR(100),
    is_issued BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id VARCHAR(20) NOT NULL,
    member_id VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE DEFAULT NULL,
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

