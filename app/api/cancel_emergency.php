<?php
header('Content-Type: application/json');
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$input = json_decode(file_get_contents('php://input'), true);

if (!isset($input['request_id'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Request ID is required']);
    exit();
}

try {
    // First check if the request exists and is not already completed
    $checkStmt = $pdo->prepare("SELECT status FROM emergency_requests WHERE id = ? AND status != 'completed'");
    $checkStmt->execute([$input['request_id']]);
    $request = $checkStmt->fetch();

    if (!$request) {
        http_response_code(404);
        echo json_encode(['error' => 'Emergency request not found or already completed']);
        exit();
    }

    // Update the request status to cancelled
    $stmt = $pdo->prepare("UPDATE emergency_requests SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE id = ?");
    $stmt->execute([$input['request_id']]);

    echo json_encode([
        'success' => true,
        'message' => 'Emergency request cancelled successfully'
    ]);
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
