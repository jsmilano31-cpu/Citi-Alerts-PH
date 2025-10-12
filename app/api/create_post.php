<?php
require_once 'config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
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
    if (!isset($input['title']) || !isset($input['description']) || !isset($input['moderator_id'])) {
        throw new Exception('Missing required fields: title, description, moderator_id');
    }

    $title = $conn->real_escape_string($input['title']);
    $description = $conn->real_escape_string($input['description']);
    $moderator_id = (int)$input['moderator_id'];
    $image_path = isset($input['image_path']) ? $conn->real_escape_string($input['image_path']) : null;

    // Verify moderator exists and is valid
    $moderatorCheck = "SELECT id FROM users WHERE id = $moderator_id AND user_type = 'moderator'";
    $moderatorResult = $conn->query($moderatorCheck);

    if ($moderatorResult->num_rows === 0) {
        throw new Exception('Invalid moderator ID');
    }

    // Insert new post
    $query = "INSERT INTO posts (moderator_id, title, description, image_path) VALUES (?, ?, ?, ?)";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("isss", $moderator_id, $title, $description, $image_path);

    if ($stmt->execute()) {
        $post_id = $conn->insert_id;

        // Get the created post data
        $getPost = "SELECT p.*, u.username, u.first_name, u.last_name
                    FROM posts p
                    JOIN users u ON p.moderator_id = u.id
                    WHERE p.id = $post_id";
        $result = $conn->query($getPost);
        $post = $result->fetch_assoc();

        $response = [
            'success' => true,
            'message' => 'Post created successfully',
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
        throw new Exception('Failed to create post');
    }

} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ]);
}
?>
