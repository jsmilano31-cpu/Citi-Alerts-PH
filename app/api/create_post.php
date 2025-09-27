<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

require_once "config.php";

// Define upload directory for post images
$upload_dir = dirname(__FILE__) . '/uploads/posts/';
if (!file_exists($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}

try {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data || !isset($data->title) || !isset($data->description) || !isset($data->moderator_id)) {
        throw new Exception("Missing required fields");
    }

    // Handle image upload if provided
    $image_path = null;
    if (isset($data->image) && !empty($data->image)) {
        $image_data = base64_decode($data->image);
        $file_name = uniqid() . '_post.jpg';
        $file_path = $upload_dir . $file_name;

        if (file_put_contents($file_path, $image_data)) {
            $image_path = 'uploads/posts/' . $file_name;
        }
    }

    // Insert post into database
    $stmt = $pdo->prepare("INSERT INTO posts (moderator_id, title, description, image_path, created_at, updated_at)
                          VALUES (?, ?, ?, ?, NOW(), NOW())");

    if (!$stmt->execute([$data->moderator_id, $data->title, $data->description, $image_path])) {
        throw new Exception("Failed to create post");
    }

    $post_id = $pdo->lastInsertId();

    // Get the created post details
    $stmt = $pdo->prepare("SELECT p.*, u.username as moderator_name, u.profile_image as moderator_image
                          FROM posts p
                          JOIN users u ON p.moderator_id = u.id
                          WHERE p.id = ?");
    $stmt->execute([$post_id]);
    $post = $stmt->fetch();

    echo json_encode([
        "success" => true,
        "message" => "Post created successfully",
        "post" => $post
    ]);

} catch (Exception $e) {
    error_log("Create post error: " . $e->getMessage());
    http_response_code(200); // Keep 200 to handle error in app
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
}
