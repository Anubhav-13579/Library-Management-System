USE lib_sys_db;

CREATE TABLE IF NOT EXISTS library_charges (
  transaction_id INT PRIMARY KEY,
  amount DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS library_payments (
  receipt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id VARCHAR(20) NOT NULL,
  transaction_id INT NULL UNIQUE,
  amount DECIMAL(10,2) NOT NULL,
  method VARCHAR(10) NOT NULL,
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX (paid_at),
  FOREIGN KEY (member_id) REFERENCES members(member_id),
  FOREIGN KEY (transaction_id) REFERENCES library_charges(transaction_id)
) ENGINE=InnoDB;
