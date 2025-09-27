<?php
define('DB_HOST', 'localhost');
define('DB_USER', 'u847001018_spencer');
define('DB_PASS', 'SpencerMil@no123');
define('DB_NAME', 'u847001018_citialerts');

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Create admin table if not exists
$create_admin_table = "CREATE TABLE IF NOT EXISTS tbl_admin (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";

if ($conn->query($create_admin_table) === FALSE) {
    die("Error creating table: " . $conn->error);
}

// Check if default admin exists
$check_admin = "SELECT id FROM tbl_admin WHERE username = 'admin'";
$result = $conn->query($check_admin);

if ($result->num_rows == 0) {
    // Insert default admin user
    $default_password = password_hash('Admin@123', PASSWORD_DEFAULT);
    $insert_admin = "INSERT INTO tbl_admin (username, password, email)
                     VALUES ('admin', '$default_password', 'admin@citialerts.ph')";

    if ($conn->query($insert_admin) === FALSE) {
        die("Error creating default admin: " . $conn->error);
    }
}
?>
