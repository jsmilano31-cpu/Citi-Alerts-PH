<?php
header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'config.php';

try {
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (!$data || !isset($data['user_id'], $data['current_password'], $data['new_password'])) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }

    $user_id = (int)$data['user_id'];
    $current = $data['current_password'];
    $new = $data['new_password'];

    $stmt = $pdo->prepare('SELECT password FROM users WHERE id = ?');
    $stmt->execute([$user_id]);
    $user = $stmt->fetch();

    if (!$user) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'User not found']);
        exit();
    }

    if (!password_verify($current, $user['password'])) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Current password is incorrect']);
        exit();
    }

    $hash = password_hash($new, PASSWORD_DEFAULT);
    $up = $pdo->prepare('UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?');
    $ok = $up->execute([$hash, $user_id]);

    if (!$ok) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Failed to update password']);
        exit();
    }

    echo json_encode(['success' => true, 'message' => 'Password updated']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Server error']);
}

