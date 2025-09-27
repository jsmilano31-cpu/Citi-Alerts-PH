<?php
header('Content-Type: application/json');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

require_once 'config.php';

try {
    // Get and validate JSON input
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$json || json_last_error() !== JSON_ERROR_NONE) {
        throw new Exception("Invalid JSON data received");
    }

    // Log received data
    error_log("Login attempt - Request data: " . $json);

    // Validate required fields
    if (!isset($data->username) || !isset($data->password)) {
        throw new Exception("Username and password are required");
    }

    $username = trim($data->username);
    $password = $data->password;

    // Query user
    $stmt = $pdo->prepare("SELECT * FROM users WHERE username = :username OR email = :email");
    $stmt->execute([
        ':username' => $username,
        ':email' => $username
    ]);

    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password'])) {
        throw new Exception("Invalid username or password");
    }

    // Check if moderator is verified
    if ($user['user_type'] === 'moderator' && !$user['is_verified']) {
        throw new Exception("Your moderator account is pending verification");
    }

    // Prepare user data for response
    $userData = array(
        'id' => $user['id'],
        'username' => $user['username'],
        'email' => $user['email'],
        'first_name' => $user['first_name'],
        'last_name' => $user['last_name'],
        'phone' => $user['phone'],
        'user_type' => $user['user_type'],
        'is_verified' => (bool)$user['is_verified'],
        'profile_image' => $user['profile_image']
    );

    echo json_encode([
        'success' => true,
        'message' => 'Login successful',
        'user' => $userData
    ]);

} catch (Exception $e) {
    error_log("Login error: " . $e->getMessage());
    http_response_code(200); // Send 200 instead of 500 for handled errors
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>
