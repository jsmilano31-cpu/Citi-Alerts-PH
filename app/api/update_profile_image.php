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

$upload_dir = __DIR__ . '/uploads/profile/';
if (!file_exists($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}

try {
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (!$data || !isset($data['user_id'], $data['image'])) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }

    $user_id = (int)$data['user_id'];
    $image_b64 = $data['image'];

    // Sanitize base64 prefix if present
    if (strpos($image_b64, ',') !== false) {
        $parts = explode(',', $image_b64, 2);
        $image_b64 = $parts[1];
    }

    $binary = base64_decode($image_b64);
    if ($binary === false) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Invalid image data']);
        exit();
    }

    $filename = uniqid('avatar_', true) . '.jpg';
    $filepath = $upload_dir . $filename;

    if (file_put_contents($filepath, $binary) === false) {
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Failed to save image']);
        exit();
    }

    $relative_path = 'uploads/profile/' . $filename;

    $stmt = $pdo->prepare('UPDATE users SET profile_image = ?, updated_at = NOW() WHERE id = ?');
    $ok = $stmt->execute([$relative_path, $user_id]);

    if (!$ok) {
        @unlink($filepath);
        http_response_code(200);
        echo json_encode(['success' => false, 'message' => 'Failed to update profile']);
        exit();
    }

    echo json_encode(['success' => true, 'message' => 'Profile image updated', 'profile_image' => $relative_path]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Server error']);
}

