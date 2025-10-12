<?php
require_once 'includes/header.php';
require_once 'config.php';

// Handle feedback actions
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['action'])) {
        $feedback_id = $conn->real_escape_string($_POST['feedback_id']);

        switch ($_POST['action']) {
            case 'delete':
                $conn->query("DELETE FROM user_feedback WHERE id = $feedback_id");
                break;
        }
    }
}

// Get feedback with search and filtering
$search = isset($_GET['search']) ? $conn->real_escape_string($_GET['search']) : '';
$filter = isset($_GET['filter']) ? $conn->real_escape_string($_GET['filter']) : 'all';
$rating_filter = isset($_GET['rating']) ? $conn->real_escape_string($_GET['rating']) : 'all';

$query = "SELECT * FROM user_feedback WHERE 1=1";
if ($search) {
    $query .= " AND (user_name LIKE '%$search%' OR user_email LIKE '%$search%' OR comments LIKE '%$search%')";
}
if ($filter !== 'all') {
    $query .= " AND feedback_category = '$filter'";
}
if ($rating_filter !== 'all') {
    $query .= " AND overall_rating = $rating_filter";
}
$query .= " ORDER BY created_at DESC";

$feedbacks = $conn->query($query);

// Get statistics
$stats_query = "SELECT
    COUNT(*) as total_feedback,
    AVG(overall_rating) as avg_rating,
    COUNT(CASE WHEN would_recommend = 'Yes' THEN 1 END) as would_recommend_yes,
    COUNT(CASE WHEN feedback_category = 'Bug Report' THEN 1 END) as bug_reports,
    COUNT(CASE WHEN feedback_category = 'Compliment' THEN 1 END) as compliments
    FROM user_feedback";
$stats_result = $conn->query($stats_query);
$stats = $stats_result->fetch_assoc();
?>

<div class="main-content">
    <div class="container-fluid">
        <h2 class="mb-4">User Feedback & Reports</h2>

        <!-- Statistics Cards -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-message-dots display-4 text-primary'></i>
                    <h3 class="mt-2"><?php echo $stats['total_feedback'] ?? 0; ?></h3>
                    <p class="text-muted">Total Feedback</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-star display-4 text-warning'></i>
                    <h3 class="mt-2"><?php echo number_format($stats['avg_rating'] ?? 0, 1); ?>/5</h3>
                    <p class="text-muted">Average Rating</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-like display-4 text-success'></i>
                    <h3 class="mt-2"><?php echo $stats['would_recommend_yes'] ?? 0; ?></h3>
                    <p class="text-muted">Would Recommend</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-bug display-4 text-danger'></i>
                    <h3 class="mt-2"><?php echo $stats['bug_reports'] ?? 0; ?></h3>
                    <p class="text-muted">Bug Reports</p>
                </div>
            </div>
        </div>

        <!-- Search and Filter Section -->
        <div class="card mb-4">
            <div class="card-body">
                <form class="row g-3">
                    <div class="col-md-4">
                        <input type="text" name="search" class="form-control" placeholder="Search feedback..." value="<?php echo htmlspecialchars($_GET['search'] ?? ''); ?>">
                    </div>
                    <div class="col-md-3">
                        <select name="filter" class="form-select">
                            <option value="all" <?php echo ($filter === 'all') ? 'selected' : ''; ?>>All Categories</option>
                            <option value="Bug Report" <?php echo ($filter === 'Bug Report') ? 'selected' : ''; ?>>Bug Reports</option>
                            <option value="Feature Request" <?php echo ($filter === 'Feature Request') ? 'selected' : ''; ?>>Feature Requests</option>
                            <option value="General Feedback" <?php echo ($filter === 'General Feedback') ? 'selected' : ''; ?>>General Feedback</option>
                            <option value="Complaint" <?php echo ($filter === 'Complaint') ? 'selected' : ''; ?>>Complaints</option>
                            <option value="Compliment" <?php echo ($filter === 'Compliment') ? 'selected' : ''; ?>>Compliments</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select name="rating" class="form-select">
                            <option value="all" <?php echo ($rating_filter === 'all') ? 'selected' : ''; ?>>All Ratings</option>
                            <option value="5" <?php echo ($rating_filter === '5') ? 'selected' : ''; ?>>5 Stars</option>
                            <option value="4" <?php echo ($rating_filter === '4') ? 'selected' : ''; ?>>4 Stars</option>
                            <option value="3" <?php echo ($rating_filter === '3') ? 'selected' : ''; ?>>3 Stars</option>
                            <option value="2" <?php echo ($rating_filter === '2') ? 'selected' : ''; ?>>2 Stars</option>
                            <option value="1" <?php echo ($rating_filter === '1') ? 'selected' : ''; ?>>1 Star</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-primary w-100">Filter</button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Feedback Table -->
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Category</th>
                                <th>Overall Rating</th>
                                <th>Feature Ratings</th>
                                <th>Recommend</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if ($feedbacks->num_rows > 0): ?>
                                <?php while($feedback = $feedbacks->fetch_assoc()): ?>
                                <tr>
                                    <td>
                                        <div>
                                            <strong><?php echo htmlspecialchars($feedback['user_name']); ?></strong><br>
                                            <small class="text-muted"><?php echo htmlspecialchars($feedback['user_email']); ?></small>
                                        </div>
                                    </td>
                                    <td>
                                        <?php
                                        $category_colors = [
                                            'Bug Report' => 'danger',
                                            'Feature Request' => 'info',
                                            'General Feedback' => 'secondary',
                                            'Complaint' => 'warning',
                                            'Compliment' => 'success'
                                        ];
                                        $color = $category_colors[$feedback['feedback_category']] ?? 'secondary';
                                        ?>
                                        <span class="badge bg-<?php echo $color; ?>"><?php echo htmlspecialchars($feedback['feedback_category']); ?></span>
                                    </td>
                                    <td>
                                        <div class="d-flex align-items-center">
                                            <?php for($i = 1; $i <= 5; $i++): ?>
                                                <i class='bx bxs-star text-<?php echo $i <= $feedback['overall_rating'] ? 'warning' : 'muted'; ?>'></i>
                                            <?php endfor; ?>
                                            <span class="ms-2">(<?php echo $feedback['overall_rating']; ?>/5)</span>
                                        </div>
                                    </td>
                                    <td>
                                        <small>
                                            Emergency: <?php echo $feedback['emergency_alerts_rating'] ?? 'N/A'; ?>/5<br>
                                            Evacuation: <?php echo $feedback['evacuation_centers_rating'] ?? 'N/A'; ?>/5<br>
                                            News: <?php echo $feedback['news_updates_rating'] ?? 'N/A'; ?>/5<br>
                                            Performance: <?php echo $feedback['app_performance_rating'] ?? 'N/A'; ?>/5
                                        </small>
                                    </td>
                                    <td>
                                        <?php
                                        $recommend_colors = [
                                            'Yes' => 'success',
                                            'No' => 'danger',
                                            'Maybe' => 'warning'
                                        ];
                                        $recommend_color = $recommend_colors[$feedback['would_recommend']] ?? 'secondary';
                                        ?>
                                        <span class="badge bg-<?php echo $recommend_color; ?>"><?php echo $feedback['would_recommend']; ?></span>
                                    </td>
                                    <td><?php echo date('M d, Y', strtotime($feedback['created_at'])); ?></td>
                                    <td>
                                        <button class="btn btn-sm btn-info" data-bs-toggle="modal" data-bs-target="#viewModal<?php echo $feedback['id']; ?>">
                                            <i class='bx bx-show'></i>
                                        </button>
                                        <form method="POST" class="d-inline" onsubmit="return confirm('Are you sure you want to delete this feedback?')">
                                            <input type="hidden" name="feedback_id" value="<?php echo $feedback['id']; ?>">
                                            <input type="hidden" name="action" value="delete">
                                            <button type="submit" class="btn btn-sm btn-danger">
                                                <i class='bx bx-trash'></i>
                                            </button>
                                        </form>
                                    </td>
                                </tr>

                                <!-- View Modal -->
                                <div class="modal fade" id="viewModal<?php echo $feedback['id']; ?>" tabindex="-1">
                                    <div class="modal-dialog modal-lg">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title">Feedback Details</h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="row">
                                                    <div class="col-md-6">
                                                        <h6>User Information</h6>
                                                        <p><strong>Name:</strong> <?php echo htmlspecialchars($feedback['user_name']); ?></p>
                                                        <p><strong>Email:</strong> <?php echo htmlspecialchars($feedback['user_email']); ?></p>
                                                        <p><strong>Date:</strong> <?php echo date('F j, Y g:i A', strtotime($feedback['created_at'])); ?></p>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <h6>App Information</h6>
                                                        <p><strong>Version:</strong> <?php echo htmlspecialchars($feedback['app_version'] ?? 'Not provided'); ?></p>
                                                        <p><strong>Device:</strong> <?php echo htmlspecialchars($feedback['device_info'] ?? 'Not provided'); ?></p>
                                                    </div>
                                                </div>

                                                <hr>

                                                <div class="row">
                                                    <div class="col-md-6">
                                                        <h6>Ratings</h6>
                                                        <p><strong>Overall:</strong> <?php echo $feedback['overall_rating']; ?>/5</p>
                                                        <p><strong>Emergency Alerts:</strong> <?php echo $feedback['emergency_alerts_rating'] ?? 'Not rated'; ?>/5</p>
                                                        <p><strong>Evacuation Centers:</strong> <?php echo $feedback['evacuation_centers_rating'] ?? 'Not rated'; ?>/5</p>
                                                        <p><strong>News Updates:</strong> <?php echo $feedback['news_updates_rating'] ?? 'Not rated'; ?>/5</p>
                                                        <p><strong>App Performance:</strong> <?php echo $feedback['app_performance_rating'] ?? 'Not rated'; ?>/5</p>
                                                    </div>
                                                    <div class="col-md-6">
                                                        <h6>Feedback Details</h6>
                                                        <p><strong>Category:</strong> <?php echo htmlspecialchars($feedback['feedback_category']); ?></p>
                                                        <p><strong>Would Recommend:</strong> <?php echo htmlspecialchars($feedback['would_recommend']); ?></p>
                                                    </div>
                                                </div>

                                                <?php if (!empty($feedback['comments'])): ?>
                                                <hr>
                                                <h6>Comments</h6>
                                                <div class="bg-light p-3 rounded">
                                                    <?php echo nl2br(htmlspecialchars($feedback['comments'])); ?>
                                                </div>
                                                <?php endif; ?>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <?php endwhile; ?>
                            <?php else: ?>
                                <tr>
                                    <td colspan="7" class="text-center text-muted py-4">
                                        <i class='bx bx-message-dots display-1'></i><br>
                                        No feedback found
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

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
