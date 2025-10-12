<?php
header('Content-Type: application/json');
require_once 'config.php';
require_once 'error_log.php';

// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    logError("Invalid method: " . $_SERVER['REQUEST_METHOD']);
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

try {
    $rawInput = file_get_contents('php://input');
    logError("Received input: " . $rawInput);
    $input = json_decode($rawInput, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        throw new Exception('Invalid JSON data: ' . json_last_error_msg());
    }

    if (!isset($input['user_id']) || !isset($input['emergency_type']) ||
        !isset($input['latitude']) || !isset($input['longitude'])) {
        logError("Missing fields in input: " . json_encode($input));
        http_response_code(400);
        echo json_encode(['error' => 'Missing required fields']);
        exit();
    }

    // Validate user_id exists in the users table
    $checkUser = $pdo->prepare("SELECT id FROM users WHERE id = ?");
    $checkUser->execute([$input['user_id']]);
    if (!$checkUser->fetch()) {
        throw new Exception('Invalid user_id');
    }

    $stmt = $pdo->prepare("INSERT INTO emergency_requests (user_id, emergency_type, latitude, longitude, location_name)
                           VALUES (:user_id, :emergency_type, :latitude, :longitude, :location_name)");

    $params = [
        ':user_id' => $input['user_id'],
        ':emergency_type' => $input['emergency_type'],
        ':latitude' => $input['latitude'],
        ':longitude' => $input['longitude'],
        ':location_name' => $input['location_name'] ?? null
    ];

    logError("Executing query with params: " . json_encode($params));
    $stmt->execute($params);

    $requestId = $pdo->lastInsertId();
    logError("Successfully created emergency request with ID: " . $requestId);

    echo json_encode([
        'success' => true,
        'message' => 'Emergency request created successfully',
        'request_id' => $requestId
    ]);
} catch(PDOException $e) {
    logError("Database error in create_emergency.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
} catch(Exception $e) {
    logError("Error in create_emergency.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
?>
