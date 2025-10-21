-- 데이터베이스 사용
USE redis_learning;

-- 상품 테이블 생성
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 테이블 생성
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주문 테이블 생성
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기 데이터 삽입 (성능 테스트용)
INSERT INTO product (name, description, price, stock_quantity, category) VALUES
     ('Laptop', 'High-Performance Gaming Laptop', 1500000.00, 50, 'Electronics'),
     ('Mouse', 'Wireless Gaming Mouse', 80000.00, 200, 'Electronics'),
     ('Keyboard', 'Mechanical Keyboard', 150000.00, 150, 'Electronics'),
     ('Monitor', '27-inch 4K Monitor', 500000.00, 80, 'Electronics'),
     ('Headset', 'Noise-Canceling Headset', 200000.00, 120, 'Electronics');

INSERT INTO user (username, email, full_name) VALUES
  ('user1', 'user1@example.com', 'John Kim'),
  ('user2', 'user2@example.com', 'Jane Lee'),
  ('user3', 'user3@example.com', 'Michael Park'),
  ('user4', 'user4@example.com', 'Sarah Jung'),
  ('user5', 'user5@example.com', 'David Choi');
