<?php
// Set session cookie parameters BEFORE starting session (must match other files)
ini_set('session.cookie_samesite', 'None');
ini_set('session.cookie_secure', 'On');
ini_set('session.cookie_domain', '.jsmkj.space');
ini_set('session.cookie_path', '/');
ini_set('session.cookie_lifetime', 86400);
ini_set('session.cookie_httponly', true);

// CORS Headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Access-Control-Allow-Credentials: true");
header("Content-Type: application/json; charset=UTF-8");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Start session to access session data
session_start();

require_once "config.php";

// Enhanced debug logging for logout
error_log("=== LOGOUT DEBUG START ===");
error_log("Session ID: " . session_id());
error_log("Session data before logout: " . print_r($_SESSION, true));
error_log("Cookies before logout: " . print_r($_COOKIE, true));
error_log("=== LOGOUT DEBUG END ===");

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $data = json_decode(file_get_contents("php://input"));
        $user_id = null;

        // Get user_id from session or request data
        if (isset($_SESSION['user_id'])) {
            $user_id = $_SESSION['user_id'];
            error_log("Got user_id from session: " . $user_id);
        } elseif (isset($data->user_id)) {
            $user_id = $data->user_id;
            error_log("Got user_id from request: " . $user_id);
        }

        if ($user_id) {
            // Update last_logout timestamp in database
            $query = "UPDATE users SET last_logout = NOW() WHERE id = ?";
            $stmt = $pdo->prepare($query);
            $stmt->execute([$user_id]);

            error_log("Updated last_logout for user_id: " . $user_id);
        }

        // GRACEFUL SESSION CLEANUP

        // 1. Clear all session data
        $_SESSION = array();
        error_log("Cleared session array");

        // 2. Delete the session cookie from client
        if (isset($_COOKIE[session_name()])) {
            setcookie(
                session_name(),
                '',
                time() - 3600,
                ini_get('session.cookie_path'),
                ini_get('session.cookie_domain'),
                ini_get('session.cookie_secure'),
                ini_get('session.cookie_httponly')
            );
            error_log("Deleted session cookie from client");
        }

        // 3. Destroy the session on server
        if (session_status() === PHP_SESSION_ACTIVE) {
            session_destroy();
            error_log("Destroyed server-side session");
        }

        // 4. Clear any additional cookies that might exist
        setcookie('PHPSESSID', '', time() - 3600, '/', '.jsmkj.space', true, true);

        error_log("Logout completed successfully");

        echo json_encode([
            "success" => true,
            "message" => "Logged out successfully",
            "session_cleared" => true
        ]);

    } catch (Exception $e) {
        error_log("Logout error: " . $e->getMessage());

        // Still try to clear session even if database update fails
        $_SESSION = array();
        if (isset($_COOKIE[session_name()])) {
            setcookie(session_name(), '', time() - 3600, '/', '.jsmkj.space', true, true);
        }
        if (session_status() === PHP_SESSION_ACTIVE) {
            session_destroy();
        }

        echo json_encode([
            "success" => true, // Still return success since session was cleared
            "message" => "Logged out successfully (with minor database error)",
            "session_cleared" => true
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        "success" => false,
        "message" => "Invalid request method"
    ]);
}
?>
