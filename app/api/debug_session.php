<?php
header('Content-Type: application/json');

// Use EXACT same session configuration as complete_emergency.php
ini_set('session.cookie_samesite', 'None');
ini_set('session.cookie_secure', 'On');
ini_set('session.cookie_domain', '.jsmkj.space');
ini_set('session.cookie_path', '/');
ini_set('session.cookie_lifetime', 86400);
ini_set('session.cookie_httponly', true);
ini_set('session.use_strict_mode', true);

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

$session_id = session_id();
$cookie_session_id = $_COOKIE['PHPSESSID'] ?? 'NOT_SET';
$session_file = session_save_path() . '/sess_' . $session_id;

error_log("=== SESSION DIAGNOSTIC ===");
error_log("Session ID: " . $session_id);
error_log("Cookie Session ID: " . $cookie_session_id);
error_log("Session file path: " . $session_file);
error_log("Session file exists: " . (file_exists($session_file) ? 'YES' : 'NO'));
error_log("Session data: " . print_r($_SESSION, true));

if (file_exists($session_file)) {
    $file_contents = file_get_contents($session_file);
    error_log("Session file contents: " . $file_contents);
    error_log("Session file size: " . filesize($session_file) . " bytes");
    error_log("Session file modified: " . date('Y-m-d H:i:s', filemtime($session_file)));
} else {
    error_log("Session file does not exist");
}

echo json_encode([
    'session_id' => $session_id,
    'cookie_session_id' => $cookie_session_id,
    'session_data' => $_SESSION,
    'session_file_exists' => file_exists($session_file),
    'session_file_size' => file_exists($session_file) ? filesize($session_file) : 0,
    'session_file_contents' => file_exists($session_file) ? file_get_contents($session_file) : 'NO_FILE',
    'all_cookies' => $_COOKIE
]);
?>
