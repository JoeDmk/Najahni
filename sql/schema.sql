-- ============================================
-- NAJAHNI Database Schema
-- Business & Entrepreneurship Platform
-- ============================================

-- Create database
CREATE DATABASE IF NOT EXISTS najahni_db;
USE najahni_db;

-- ============================================
-- Drop existing tables (for clean setup)
-- ============================================
DROP TABLE IF EXISTS investment_opportunity;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS user;

-- ============================================
-- User Table
-- ============================================
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ENTREPRENEUR', 'INVESTOR') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Project Table
-- ============================================
CREATE TABLE project (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sector VARCHAR(100) NOT NULL,
    status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'FUNDED') DEFAULT 'DRAFT',
    entrepreneur_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_entrepreneur 
        FOREIGN KEY (entrepreneur_id) REFERENCES user(id) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Investment Opportunity Table
-- ============================================
CREATE TABLE investment_opportunity (
    id INT PRIMARY KEY AUTO_INCREMENT,
    amount DECIMAL(15, 2) NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED') DEFAULT 'PENDING',
    project_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_investment_project 
        FOREIGN KEY (project_id) REFERENCES project(id) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Indexes for better performance
-- ============================================
CREATE INDEX idx_user_email ON user(email);
CREATE INDEX idx_user_role ON user(role);
CREATE INDEX idx_project_entrepreneur ON project(entrepreneur_id);
CREATE INDEX idx_project_status ON project(status);
CREATE INDEX idx_project_sector ON project(sector);
CREATE INDEX idx_investment_project ON investment_opportunity(project_id);
CREATE INDEX idx_investment_status ON investment_opportunity(status);

-- ============================================
-- Sample Data (Optional)
-- ============================================
INSERT INTO user (name, email, password, role) VALUES
('Ahmed Ben Ali', 'ahmed@najahni.tn', 'password123', 'ENTREPRENEUR'),
('Fatma Trabelsi', 'fatma@najahni.tn', 'password123', 'INVESTOR'),
('Mohamed Sassi', 'mohamed@najahni.tn', 'password123', 'ENTREPRENEUR');

INSERT INTO project (title, description, sector, status, entrepreneur_id) VALUES
('TechStart Tunisia', 'A technology startup incubator', 'Technology', 'APPROVED', 1),
('Green Energy TN', 'Renewable energy solutions for homes', 'Energy', 'PENDING', 1),
('EduTech Platform', 'Online education platform for students', 'Education', 'DRAFT', 3);

INSERT INTO investment_opportunity (amount, status, project_id) VALUES
(50000.00, 'PENDING', 1),
(25000.00, 'ACCEPTED', 1),
(100000.00, 'PENDING', 2);
