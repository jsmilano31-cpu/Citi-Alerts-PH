<?php
require_once 'config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: PUT, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

if ($_SERVER['REQUEST_METHOD'] !== 'PUT') {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method not allowed']);
    exit;
}

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);

    if (!$input) {
        throw new Exception('Invalid JSON input');
    }

    // Validate required fields
    if (!isset($input['id'])) {
        throw new Exception('Missing required field: id');
    }

    $post_id = (int)$input['id'];

    // Check if post exists
    $checkPost = "SELECT id FROM posts WHERE id = $post_id";
    $checkResult = $conn->query($checkPost);

    if ($checkResult->num_rows === 0) {
        throw new Exception('Post not found');
    }

    // Build update query dynamically
    $updateFields = [];
    $types = "";
    $values = [];

    if (isset($input['title'])) {
        $updateFields[] = "title = ?";
        $types .= "s";
        $values[] = $input['title'];
    }

    if (isset($input['description'])) {
        $updateFields[] = "description = ?";
        $types .= "s";
        $values[] = $input['description'];
    }

    if (isset($input['status'])) {
        if (!in_array($input['status'], ['active', 'archived', 'deleted'])) {
            throw new Exception('Invalid status value');
        }
        $updateFields[] = "status = ?";
        $types .= "s";
        $values[] = $input['status'];
    }

    if (isset($input['image_path'])) {
        $updateFields[] = "image_path = ?";
        $types .= "s";
        $values[] = $input['image_path'];
    }

    if (empty($updateFields)) {
        throw new Exception('No fields to update');
    }

    // Add post_id for WHERE clause
    $types .= "i";
    $values[] = $post_id;

    $query = "UPDATE posts SET " . implode(", ", $updateFields) . " WHERE id = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param($types, ...$values);

    if ($stmt->execute()) {
        // Get updated post data
        $getPost = "SELECT p.*, u.username, u.first_name, u.last_name
                    FROM posts p
                    JOIN users u ON p.moderator_id = u.id
                    WHERE p.id = $post_id";
        $result = $conn->query($getPost);
        $post = $result->fetch_assoc();

        $response = [
            'success' => true,
            'message' => 'Post updated successfully',
            'data' => [
                'id' => (int)$post['id'],
                'title' => $post['title'],
                'description' => $post['description'],
                'image_url' => $post['image_path'] ? $_SERVER['HTTP_HOST'] . '/' . $post['image_path'] : null,
                'status' => $post['status'],
                'views' => (int)$post['views'],
                'author' => [
                    'username' => $post['username'],
                    'name' => $post['first_name'] . ' ' . $post['last_name']
                ],
                'created_at' => $post['created_at'],
                'updated_at' => $post['updated_at']
            ]
        ];

        echo json_encode($response);
    } else {
        throw new Exception('Failed to update post');
    }

} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ]);
}
?>
