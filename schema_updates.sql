-- ========================================================================
-- Database Schema Updates for SachITech Training Management System
-- ========================================================================
-- This file contains all new tables and modifications needed for:
-- 1. Fee Management System (Task 9)
-- 2. Trainer Payment Management
-- 3. Profit/Loss Reporting
-- ========================================================================

USE tms_db;

-- ========================================================================
-- 1. FEE_RECORD Table - Core fee tracking per student-course enrollment
-- ========================================================================
CREATE TABLE IF NOT EXISTS `fee_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `course_id` BIGINT,
  `total_fee_at_enrollment` DOUBLE NOT NULL,
  `amount_paid` DOUBLE DEFAULT 0,
  `pending_amount` DOUBLE NOT NULL,
  `last_transaction_date` DATE,
  `version` BIGINT,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL,
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_student_course` (`student_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================================
-- 2. FEE_TRANSACTION Table - Individual payment transactions
-- ========================================================================
CREATE TABLE IF NOT EXISTS `fee_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fee_record_id` BIGINT NOT NULL,
  `installment_amount` DOUBLE NOT NULL,
  `payment_date` DATE NOT NULL,
  `transaction_type` VARCHAR(50),
  `receipt_no` VARCHAR(100) UNIQUE,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`fee_record_id`) REFERENCES `fee_record` (`id`) ON DELETE CASCADE,
  INDEX `idx_fee_record_id` (`fee_record_id`),
  INDEX `idx_payment_date` (`payment_date`),
  INDEX `idx_receipt_no` (`receipt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================================
-- 3. TRAINER_PAYMENT Table - Payments made to trainers
-- ========================================================================
CREATE TABLE IF NOT EXISTS `trainer_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trainer_id` BIGINT NOT NULL,
  `amount` DOUBLE NOT NULL,
  `payment_mode` VARCHAR(50),
  `payment_reference` VARCHAR(100) UNIQUE NOT NULL,
  `payment_date` DATE NOT NULL,
  `status` VARCHAR(50),
  `remarks` VARCHAR(500),
  `created_at` DATE NOT NULL,
  `updated_at` DATE NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`trainer_id`) REFERENCES `trainer_profile` (`id`) ON DELETE CASCADE,
  INDEX `idx_trainer_id` (`trainer_id`),
  INDEX `idx_payment_date` (`payment_date`),
  INDEX `idx_status` (`status`),
  INDEX `idx_payment_reference` (`payment_reference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================================
-- 4. Update COURSE Table - Add total_fee column if not exists
-- ========================================================================
ALTER TABLE `course` ADD COLUMN IF NOT EXISTS `total_fee` DOUBLE DEFAULT 0;
ALTER TABLE `course` ADD COLUMN IF NOT EXISTS `name` VARCHAR(255);
ALTER TABLE `course` ADD COLUMN IF NOT EXISTS `trainer_id` BIGINT;
ALTER TABLE `course` ADD FOREIGN KEY IF NOT EXISTS (`trainer_id`) REFERENCES `trainer_profile` (`id`) ON DELETE SET NULL;

-- ========================================================================
-- 5. Update STUDENT_PROFILE Table - Add missing columns
-- ========================================================================
ALTER TABLE `student_profile` ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20);
ALTER TABLE `student_profile` ADD COLUMN IF NOT EXISTS `course` VARCHAR(255);
ALTER TABLE `student_profile` ADD COLUMN IF NOT EXISTS `admission_date` DATE;
ALTER TABLE `student_profile` ADD COLUMN IF NOT EXISTS `fee_paid` BOOLEAN DEFAULT FALSE;

-- ========================================================================
-- 6. Update TRAINER_PROFILE Table - Add missing columns
-- ========================================================================
ALTER TABLE `trainer_profile` ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20);

-- ========================================================================
-- 7. Update ASSIGNMENT Table - Add missing columns
-- ========================================================================
ALTER TABLE `assignment` ADD COLUMN IF NOT EXISTS `title` VARCHAR(255);
ALTER TABLE `assignment` ADD COLUMN IF NOT EXISTS `max_score` DOUBLE;
ALTER TABLE `assignment` ADD COLUMN IF NOT EXISTS `score` DOUBLE;
ALTER TABLE `assignment` ADD COLUMN IF NOT EXISTS `evaluation_status` VARCHAR(50);

-- ========================================================================
-- 8. Create Junction Tables for Many-to-Many Relationships
-- ========================================================================

-- Student-Course enrollment
CREATE TABLE IF NOT EXISTS `student_courses` (
  `student_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  PRIMARY KEY (`student_id`, `course_id`),
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Student-Internship enrollment
CREATE TABLE IF NOT EXISTS `student_internships` (
  `student_id` BIGINT NOT NULL,
  `internship_id` BIGINT NOT NULL,
  PRIMARY KEY (`student_id`, `internship_id`),
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`internship_id`) REFERENCES `internship` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trainer-Course teaching
CREATE TABLE IF NOT EXISTS `trainer_courses` (
  `trainer_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  PRIMARY KEY (`trainer_id`, `course_id`),
  FOREIGN KEY (`trainer_id`) REFERENCES `trainer_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trainer-Internship teaching
CREATE TABLE IF NOT EXISTS `trainer_internships` (
  `trainer_id` BIGINT NOT NULL,
  `internship_id` BIGINT NOT NULL,
  PRIMARY KEY (`trainer_id`, `internship_id`),
  FOREIGN KEY (`trainer_id`) REFERENCES `trainer_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`internship_id`) REFERENCES `internship` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================================
-- 9. Create Indexes for Performance Optimization
-- ========================================================================

-- Fee Record Indexes
CREATE INDEX IF NOT EXISTS `idx_fee_record_student_course` ON `fee_record` (`student_id`, `course_id`);
CREATE INDEX IF NOT EXISTS `idx_fee_record_pending` ON `fee_record` (`pending_amount`);

-- Fee Transaction Indexes
CREATE INDEX IF NOT EXISTS `idx_fee_transaction_type` ON `fee_transaction` (`transaction_type`);
CREATE INDEX IF NOT EXISTS `idx_fee_transaction_date_range` ON `fee_transaction` (`payment_date`);

-- Trainer Payment Indexes
CREATE INDEX IF NOT EXISTS `idx_trainer_payment_date_range` ON `trainer_payment` (`payment_date`);
CREATE INDEX IF NOT EXISTS `idx_trainer_payment_status` ON `trainer_payment` (`status`);

-- Attendance Indexes
CREATE INDEX IF NOT EXISTS `idx_attendance_student_course` ON `attendance` (`student_id`, `course_id`);
CREATE INDEX IF NOT EXISTS `idx_attendance_date` ON `attendance` (`date`);

-- Assignment Indexes
CREATE INDEX IF NOT EXISTS `idx_assignment_student_course` ON `assignment` (`student_id`, `course_id`);
CREATE INDEX IF NOT EXISTS `idx_assignment_status` ON `assignment` (`evaluation_status`);

-- ========================================================================
-- 10. Verify Schema Creation
-- ========================================================================

-- Show all tables
SHOW TABLES;

-- Show fee_record structure
DESCRIBE fee_record;

-- Show fee_transaction structure
DESCRIBE fee_transaction;

-- Show trainer_payment structure
DESCRIBE trainer_payment;

-- ========================================================================
-- End of Schema Updates
-- ========================================================================
