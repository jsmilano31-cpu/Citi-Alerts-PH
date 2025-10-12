<?php
header('Content-Type: application/json');
require_once 'config.php';

// Start session AFTER config is loaded
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

error_log("=== SESSION TEST DEBUG ===");
error_log("Session ID from session_id(): " . session_id());
error_log("Session ID from cookie: " . (isset($_COOKIE['PHPSESSID']) ? $_COOKIE['PHPSESSID'] : 'NOT SET'));
error_log("All cookies: " . print_r($_COOKIE, true));
error_log("Session data: " . print_r($_SESSION, true));

// Check if session file exists on server
$session_file = session_save_path() . '/sess_' . session_id();
error_log("Session file path: " . $session_file);
error_log("Session file exists: " . (file_exists($session_file) ? 'YES' : 'NO'));
if (file_exists($session_file)) {
    error_log("Session file contents: " . file_get_contents($session_file));
    error_log("Session file size: " . filesize($session_file) . " bytes");
}

// If this is a POST request, set some test session data
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $_SESSION['test_user_id'] = 123;
    $_SESSION['test_timestamp'] = time();
    session_write_close();

    echo json_encode([
        'success' => true,
        'message' => 'Session data set',
        'session_id' => session_id(),
        'data_set' => [
            'test_user_id' => 123,
            'test_timestamp' => time()
        ]
    ]);
} else {
    // GET request - just return session info
    echo json_encode([
        'session_id' => session_id(),
        'cookie_session_id' => $_COOKIE['PHPSESSID'] ?? 'NOT SET',
        'session_data' => $_SESSION,
        'session_file_exists' => file_exists($session_file),
        'session_file_contents' => file_exists($session_file) ? file_get_contents($session_file) : 'NO FILE'
    ]);
}
?>
