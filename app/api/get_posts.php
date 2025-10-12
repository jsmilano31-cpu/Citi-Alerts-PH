<?php
require_once 'config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

try {
    // Get posts with pagination
    $page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 10;
    $offset = ($page - 1) * $limit;

    $search = isset($_GET['search']) ? $conn->real_escape_string($_GET['search']) : '';
    $status = isset($_GET['status']) ? $conn->real_escape_string($_GET['status']) : 'active';

    // Build query
    $whereClause = "WHERE p.status = '$status'";
    if ($search) {
        $whereClause .= " AND (p.title LIKE '%$search%' OR p.description LIKE '%$search%')";
    }

    // Get total count
    $countQuery = "SELECT COUNT(*) as total FROM posts p $whereClause";
    $countResult = $conn->query($countQuery);
    $totalCount = $countResult->fetch_assoc()['total'];

    // Get posts
    $query = "SELECT p.*, u.username, u.first_name, u.last_name
              FROM posts p
              JOIN users u ON p.moderator_id = u.id
              $whereClause
              ORDER BY p.created_at DESC
              LIMIT $limit OFFSET $offset";

    $result = $conn->query($query);
    $posts = [];

    while ($row = $result->fetch_assoc()) {
        // Increment view count for each request (optional - you might want to track unique views)
        $updateViews = "UPDATE posts SET views = views + 1 WHERE id = " . $row['id'];
        $conn->query($updateViews);

        $posts[] = [
            'id' => (int)$row['id'],
            'title' => $row['title'],
            'description' => $row['description'],
            'image_url' => $row['image_path'] ? $_SERVER['HTTP_HOST'] . '/' . $row['image_path'] : null,
            'status' => $row['status'],
            'views' => (int)$row['views'],
            'author' => [
                'username' => $row['username'],
                'name' => $row['first_name'] . ' ' . $row['last_name']
            ],
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at']
        ];
    }

    $response = [
        'success' => true,
        'data' => $posts,
        'pagination' => [
            'current_page' => $page,
            'per_page' => $limit,
            'total' => (int)$totalCount,
            'total_pages' => ceil($totalCount / $limit),
            'has_next' => $page < ceil($totalCount / $limit),
            'has_prev' => $page > 1
        ]
    ];

    echo json_encode($response);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
