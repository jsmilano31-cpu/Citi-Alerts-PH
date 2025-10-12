<?php
// Start output buffering to prevent any unwanted output
ob_start();

// Set session cookie parameters BEFORE starting session (must match other files)
ini_set('session.cookie_samesite', 'None');
ini_set('session.cookie_secure', 'On');
ini_set('session.cookie_domain', '.jsmkj.space');
ini_set('session.cookie_path', '/');
ini_set('session.cookie_lifetime', 86400);
ini_set('session.cookie_httponly', true);
ini_set('session.use_strict_mode', true);

// CORS Headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');  // For development, replace with specific domain in production
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Access-Control-Allow-Credentials: true');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'config.php';

try {
    // Get and validate JSON input
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    error_log("Login attempt - Raw input: " . $json);

    if (!$json || json_last_error() !== JSON_ERROR_NONE) {
        error_log("Login error - Invalid JSON: " . json_last_error_msg());
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid JSON data received']);
        exit;
    }

    // Validate required fields
    if (!isset($data->username) || !isset($data->password)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Username and password are required']);
        exit;
    }

    $username = trim($data->username);
    $password = $data->password;

    error_log("Login attempt for username: " . $username);

    // Query user
    $stmt = $pdo->prepare("SELECT * FROM users WHERE username = :username OR email = :email");
    $stmt->execute([
        ':username' => $username,
        ':email' => $username
    ]);

    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password'])) {
        error_log("Login failed - Invalid credentials for username: " . $username);
        http_response_code(401);
        echo json_encode(['success' => false, 'message' => 'Invalid username or password']);
        exit;
    }

    // Check if moderator is verified
    if ($user['user_type'] === 'moderator' && !$user['is_verified']) {
        http_response_code(403);
        echo json_encode(['success' => false, 'message' => 'Your moderator account is pending verification']);
        exit;
    }

    // Clean up any existing session
    if (session_status() === PHP_SESSION_ACTIVE) {
        session_destroy();
    }

    // Start new session with consistent configuration
    session_start();

    // EXTENSIVE DEBUG LOGGING FOR LOGIN
    error_log("=== LOGIN SESSION DEBUG START ===");
    error_log("Session ID after session_start(): " . session_id());
    error_log("Session status: " . session_status());
    error_log("Session save path: " . session_save_path());
    error_log("Session cookie params: " . print_r(session_get_cookie_params(), true));
    error_log("=== LOGIN SESSION DEBUG END ===");

    // Set session data
    $_SESSION['user_id'] = $user['id'];
    $_SESSION['username'] = $user['username'];
    $_SESSION['user_type'] = $user['user_type'];

    // Log session data after setting
    error_log("Session data AFTER setting user info: " . print_r($_SESSION, true));

    // Check if session file was created
    $session_file = session_save_path() . '/sess_' . session_id();
    error_log("Session file path: " . $session_file);

    // Make sure session is written
    session_write_close();

    // Check session file after write_close
    error_log("Session file exists after write_close: " . (file_exists($session_file) ? 'YES' : 'NO'));
    if (file_exists($session_file)) {
        error_log("Session file contents after write_close: " . file_get_contents($session_file));
        error_log("Session file size: " . filesize($session_file) . " bytes");
    }

    error_log("Login successful for user ID: " . $user['id'] . ", Session ID: " . session_id());

    // Clear any buffered output before sending response
    ob_clean();

    // Send success response
    echo json_encode([
        'success' => true,
        'message' => 'Login successful',
        'user' => [
            'id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'user_type' => $user['user_type'],
            'first_name' => $user['first_name'],
            'last_name' => $user['last_name'],
            'phone' => $user['phone'],
            'organization' => $user['organization'],
            'profile_image' => $user['profile_image'],
            'is_verified' => (bool)$user['is_verified'],
            'created_at' => $user['created_at']
        ]
    ]);

} catch (Exception $e) {
    error_log("Login error: " . $e->getMessage() . "\n" . $e->getTraceAsString());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'An error occurred during login'
    ]);
} finally {
    // Ensure output buffer is cleaned and ended
    if (ob_get_level() > 0) {
        ob_end_flush();
    }
}
?>
