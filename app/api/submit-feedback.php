<?php
// submit-feedback.php
// Mobile API endpoint for submitting user feedback

// Database configuration for mobile endpoints
$host = 'localhost';
$dbname = 'u847001018_citialerts';
$username = 'u847001018_spencer';
$password = 'SpencerMil@no123';

// Create MySQLi connection directly
try {
    $conn = new mysqli($host, $username, $password, $dbname);

    // Check connection
    if ($conn->connect_error) {
        throw new Exception("Connection failed: " . $conn->connect_error);
    }

    // Set charset
    $conn->set_charset("utf8mb4");

} catch (Exception $e) {
    error_log("Feedback submission DB connection error: " . $e->getMessage());
    header('Content-Type: application/json');
    echo json_encode(['success' => false, 'error' => 'Database connection failed']);
    exit();
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

function respond($data, $code = 200) {
    http_response_code($code);
    echo json_encode($data);
    exit;
}

// Add debugging function
function debug_log($message) {
    error_log("Feedback API DEBUG: " . $message);
}

debug_log("Feedback API endpoint called - Method: " . $_SERVER['REQUEST_METHOD']);

try {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        debug_log("Processing POST request for feedback submission");

        // Get JSON input
        $input = file_get_contents('php://input');
        debug_log("Raw input: " . $input);

        if (empty($input)) {
            respond(['success' => false, 'error' => 'No data received'], 400);
        }

        $data = json_decode($input, true);

        if ($data === null) {
            debug_log("JSON decode error: " . json_last_error_msg());
            respond(['success' => false, 'error' => 'Invalid JSON data'], 400);
        }

        debug_log("Decoded data: " . print_r($data, true));

        // Validate required fields
        $required_fields = ['user_name', 'user_email', 'overall_rating', 'feedback_category', 'would_recommend'];
        foreach ($required_fields as $field) {
            if (!isset($data[$field]) || empty($data[$field])) {
                respond(['success' => false, 'error' => "Missing required field: $field"], 400);
            }
        }

        // Validate email format
        if (!filter_var($data['user_email'], FILTER_VALIDATE_EMAIL)) {
            respond(['success' => false, 'error' => 'Invalid email format'], 400);
        }

        // Validate rating ranges
        if ($data['overall_rating'] < 1 || $data['overall_rating'] > 5) {
            respond(['success' => false, 'error' => 'Overall rating must be between 1 and 5'], 400);
        }

        // Validate optional ratings
        $rating_fields = ['emergency_alerts_rating', 'evacuation_centers_rating', 'news_updates_rating', 'app_performance_rating'];
        foreach ($rating_fields as $field) {
            if (isset($data[$field]) && $data[$field] > 0 && ($data[$field] < 1 || $data[$field] > 5)) {
                respond(['success' => false, 'error' => "Rating for $field must be between 1 and 5"], 400);
            }
        }

        // Validate enum values
        $valid_categories = ['Bug Report', 'Feature Request', 'General Feedback', 'Complaint', 'Compliment'];
        if (!in_array($data['feedback_category'], $valid_categories)) {
            respond(['success' => false, 'error' => 'Invalid feedback category'], 400);
        }

        $valid_recommendations = ['Yes', 'No', 'Maybe'];
        if (!in_array($data['would_recommend'], $valid_recommendations)) {
            respond(['success' => false, 'error' => 'Invalid recommendation value'], 400);
        }

        // Prepare data for insertion
        $user_name = trim($data['user_name']);
        $user_email = trim($data['user_email']);
        $overall_rating = (int)$data['overall_rating'];
        $emergency_alerts_rating = isset($data['emergency_alerts_rating']) && $data['emergency_alerts_rating'] > 0 ? (int)$data['emergency_alerts_rating'] : null;
        $evacuation_centers_rating = isset($data['evacuation_centers_rating']) && $data['evacuation_centers_rating'] > 0 ? (int)$data['evacuation_centers_rating'] : null;
        $news_updates_rating = isset($data['news_updates_rating']) && $data['news_updates_rating'] > 0 ? (int)$data['news_updates_rating'] : null;
        $app_performance_rating = isset($data['app_performance_rating']) && $data['app_performance_rating'] > 0 ? (int)$data['app_performance_rating'] : null;
        $feedback_category = $data['feedback_category'];
        $would_recommend = $data['would_recommend'];
        $comments = isset($data['comments']) ? trim($data['comments']) : null;

        // Get additional info from headers
        $user_agent = $_SERVER['HTTP_USER_AGENT'] ?? 'Unknown';
        $device_info = substr($user_agent, 0, 255); // Limit to 255 chars

        debug_log("Inserting feedback for user: " . $user_email);

        // Insert feedback into database
        $query = "INSERT INTO user_feedback (
            user_name, user_email, overall_rating,
            emergency_alerts_rating, evacuation_centers_rating, news_updates_rating, app_performance_rating,
            feedback_category, would_recommend, comments, device_info
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        $stmt = $conn->prepare($query);

        if (!$stmt) {
            debug_log("Prepare failed: " . $conn->error);
            respond(['success' => false, 'error' => 'Database prepare failed'], 500);
        }

        $stmt->bind_param(
            "ssiiiisssss",
            $user_name, $user_email, $overall_rating,
            $emergency_alerts_rating, $evacuation_centers_rating, $news_updates_rating, $app_performance_rating,
            $feedback_category, $would_recommend, $comments, $device_info
        );

        if ($stmt->execute()) {
            $feedback_id = $conn->insert_id;
            debug_log("Feedback inserted successfully with ID: " . $feedback_id);

            respond([
                'success' => true,
                'message' => 'Feedback submitted successfully',
                'feedback_id' => $feedback_id
            ]);
        } else {
            debug_log("Execute failed: " . $stmt->error);
            respond(['success' => false, 'error' => 'Failed to save feedback'], 500);
        }

    } else {
        debug_log("Invalid method: " . $_SERVER['REQUEST_METHOD']);
        respond(['success' => false, 'error' => 'Method not allowed. Use POST request.'], 405);
    }

} catch (Exception $e) {
    debug_log("Exception caught: " . $e->getMessage());
    error_log('Feedback submission API error: ' . $e->getMessage());
    respond(['success' => false, 'error' => 'Internal server error'], 500);
}
?>
