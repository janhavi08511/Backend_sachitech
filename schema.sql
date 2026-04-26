-- SQL Schema for Training Management System Database (tms_db)
-- Designed for MySQL Database

CREATE DATABASE IF NOT EXISTS tms_db;
USE tms_db;

-- --------------------------------------------------------
-- Table structure for table `users`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `course`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `duration` VARCHAR(255) DEFAULT NULL,
  `fee` DOUBLE DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `internship`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `internship` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `duration` VARCHAR(255) DEFAULT NULL,
  `fee` DOUBLE DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `student_profile`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `student_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `enrollment_number` VARCHAR(255) DEFAULT NULL,
  `contact_number` VARCHAR(255) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `trainer_profile`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trainer_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `specialization` VARCHAR(255) DEFAULT NULL,
  `contact_number` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `assignment`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) DEFAULT NULL,
  `max_score` DOUBLE DEFAULT NULL,
  `score` DOUBLE DEFAULT NULL,
  `student_submission_link` VARCHAR(255) DEFAULT NULL,
  `evaluation_status` VARCHAR(255) DEFAULT NULL,
  `course_id` BIGINT DEFAULT NULL,
  `internship_id` BIGINT DEFAULT NULL,
  `student_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`internship_id`) REFERENCES `internship` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `attendance`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `date` DATE DEFAULT NULL,
  `status` VARCHAR(255) DEFAULT NULL,
  `student_id` BIGINT DEFAULT NULL,
  `course_id` BIGINT DEFAULT NULL,
  `internship_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`internship_id`) REFERENCES `internship` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `fee_management`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fee_management` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fee_amount` DOUBLE DEFAULT NULL,
  `pending_amount` DOUBLE DEFAULT NULL,
  `payment_date` DATE DEFAULT NULL,
  `payment_type` VARCHAR(255) DEFAULT NULL,
  `student_id` BIGINT DEFAULT NULL,
  `course_id` BIGINT DEFAULT NULL,
  `internship_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`internship_id`) REFERENCES `internship` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `note`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) DEFAULT NULL,
  `file_url` VARCHAR(255) DEFAULT NULL,
  `course_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- End of Schema Definition
-- --------------------------------------------------------
