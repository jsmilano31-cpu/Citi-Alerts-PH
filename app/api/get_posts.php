<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

require_once "config.php";

try {
    // Get posts with moderator information
    $stmt = $pdo->prepare("
        SELECT
            p.*,
            u.username as moderator_name,
            u.profile_image as moderator_image,
            u.is_verified as moderator_verified
        FROM posts p
        JOIN users u ON p.moderator_id = u.id
        WHERE p.status = 'active'
        ORDER BY p.created_at DESC
    ");

    $stmt->execute();
    $posts = $stmt->fetchAll();

    echo json_encode([
        "success" => true,
        "message" => "Posts retrieved successfully",
        "posts" => $posts
    ]);

} catch (Exception $e) {
    error_log("Get posts error: " . $e->getMessage());
    http_response_code(200);
    echo json_encode([
        "success" => false,
        "message" => "Failed to load posts"
    ]);
}
