<?php
header('Content-Type: application/json');
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$input = json_decode(file_get_contents('php://input'), true);

if (!isset($input['request_id']) || !isset($input['responder_id'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Request ID and responder ID are required']);
    exit();
}

try {
    // Check if the request is still available
    $checkStmt = $pdo->prepare("SELECT status FROM emergency_requests WHERE id = ? AND status = 'pending'");
    $checkStmt->execute([$input['request_id']]);

    if (!$checkStmt->fetch()) {
        http_response_code(400);
        echo json_encode(['error' => 'Emergency request is no longer available']);
        exit();
    }

    // Update the emergency request
    $stmt = $pdo->prepare("
        UPDATE emergency_requests
        SET status = 'help_coming',
            responder_id = :responder_id,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :request_id
    ");

    $stmt->execute([
        ':request_id' => $input['request_id'],
        ':responder_id' => $input['responder_id']
    ]);

    echo json_encode([
        'success' => true,
        'message' => 'Emergency request accepted successfully'
    ]);
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
