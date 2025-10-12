<?php
// Unified session configuration for all endpoints
class SessionHandler {
    public static function initializeSession() {
        // Set consistent session configuration
        ini_set('session.cookie_samesite', 'None');
        ini_set('session.cookie_secure', 'On');
        ini_set('session.cookie_domain', '.jsmkj.space');
        ini_set('session.cookie_path', '/');
        ini_set('session.cookie_lifetime', 86400);
        ini_set('session.cookie_httponly', true);
        ini_set('session.use_strict_mode', true);

        // Only start session if not already active
        if (session_status() === PHP_SESSION_NONE) {
            session_start();
        }

        error_log("Session initialized - ID: " . session_id());
        error_log("Session data: " . print_r($_SESSION, true));

        return session_id();
    }

    public static function validateAuthentication() {
        self::initializeSession();

        if (!isset($_SESSION['user_id'])) {
            error_log("Authentication failed - no user_id in session");
            error_log("Session data: " . print_r($_SESSION, true));
            error_log("Cookies: " . print_r($_COOKIE, true));
            return false;
        }

        error_log("Authentication successful - user_id: " . $_SESSION['user_id']);
        return true;
    }
}
?>

