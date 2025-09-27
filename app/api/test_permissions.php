<?php
$test_dir = "uploads/credentials/";
$test_file = $test_dir . "test.txt";

echo "<h2>Directory and File Permission Test</h2>";

// Test directory creation
if (!file_exists($test_dir)) {
    if (mkdir($test_dir, 0755, true)) {
        echo "Successfully created directory: " . $test_dir . "<br>";
    } else {
        echo "Failed to create directory: " . $test_dir . "<br>";
        echo "Error: " . error_get_last()['message'] . "<br>";
    }
}

// Test directory permissions
echo "Directory exists: " . (file_exists($test_dir) ? 'Yes' : 'No') . "<br>";
echo "Directory writable: " . (is_writable($test_dir) ? 'Yes' : 'No') . "<br>";
echo "Directory permissions: " . substr(sprintf('%o', fileperms($test_dir)), -4) . "<br>";
echo "Directory owner: " . fileowner($test_dir) . "<br>";
echo "Current script owner: " . get_current_user() . "<br>";

// Test file creation
if (file_put_contents($test_file, "Test content")) {
    echo "Successfully created test file<br>";
    echo "File permissions: " . substr(sprintf('%o', fileperms($test_file)), -4) . "<br>";
} else {
    echo "Failed to create test file<br>";
    echo "Error: " . error_get_last()['message'] . "<br>";
}

// Display full path
echo "Full path: " . realpath($test_dir) . "<br>";
echo "Document root: " . $_SERVER['DOCUMENT_ROOT'] . "<br>";
?>
