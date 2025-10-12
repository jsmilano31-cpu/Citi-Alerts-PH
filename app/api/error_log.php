<?php
function logError($message) {
    $log_file = __DIR__ . '/logs/error_log_' . date('Y-m-d') . '.txt';

    // Create logs directory if it doesn't exist
    $logDir = __DIR__ . '/logs';
    if (!file_exists($logDir)) {
        mkdir($logDir, 0777, true);
    }

    // Format the timestamp and log entry
    $timestamp = date('Y-m-d H:i:s');
    $log_entry = "[{$timestamp}] {$message}\n";

    // Write to log file
    file_put_contents($log_file, $log_entry, FILE_APPEND);
}
?>
