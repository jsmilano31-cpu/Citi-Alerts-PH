<?php
// Ensure clean output - no HTML errors
ini_set('display_errors', 0);
error_reporting(E_ALL);
ini_set('error_log', 'php_errors.log');

// Database configuration
$servername = "localhost";
$username = "u847001018_spencer";
$password = "SpencerMil@no123";
$dbname = "u847001018_citialerts";

// Function to send JSON response
function sendDatabaseError($message) {
    header('Content-Type: application/json');
    echo json_encode(array(
        "success" => false,
        "message" => $message
    ));
    exit();
}

// Create connection
try {
    $pdo = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
    // Set the PDO error mode to exception
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    error_log("Database connected successfully");
} catch(PDOException $e) {
    error_log("Connection failed: " . $e->getMessage());
    sendDatabaseError("Database connection failed");
}
?>
