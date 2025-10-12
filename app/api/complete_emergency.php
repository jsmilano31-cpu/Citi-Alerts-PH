<?php
// Set session cookie parameters FIRST, before any includes
ini_set('session.cookie_samesite', 'None');
ini_set('session.cookie_secure', 'On');
ini_set('session.cookie_domain', '.jsmkj.space');
ini_set('session.cookie_path', '/');
ini_set('session.cookie_lifetime', 86400);
ini_set('session.cookie_httponly', true);
ini_set('session.use_strict_mode', true);

header('Content-Type: application/json');

// Include config AFTER session configuration is set
require_once 'config.php';

// Start session AFTER both config and session parameters are set
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

$input = json_decode(file_get_contents('php://input'), true);
error_log("Complete emergency request received: " . print_r($input, true));

if (!isset($input['request_id']) || !isset($input['user_id'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Request ID and User ID are required']);
    exit();
}

$request_id = $input['request_id'];
$user_id = $input['user_id'];

try {
    // Begin transaction
    $pdo->beginTransaction();

    // Check if the request exists and user owns it
    $checkStmt = $pdo->prepare("SELECT status, user_id FROM emergency_requests WHERE id = ? FOR UPDATE");
    $checkStmt->execute([$request_id]);
    $request = $checkStmt->fetch();

    if (!$request) {
        $pdo->rollBack();
        error_log("Request not found. Request ID: " . $request_id);
        http_response_code(404);
        echo json_encode(['error' => 'Emergency request not found']);
        exit();
    }

    // Check if the user owns this request
    if ((int)$request['user_id'] !== (int)$user_id) {
        $pdo->rollBack();
        error_log("User " . $user_id . " not authorized to complete request owned by " . $request['user_id']);
        http_response_code(403);
        echo json_encode(['error' => 'Not authorized to complete this request']);
        exit();
    }

    // Check if request can be completed (should be in help_coming status)
    if ($request['status'] !== 'help_coming') {
        $pdo->rollBack();
        error_log("Request cannot be completed. Current status: " . $request['status']);
        http_response_code(400);
        echo json_encode(['error' => 'Request cannot be completed in current status: ' . $request['status']]);
        exit();
    }

    // Update the request status to completed
    $stmt = $pdo->prepare("UPDATE emergency_requests SET status = 'completed', updated_at = CURRENT_TIMESTAMP WHERE id = ?");
    $result = $stmt->execute([$request_id]);

    if ($stmt->rowCount() === 0) {
        $pdo->rollBack();
        error_log("Failed to update request status. No rows affected.");
        http_response_code(500);
        echo json_encode(['error' => 'Failed to update request status']);
        exit();
    }

    // Verify the update was successful
    $verifyStmt = $pdo->prepare("SELECT status FROM emergency_requests WHERE id = ?");
    $verifyStmt->execute([$request_id]);
    $updatedRequest = $verifyStmt->fetch();

    if ($updatedRequest['status'] !== 'completed') {
        $pdo->rollBack();
        error_log("Status verification failed. Expected 'completed' but got: " . $updatedRequest['status']);
        http_response_code(500);
        echo json_encode(['error' => 'Failed to verify status update']);
        exit();
    }

    // Commit the transaction
    $pdo->commit();

    error_log("Successfully completed emergency request ID: " . $request_id . " by user: " . $user_id);

    echo json_encode([
        'success' => true,
        'message' => 'Emergency request marked as completed',
        'request_id' => $request_id,
        'new_status' => 'completed'
    ]);

} catch(PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Database error in complete_emergency.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Database error occurred']);
} catch(Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("General error in complete_emergency.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'An error occurred']);
}
?>
