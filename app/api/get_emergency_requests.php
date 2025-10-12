<?php
header('Content-Type: application/json');
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

// Get status filter from query parameters
$status = isset($_GET['status']) ? $_GET['status'] : null;
$showAll = isset($_GET['show_all']) && $_GET['show_all'] === 'true';

try {
    $query = "
        SELECT
            er.*,
            u.first_name as user_first_name,
            u.last_name as user_last_name,
            u.phone as user_phone,
            r.first_name as responder_first_name,
            r.last_name as responder_last_name,
            r.phone as responder_phone
        FROM emergency_requests er
        JOIN users u ON er.user_id = u.id
        LEFT JOIN users r ON er.responder_id = r.id
    ";

    $params = [];

    if (!$showAll) {
        if ($status) {
            $query .= " WHERE er.status = ?";
            $params[] = $status;
        } else {
            $query .= " WHERE er.status != 'completed' AND er.status != 'cancelled'";
        }
    }

    $query .= " ORDER BY
        CASE
            WHEN er.status = 'pending' THEN 1
            WHEN er.status = 'help_coming' THEN 2
            WHEN er.status = 'completed' THEN 3
            ELSE 4
        END,
        er.created_at DESC";

    $stmt = $pdo->prepare($query);
    $stmt->execute($params);
    $requests = $stmt->fetchAll();

    echo json_encode([
        'success' => true,
        'requests' => $requests
    ]);
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
