<?php
require_once 'config.php';

try {
    $sql = "CREATE TABLE IF NOT EXISTS emergency_requests (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        emergency_type VARCHAR(50) NOT NULL,
        latitude DOUBLE NOT NULL,
        longitude DOUBLE NOT NULL,
        location_name TEXT,
        status ENUM('pending', 'help_coming', 'completed') DEFAULT 'pending',
        responder_id INT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id),
        FOREIGN KEY (responder_id) REFERENCES users(id)
    )";

    $conn->exec($sql);
    echo "Emergency requests table created successfully";
} catch(PDOException $e) {
    echo "Error creating table: " . $e->getMessage();
}

$conn = null;
?>
