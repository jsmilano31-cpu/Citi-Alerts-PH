<?php
function logError($type, $message, $data = null) {
    $log_file = 'logs/error_log_' . date('Y-m-d') . '.txt';

    // Create logs directory if it doesn't exist
    if (!file_exists('logs')) {
        mkdir('logs', 0777, true);
    }

    // Format the timestamp
    $timestamp = date('Y-m-d H:i:s');

    // Format the log entry
    $log_entry = "[{$timestamp}] {$type}: {$message}\n";

    // Add data if provided
    if ($data !== null) {
        $log_entry .= "Data: " . print_r($data, true) . "\n";
    }

    $log_entry .= "--------------------\n";

    // Append to log file
    file_put_contents($log_file, $log_entry, FILE_APPEND);
}

function logLoginAttempt($username, $success, $error = null) {
    $data = [
        'username' => $username,
        'ip_address' => $_SERVER['REMOTE_ADDR'],
        'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'Unknown',
        'success' => $success
    ];

    if ($error) {
        $data['error'] = $error;
    }

    $message = $success ? "Successful login attempt" : "Failed login attempt";
    logError("LOGIN", $message, $data);
}

function logSQLError($query, $error) {
    $data = [
        'query' => $query,
        'error_message' => $error
    ];

    logError("SQL", "Database error occurred", $data);
}

function logRequestData($method, $input) {
    // Remove sensitive data like passwords
    if (isset($input['password'])) {
        $input['password'] = '******';
    }

    $data = [
        'method' => $method,
        'input' => $input,
        'headers' => getallheaders()
    ];

    logError("REQUEST", "Incoming request data", $data);
}

// Create .htaccess to protect the logs directory
$htaccess_content = "Order deny,allow\nDeny from all";
if (!file_exists('logs/.htaccess')) {
    file_put_contents('logs/.htaccess', $htaccess_content);
}
?>

