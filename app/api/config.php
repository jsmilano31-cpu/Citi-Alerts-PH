<?php
// Prevent any output before headers
ob_start();

// Database configuration
$host = 'localhost';
$dbname = 'u847001018_citialerts';
$username = 'u847001018_spencer';
$password = 'SpencerMil@no123';

// Error handling setup
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'error.log');

// REMOVED SESSION CONFIGURATION - let individual endpoints handle it

try {
    // PDO Connection (for existing endpoints)
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->setAttribute(PDO::ATTR_EMULATE_PREPARES, false);

    // MySQLi Connection (for mobile endpoints and legacy code)
    $conn = new mysqli($host, $username, $password, $dbname);

    // Check MySQLi connection
    if ($conn->connect_error) {
        throw new Exception("MySQLi connection failed: " . $conn->connect_error);
    }

    // Set charset for MySQLi
    $conn->set_charset("utf8mb4");

} catch(PDOException $e) {
    error_log("PDO Database connection failed: " . $e->getMessage());
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode(['error' => 'Database connection failed']);
    exit();
} catch(Exception $e) {
    error_log("MySQLi Database connection failed: " . $e->getMessage());
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode(['error' => 'Database connection failed']);
    exit();
}

// Function to validate session
function validateSession() {
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    if (!isset($_SESSION['user_id'])) {
        error_log("Session validation failed. Session data: " . print_r($_SESSION, true));
        error_log("Cookies: " . print_r($_COOKIE, true));
        return false;
    }
    return true;
}

function sendJsonResponse($data, $statusCode = 200) {
    ob_clean();
    http_response_code($statusCode);
    header('Content-Type: application/json');
    echo json_encode($data);
    exit();
}
?>
