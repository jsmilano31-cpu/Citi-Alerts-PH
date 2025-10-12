<?php
require_once 'includes/header.php';
require_once 'config.php';

// Handle post actions
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['action'])) {
        $action = $_POST['action'];

        switch ($action) {
            case 'create':
                $title = $conn->real_escape_string($_POST['title']);
                $description = $conn->real_escape_string($_POST['description']);
                $moderator_id = $_SESSION['admin_id']; // Assuming admin_id is stored in session

                // Handle image upload
                $image_path = null;
                if (isset($_FILES['image']) && $_FILES['image']['error'] === UPLOAD_ERR_OK) {
                    $upload_dir = '../api/uploads/posts/';
                    if (!is_dir($upload_dir)) {
                        mkdir($upload_dir, 0755, true);
                    }

                    $file_extension = pathinfo($_FILES['image']['name'], PATHINFO_EXTENSION);
                    $filename = uniqid() . '.' . $file_extension;
                    $image_path = $upload_dir . $filename;

                    if (move_uploaded_file($_FILES['image']['tmp_name'], $image_path)) {
                        // Image uploaded successfully - store relative path for API access
                        $image_path = 'uploads/posts/' . $filename;
                    } else {
                        $image_path = null;
                    }
                }

                $query = "INSERT INTO posts (moderator_id, title, description, image_path) VALUES (?, ?, ?, ?)";
                $stmt = $conn->prepare($query);
                $stmt->bind_param("isss", $moderator_id, $title, $description, $image_path);
                $stmt->execute();
                break;

            case 'update':
                $post_id = $conn->real_escape_string($_POST['post_id']);
                $title = $conn->real_escape_string($_POST['title']);
                $description = $conn->real_escape_string($_POST['description']);

                $query = "UPDATE posts SET title = ?, description = ? WHERE id = ?";
                $stmt = $conn->prepare($query);
                $stmt->bind_param("ssi", $title, $description, $post_id);
                $stmt->execute();
                break;

            case 'change_status':
                $post_id = $conn->real_escape_string($_POST['post_id']);
                $new_status = $conn->real_escape_string($_POST['new_status']);

                $query = "UPDATE posts SET status = ? WHERE id = ?";
                $stmt = $conn->prepare($query);
                $stmt->bind_param("si", $new_status, $post_id);
                $stmt->execute();
                break;

            case 'delete':
                $post_id = $conn->real_escape_string($_POST['post_id']);

                // Get image path before deletion
                $image_query = "SELECT image_path FROM posts WHERE id = ?";
                $stmt = $conn->prepare($image_query);
                $stmt->bind_param("i", $post_id);
                $stmt->execute();
                $result = $stmt->get_result();
                $post = $result->fetch_assoc();

                // Delete image file if exists
                if ($post && $post['image_path'] && file_exists($post['image_path'])) {
                    unlink($post['image_path']);
                }

                // Delete post from database
                $query = "DELETE FROM posts WHERE id = ?";
                $stmt = $conn->prepare($query);
                $stmt->bind_param("i", $post_id);
                $stmt->execute();
                break;
        }
    }
}

// Get posts with search and filtering
$search = isset($_GET['search']) ? $conn->real_escape_string($_GET['search']) : '';
$status_filter = isset($_GET['status']) ? $conn->real_escape_string($_GET['status']) : 'all';
$sort = isset($_GET['sort']) ? $conn->real_escape_string($_GET['sort']) : 'newest';

$query = "SELECT p.*, u.username as moderator_name, u.first_name, u.last_name
          FROM posts p
          JOIN users u ON p.moderator_id = u.id
          WHERE 1=1";

if ($search) {
    $query .= " AND (p.title LIKE '%$search%' OR p.description LIKE '%$search%' OR u.username LIKE '%$search%')";
}

if ($status_filter !== 'all') {
    $query .= " AND p.status = '$status_filter'";
}

switch ($sort) {
    case 'oldest':
        $query .= " ORDER BY p.created_at ASC";
        break;
    case 'most_viewed':
        $query .= " ORDER BY p.views DESC";
        break;
    case 'title':
        $query .= " ORDER BY p.title ASC";
        break;
    default:
        $query .= " ORDER BY p.created_at DESC";
}

$posts = $conn->query($query);

// Get statistics
$stats_query = "SELECT
    COUNT(*) as total_posts,
    COUNT(CASE WHEN status = 'active' THEN 1 END) as active_posts,
    COUNT(CASE WHEN status = 'archived' THEN 1 END) as archived_posts,
    SUM(views) as total_views,
    AVG(views) as avg_views
    FROM posts";
$stats_result = $conn->query($stats_query);
$stats = $stats_result->fetch_assoc();
?>

<div class="main-content">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Manage Posts</h2>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createPostModal">
                <i class='bx bx-plus'></i> Create New Post
            </button>
        </div>

        <!-- Statistics Cards -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bx-news display-4 text-primary'></i>
                    <h3 class="mt-2"><?php echo $stats['total_posts'] ?? 0; ?></h3>
                    <p class="text-muted">Total Posts</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bx-check-circle display-4 text-success'></i>
                    <h3 class="mt-2"><?php echo $stats['active_posts'] ?? 0; ?></h3>
                    <p class="text-muted">Active Posts</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bx-archive display-4 text-warning'></i>
                    <h3 class="mt-2"><?php echo $stats['archived_posts'] ?? 0; ?></h3>
                    <p class="text-muted">Archived Posts</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bx-show display-4 text-info'></i>
                    <h3 class="mt-2"><?php echo number_format($stats['total_views'] ?? 0); ?></h3>
                    <p class="text-muted">Total Views</p>
                </div>
            </div>
        </div>

        <!-- Search and Filter Section -->
        <div class="card mb-4">
            <div class="card-body">
                <form class="row g-3">
                    <div class="col-md-4">
                        <input type="text" name="search" class="form-control" placeholder="Search posts..." value="<?php echo htmlspecialchars($_GET['search'] ?? ''); ?>">
                    </div>
                    <div class="col-md-3">
                        <select name="status" class="form-select">
                            <option value="all" <?php echo ($status_filter === 'all') ? 'selected' : ''; ?>>All Status</option>
                            <option value="active" <?php echo ($status_filter === 'active') ? 'selected' : ''; ?>>Active</option>
                            <option value="archived" <?php echo ($status_filter === 'archived') ? 'selected' : ''; ?>>Archived</option>
                            <option value="deleted" <?php echo ($status_filter === 'deleted') ? 'selected' : ''; ?>>Deleted</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select name="sort" class="form-select">
                            <option value="newest" <?php echo ($sort === 'newest') ? 'selected' : ''; ?>>Newest First</option>
                            <option value="oldest" <?php echo ($sort === 'oldest') ? 'selected' : ''; ?>>Oldest First</option>
                            <option value="most_viewed" <?php echo ($sort === 'most_viewed') ? 'selected' : ''; ?>>Most Viewed</option>
                            <option value="title" <?php echo ($sort === 'title') ? 'selected' : ''; ?>>Title A-Z</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-primary w-100">Filter</button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Posts Table -->
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Image</th>
                                <th>Title</th>
                                <th>Author</th>
                                <th>Status</th>
                                <th>Views</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if ($posts->num_rows > 0): ?>
                                <?php while($post = $posts->fetch_assoc()): ?>
                                <tr>
                                    <td>
                                        <?php if ($post['image_path']): ?>
                                            <img src="../api/<?php echo htmlspecialchars($post['image_path']); ?>" class="img-thumbnail" style="width: 60px; height: 40px; object-fit: cover;" alt="Post Image">
                                        <?php else: ?>
                                            <div class="bg-light d-flex align-items-center justify-content-center" style="width: 60px; height: 40px; border-radius: 4px;">
                                                <i class='bx bx-image text-muted'></i>
                                            </div>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <strong><?php echo htmlspecialchars(substr($post['title'], 0, 50)) . (strlen($post['title']) > 50 ? '...' : ''); ?></strong><br>
                                        <small class="text-muted"><?php echo htmlspecialchars(substr($post['description'], 0, 80)) . (strlen($post['description']) > 80 ? '...' : ''); ?></small>
                                    </td>
                                    <td><?php echo htmlspecialchars($post['first_name'] . ' ' . $post['last_name']); ?></td>
                                    <td>
                                        <?php
                                        $status_colors = [
                                            'active' => 'success',
                                            'archived' => 'warning',
                                            'deleted' => 'danger'
                                        ];
                                        $color = $status_colors[$post['status']] ?? 'secondary';
                                        ?>
                                        <span class="badge bg-<?php echo $color; ?>"><?php echo ucfirst($post['status']); ?></span>
                                    </td>
                                    <td><?php echo number_format($post['views']); ?></td>
                                    <td><?php echo date('M d, Y', strtotime($post['created_at'])); ?></td>
                                    <td>
                                        <div class="btn-group">
                                            <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#viewPostModal<?php echo $post['id']; ?>">
                                                <i class='bx bx-show'></i>
                                            </button>
                                            <button class="btn btn-sm btn-outline-info" data-bs-toggle="modal" data-bs-target="#editPostModal<?php echo $post['id']; ?>">
                                                <i class='bx bx-edit'></i>
                                            </button>
                                            <div class="btn-group">
                                                <button class="btn btn-sm btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown">
                                                    <i class='bx bx-dots-horizontal-rounded'></i>
                                                </button>
                                                <ul class="dropdown-menu">
                                                    <li>
                                                        <form method="POST" class="d-inline">
                                                            <input type="hidden" name="post_id" value="<?php echo $post['id']; ?>">
                                                            <input type="hidden" name="action" value="change_status">
                                                            <input type="hidden" name="new_status" value="<?php echo $post['status'] === 'active' ? 'archived' : 'active'; ?>">
                                                            <button type="submit" class="dropdown-item">
                                                                <i class='bx <?php echo $post['status'] === 'active' ? 'bx-archive' : 'bx-check-circle'; ?> me-2'></i>
                                                                <?php echo $post['status'] === 'active' ? 'Archive' : 'Activate'; ?>
                                                            </button>
                                                        </form>
                                                    </li>
                                                    <li><hr class="dropdown-divider"></li>
                                                    <li>
                                                        <form method="POST" class="d-inline" onsubmit="return confirm('Are you sure you want to delete this post?')">
                                                            <input type="hidden" name="post_id" value="<?php echo $post['id']; ?>">
                                                            <input type="hidden" name="action" value="delete">
                                                            <button type="submit" class="dropdown-item text-danger">
                                                                <i class='bx bx-trash me-2'></i>Delete
                                                            </button>
                                                        </form>
                                                    </li>
                                                </ul>
                                            </div>
                                        </div>
                                    </td>
                                </tr>

                                <!-- View Post Modal -->
                                <div class="modal fade" id="viewPostModal<?php echo $post['id']; ?>" tabindex="-1">
                                    <div class="modal-dialog modal-lg">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title"><?php echo htmlspecialchars($post['title']); ?></h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <div class="modal-body">
                                                <?php if ($post['image_path']): ?>
                                                <div class="mb-3">
                                                    <img src="../api/<?php echo htmlspecialchars($post['image_path']); ?>" class="img-fluid rounded" alt="Post Image">
                                                </div>
                                                <?php endif; ?>

                                                <div class="row mb-3">
                                                    <div class="col-md-6">
                                                        <strong>Author:</strong> <?php echo htmlspecialchars($post['first_name'] . ' ' . $post['last_name']); ?><br>
                                                        <strong>Status:</strong> <span class="badge bg-<?php echo $color; ?>"><?php echo ucfirst($post['status']); ?></span><br>
                                                        <strong>Views:</strong> <?php echo number_format($post['views']); ?>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <strong>Created:</strong> <?php echo date('F j, Y g:i A', strtotime($post['created_at'])); ?><br>
                                                        <strong>Updated:</strong> <?php echo date('F j, Y g:i A', strtotime($post['updated_at'])); ?>
                                                    </div>
                                                </div>

                                                <hr>
                                                <h6>Content</h6>
                                                <div class="bg-light p-3 rounded">
                                                    <?php echo nl2br(htmlspecialchars($post['description'])); ?>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <!-- Edit Post Modal -->
                                <div class="modal fade" id="editPostModal<?php echo $post['id']; ?>" tabindex="-1">
                                    <div class="modal-dialog modal-lg">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title">Edit Post</h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <form method="POST">
                                                <div class="modal-body">
                                                    <input type="hidden" name="action" value="update">
                                                    <input type="hidden" name="post_id" value="<?php echo $post['id']; ?>">

                                                    <div class="mb-3">
                                                        <label for="edit_title_<?php echo $post['id']; ?>" class="form-label">Title</label>
                                                        <input type="text" class="form-control" id="edit_title_<?php echo $post['id']; ?>" name="title" value="<?php echo htmlspecialchars($post['title']); ?>" required>
                                                    </div>

                                                    <div class="mb-3">
                                                        <label for="edit_description_<?php echo $post['id']; ?>" class="form-label">Description</label>
                                                        <textarea class="form-control" id="edit_description_<?php echo $post['id']; ?>" name="description" rows="5" required><?php echo htmlspecialchars($post['description']); ?></textarea>
                                                    </div>
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                                                    <button type="submit" class="btn btn-primary">Update Post</button>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                                <?php endwhile; ?>
                            <?php else: ?>
                                <tr>
                                    <td colspan="7" class="text-center text-muted py-4">
                                        <i class='bx bx-news display-4'></i><br>
                                        No posts found
                                    </td>
                                </tr>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Create Post Modal -->
<div class="modal fade" id="createPostModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Create New Post</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form method="POST" enctype="multipart/form-data">
                <div class="modal-body">
                    <input type="hidden" name="action" value="create">

                    <div class="mb-3">
                        <label for="title" class="form-label">Title</label>
                        <input type="text" class="form-control" id="title" name="title" required maxlength="255">
                    </div>

                    <div class="mb-3">
                        <label for="description" class="form-label">Description</label>
                        <textarea class="form-control" id="description" name="description" rows="5" required></textarea>
                    </div>

                    <div class="mb-3">
                        <label for="image" class="form-label">Image (Optional)</label>
                        <input type="file" class="form-control" id="image" name="image" accept="image/*">
                        <div class="form-text">Supported formats: JPG, PNG, GIF. Max size: 5MB</div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Post</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
