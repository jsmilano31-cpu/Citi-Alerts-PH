-- phpMyAdmin SQL Dump
-- CitiAlerts PH - Users Table Only  
-- Database: `u847001018_citialerts`

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `u847001018_citialerts`
--

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `user_type` enum('user','moderator') NOT NULL DEFAULT 'user',
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `verification_documents` text DEFAULT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_is_verified` (`is_verified`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Sample data for table `users`
-- Password for all sample users is: password123
--

INSERT INTO `users` (`id`, `username`, `email`, `password`, `user_type`, `first_name`, `last_name`, `phone`, `profile_image`, `verification_documents`, `is_verified`, `created_at`, `updated_at`) VALUES
(1, 'johndoe', 'john.doe@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user', 'John', 'Doe', '09123456789', NULL, NULL, 1, '2024-01-15 08:30:00', '2024-01-15 08:30:00'),
(2, 'janesmith', 'jane.smith@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user', 'Jane', 'Smith', '09234567890', NULL, NULL, 1, '2024-01-16 09:15:00', '2024-01-16 09:15:00'),
(3, 'moderator1', 'mod1@citialerts.ph', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'moderator', 'Maria', 'Santos', '09345678901', NULL, '["doc1.pdf","id1.jpg"]', 1, '2024-01-17 10:00:00', '2024-01-17 14:30:00'),
(4, 'pendingmod', 'pending@citialerts.ph', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'moderator', 'Carlos', 'Reyes', '09456789012', NULL, '["cert1.pdf","license1.jpg"]', 0, '2024-01-18 11:45:00', '2024-01-18 11:45:00');

-- --------------------------------------------------------

--
-- Set AUTO_INCREMENT for table `users`
--

ALTER TABLE `users` AUTO_INCREMENT = 5;

-- --------------------------------------------------------

--
-- Additional indexes for better performance
--

CREATE INDEX `idx_user_email_type` ON `users` (`email`, `user_type`);
CREATE INDEX `idx_username_verified` ON `users` (`username`, `is_verified`);

-- --------------------------------------------------------

--
-- Trigger for automatic timestamp updates
--

DELIMITER $$

CREATE TRIGGER `users_updated_at` BEFORE UPDATE ON `users`
FOR EACH ROW BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP();
END$$

DELIMITER ;

-- --------------------------------------------------------

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

-- 
-- Import Instructions:
-- 1. Open phpMyAdmin
-- 2. Select your database: u847001018_citialerts
-- 3. Go to Import tab
-- 4. Choose this file and click Go
-- 
-- Test Accounts (Password: password123):
-- - johndoe (Regular User)
-- - janesmith (Regular User)  
-- - moderator1 (Verified Moderator)
-- - pendingmod (Pending Moderator)
--