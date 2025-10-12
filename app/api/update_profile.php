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

    if (!$data || !isset($data['user_id'], $data['first_name'], $data['last_name'], $data['email'])) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }

    $user_id = (int)$data['user_id'];
    $first = trim($data['first_name']);
    $last = trim($data['last_name']);
    $email = trim($data['email']);
    $phone = isset($data['phone']) ? trim($data['phone']) : null;

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid email format']);
        exit();
    }

    // Ensure email is unique (except current user)
    $chk = $pdo->prepare('SELECT id FROM users WHERE email = ? AND id <> ?');
    $chk->execute([$email, $user_id]);
    if ($chk->fetch()) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Email already in use']);
        exit();
    }

    $stmt = $pdo->prepare('UPDATE users SET first_name = ?, last_name = ?, phone = ?, email = ?, updated_at = NOW() WHERE id = ?');
    $ok = $stmt->execute([$first, $last, $phone, $email, $user_id]);

    if (!$ok || $stmt->rowCount() === 0) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'No changes or update failed']);
        exit();
    }

    echo json_encode(['success' => true, 'message' => 'Profile updated']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Server error']);
}

