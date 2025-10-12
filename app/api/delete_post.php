<?php
require_once 'config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

if ($_SERVER['REQUEST_METHOD'] !== 'DELETE') {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method not allowed']);
    exit;
}

try {
    // Get post ID from URL parameter
    $post_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

    if ($post_id <= 0) {
        throw new Exception('Invalid post ID');
    }

    // Check if post exists and get image path
    $checkPost = "SELECT image_path FROM posts WHERE id = $post_id";
    $result = $conn->query($checkPost);

    if ($result->num_rows === 0) {
        throw new Exception('Post not found');
    }

    $post = $result->fetch_assoc();

    // Delete the post from database
    $query = "DELETE FROM posts WHERE id = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $post_id);

    if ($stmt->execute()) {
        // Delete associated image file if exists
        if ($post['image_path'] && file_exists($post['image_path'])) {
            unlink($post['image_path']);
        }

        echo json_encode([
            'success' => true,
            'message' => 'Post deleted successfully'
        ]);
    } else {
        throw new Exception('Failed to delete post');
    }

} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ]);
}
?>
