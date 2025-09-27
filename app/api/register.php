<?php
// Ensure clean output - no HTML errors
ini_set('display_errors', 0);
error_reporting(E_ALL);
ini_set('error_log', 'php_errors.log');

// Set JSON content type first
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

// Define upload directory with absolute path
$upload_base_dir = dirname(__FILE__) . '/uploads';
$upload_dir = $upload_base_dir . '/verification/';

// Create directories if they don't exist
if (!file_exists($upload_base_dir)) {
    mkdir($upload_base_dir, 0755, true);
    error_log("Created base upload directory: " . $upload_base_dir);
}
if (!file_exists($upload_dir)) {
    mkdir($upload_dir, 0755, true);
    error_log("Created verification upload directory: " . $upload_dir);
}

// Include database connection
require_once "config.php";

function sendResponse($success, $message, $data = null) {
    $response = array(
        "success" => $success,
        "message" => $message
    );
    if ($data !== null) {
        $response["user"] = $data;
    }
    echo json_encode($response);
    exit();
}

// Check if it's a POST request
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, "Invalid request method");
}

try {
    // Get and validate JSON input
    $json = file_get_contents('php://input');
    if (!$json) {
        sendResponse(false, "No data received");
    }

    $data = json_decode($json);
    if (json_last_error() !== JSON_ERROR_NONE) {
        sendResponse(false, "Invalid JSON data: " . json_last_error_msg());
    }

    // Log the received data for debugging
    error_log("Received registration data: " . $json);

    if (!isset($data->username) || !isset($data->password) || !isset($data->email)) {
        sendResponse(false, "Required fields are missing");
    }

    // Handle base64 image for verification documents
    $verification_documents = null;
    if (isset($data->credential_image) && $data->credential_image != '') {
        $image_data = base64_decode($data->credential_image);
        if ($image_data === false) {
            error_log("Failed to decode base64 image");
            sendResponse(false, "Invalid image data");
        }

        $file_name = uniqid() . '_verification.jpg';
        $file_path = $upload_dir . $file_name;

        if (!file_put_contents($file_path, $image_data)) {
            error_log("Failed to save verification document: " . $file_path);
            error_log("Upload directory exists: " . (file_exists($upload_dir) ? 'yes' : 'no'));
            error_log("Upload directory writable: " . (is_writable($upload_dir) ? 'yes' : 'no'));
            sendResponse(false, "Failed to save verification document");
        }

        $verification_documents = $file_name;
        error_log("Verification document uploaded successfully: " . $file_path);
    }

    // Check if username/email exists
    $stmt = $pdo->prepare("SELECT id FROM users WHERE username = ? OR email = ?");
    $stmt->execute([$data->username, $data->email]);

    if ($stmt->rowCount() > 0) {
        sendResponse(false, "Username or email already exists");
    }

    // Prepare user data
    $userData = [
        'username' => $data->username,
        'password' => password_hash($data->password, PASSWORD_DEFAULT),
        'email' => $data->email,
        'first_name' => $data->first_name ?? '',
        'last_name' => $data->last_name ?? '',
        'phone' => $data->phone ?? '',
        'user_type' => $data->user_type ?? 'user',
        'verification_documents' => $verification_documents,
        'is_verified' => 0
    ];

    // Insert new user
    $sql = "INSERT INTO users (username, password, email, first_name, last_name, phone,
            user_type, verification_documents, is_verified, created_at, updated_at)
            VALUES (:username, :password, :email, :first_name, :last_name, :phone,
            :user_type, :verification_documents, :is_verified, NOW(), NOW())";

    $stmt = $pdo->prepare($sql);
    if (!$stmt->execute($userData)) {
        error_log("Database error: " . json_encode($stmt->errorInfo()));
        sendResponse(false, "Failed to create user account");
    }

    $userData['id'] = $pdo->lastInsertId();
    unset($userData['password']); // Don't send password back

    sendResponse(true, "Registration successful", $userData);

} catch (Exception $e) {
    error_log("Registration error: " . $e->getMessage());
    sendResponse(false, "Registration failed: " . $e->getMessage());
}
?>
