<?php
require_once 'includes/header.php';
require_once 'config.php';

// Get comprehensive statistics
$stats = [];

// User statistics
$users_query = "SELECT
    COUNT(*) as total_users,
    COUNT(CASE WHEN user_type = 'moderator' THEN 1 END) as total_moderators,
    COUNT(CASE WHEN is_verified = 1 AND user_type = 'moderator' THEN 1 END) as verified_moderators,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as new_users_month
    FROM users";
$users_result = $conn->query($users_query);
$user_stats = $users_result->fetch_assoc();

// Emergency requests statistics
$emergency_query = "SELECT
    COUNT(*) as total_emergencies,
    COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending_emergencies,
    COUNT(CASE WHEN status = 'help_coming' THEN 1 END) as active_emergencies,
    COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_emergencies,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1 END) as emergencies_today
    FROM emergency_requests";
$emergency_result = $conn->query($emergency_query);
$emergency_stats = $emergency_result->fetch_assoc();

// Posts statistics
$posts_query = "SELECT
    COUNT(*) as total_posts,
    COUNT(CASE WHEN status = 'active' THEN 1 END) as active_posts,
    SUM(views) as total_views,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 END) as posts_this_week
    FROM posts";
$posts_result = $conn->query($posts_query);
$post_stats = $posts_result->fetch_assoc();

// Feedback statistics
$feedback_query = "SELECT
    COUNT(*) as total_feedback,
    AVG(overall_rating) as avg_rating,
    COUNT(CASE WHEN would_recommend = 'Yes' THEN 1 END) as recommendations,
    COUNT(CASE WHEN feedback_category = 'Bug Report' THEN 1 END) as bug_reports
    FROM user_feedback";
$feedback_result = $conn->query($feedback_query);
$feedback_stats = $feedback_result->fetch_assoc();

// Evacuation centers
$evacuation_query = "SELECT
    COUNT(*) as total_centers,
    COUNT(CASE WHEN status = 'active' THEN 1 END) as active_centers
    FROM evacuation_centers";
$evacuation_result = $conn->query($evacuation_query);
$evacuation_stats = $evacuation_result->fetch_assoc();

// Recent emergency requests
$recent_emergencies_query = "SELECT er.*, u.username, u.first_name, u.last_name
    FROM emergency_requests er
    JOIN users u ON er.user_id = u.id
    ORDER BY er.created_at DESC LIMIT 8";
$recent_emergencies = $conn->query($recent_emergencies_query);

// Recent posts
$recent_posts_query = "SELECT p.*, u.username as moderator_name
    FROM posts p
    JOIN users u ON p.moderator_id = u.id
    ORDER BY p.created_at DESC LIMIT 5";
$recent_posts = $conn->query($recent_posts_query);

// Emergency trends (last 7 days)
$trends_query = "SELECT
    DATE(created_at) as date,
    COUNT(*) as count,
    emergency_type
    FROM emergency_requests
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
    GROUP BY DATE(created_at), emergency_type
    ORDER BY date DESC";
$trends_result = $conn->query($trends_query);
$trends_data = [];
while($row = $trends_result->fetch_assoc()) {
    $trends_data[] = $row;
}

// User registration trends (last 30 days)
$user_trends_query = "SELECT
    DATE(created_at) as date,
    COUNT(*) as count
    FROM users
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    GROUP BY DATE(created_at)
    ORDER BY date DESC";
$user_trends_result = $conn->query($user_trends_query);
$user_trends_data = [];
while($row = $user_trends_result->fetch_assoc()) {
    $user_trends_data[] = $row;
}
?>

<div class="main-content">
    <div class="container-fluid">
        <!-- Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h2 class="mb-1">Dashboard Overview</h2>
                <p class="text-muted mb-0">Welcome back! Here's what's happening with CitiAlerts PH today.</p>
            </div>
            <div class="text-end">
                <small class="text-muted">Last updated: <?php echo date('M d, Y g:i A'); ?></small>
            </div>
        </div>

        <!-- Key Metrics Row -->
        <div class="row mb-4">
            <div class="col-xl-3 col-md-6 mb-3">
                <div class="stat-card bg-gradient-primary text-white">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h3 class="mb-0"><?php echo $user_stats['total_users'] ?? 0; ?></h3>
                            <p class="mb-0 opacity-75">Total Users</p>
                            <small class="opacity-50">
                                <i class='bx bx-trending-up'></i> +<?php echo $user_stats['new_users_month'] ?? 0; ?> this month
                            </small>
                        </div>
                        <div class="stat-icon">
                            <i class='bx bxs-user-circle display-4 opacity-50'></i>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xl-3 col-md-6 mb-3">
                <div class="stat-card bg-gradient-danger text-white">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h3 class="mb-0"><?php echo $emergency_stats['pending_emergencies'] ?? 0; ?></h3>
                            <p class="mb-0 opacity-75">Pending Emergencies</p>
                            <small class="opacity-50">
                                <i class='bx bx-time'></i> <?php echo $emergency_stats['emergencies_today'] ?? 0; ?> today
                            </small>
                        </div>
                        <div class="stat-icon">
                            <i class='bx bxs-error display-4 opacity-50'></i>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xl-3 col-md-6 mb-3">
                <div class="stat-card bg-gradient-success text-white">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h3 class="mb-0"><?php echo $emergency_stats['completed_emergencies'] ?? 0; ?></h3>
                            <p class="mb-0 opacity-75">Resolved Cases</p>
                            <small class="opacity-50">
                                <i class='bx bx-check-circle'></i> Total completed
                            </small>
                        </div>
                        <div class="stat-icon">
                            <i class='bx bxs-check-circle display-4 opacity-50'></i>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xl-3 col-md-6 mb-3">
                <div class="stat-card bg-gradient-info text-white">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h3 class="mb-0"><?php echo number_format($feedback_stats['avg_rating'] ?? 0, 1); ?>/5</h3>
                            <p class="mb-0 opacity-75">App Rating</p>
                            <small class="opacity-50">
                                <i class='bx bx-star'></i> <?php echo $feedback_stats['total_feedback'] ?? 0; ?> reviews
                            </small>
                        </div>
                        <div class="stat-icon">
                            <i class='bx bxs-star display-4 opacity-50'></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Secondary Metrics Row -->
        <div class="row mb-4">
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-primary border-4">
                    <h4 class="text-primary mb-1"><?php echo $user_stats['verified_moderators'] ?? 0; ?></h4>
                    <small class="text-muted">Verified Moderators</small>
                </div>
            </div>
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-success border-4">
                    <h4 class="text-success mb-1"><?php echo $post_stats['active_posts'] ?? 0; ?></h4>
                    <small class="text-muted">Active Posts</small>
                </div>
            </div>
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-info border-4">
                    <h4 class="text-info mb-1"><?php echo number_format($post_stats['total_views'] ?? 0); ?></h4>
                    <small class="text-muted">Total Views</small>
                </div>
            </div>
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-warning border-4">
                    <h4 class="text-warning mb-1"><?php echo $evacuation_stats['active_centers'] ?? 0; ?></h4>
                    <small class="text-muted">Evacuation Centers</small>
                </div>
            </div>
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-danger border-4">
                    <h4 class="text-danger mb-1"><?php echo $feedback_stats['bug_reports'] ?? 0; ?></h4>
                    <small class="text-muted">Bug Reports</small>
                </div>
            </div>
            <div class="col-xl-2 col-md-4 col-6 mb-3">
                <div class="stat-card text-center border-start border-success border-4">
                    <h4 class="text-success mb-1"><?php echo $feedback_stats['recommendations'] ?? 0; ?></h4>
                    <small class="text-muted">Recommendations</small>
                </div>
            </div>
        </div>

        <!-- Charts and Analytics Row -->
        <div class="row mb-4">
            <div class="col-xl-8 mb-4">
                <div class="card h-100">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            <i class='bx bx-trending-up text-primary me-2'></i>Emergency Requests Trend (Last 7 Days)
                        </h5>
                        <div class="dropdown">
                            <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                                <i class='bx bx-dots-horizontal-rounded'></i>
                            </button>
                            <ul class="dropdown-menu">
                                <li><a class="dropdown-item" href="manage-reports.php">View All Reports</a></li>
                                <li><a class="dropdown-item" href="#">Export Data</a></li>
                            </ul>
                        </div>
                    </div>
                    <div class="card-body">
                        <div style="position: relative; height: 300px;">
                            <canvas id="emergencyTrendsChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xl-4 mb-4">
                <div class="card h-100">
                    <div class="card-header">
                        <h5 class="card-title mb-0">
                            <i class='bx bx-pie-chart text-success me-2'></i>Emergency Status Distribution
                        </h5>
                    </div>
                    <div class="card-body">
                        <div style="position: relative; height: 250px;">
                            <canvas id="statusPieChart"></canvas>
                        </div>
                        <div class="mt-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <div class="d-flex align-items-center">
                                    <div class="status-indicator bg-warning me-2"></div>
                                    <small>Pending</small>
                                </div>
                                <strong><?php echo $emergency_stats['pending_emergencies'] ?? 0; ?></strong>
                            </div>
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <div class="d-flex align-items-center">
                                    <div class="status-indicator bg-info me-2"></div>
                                    <small>Help Coming</small>
                                </div>
                                <strong><?php echo $emergency_stats['active_emergencies'] ?? 0; ?></strong>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <div class="d-flex align-items-center">
                                    <div class="status-indicator bg-success me-2"></div>
                                    <small>Completed</small>
                                </div>
                                <strong><?php echo $emergency_stats['completed_emergencies'] ?? 0; ?></strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Recent Activity Row -->
        <div class="row">
            <div class="col-xl-8 mb-4">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            <i class='bx bx-alarm text-danger me-2'></i>Recent Emergency Requests
                        </h5>
                        <a href="manage-reports.php" class="btn btn-sm btn-outline-primary">View All</a>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>User</th>
                                        <th>Emergency Type</th>
                                        <th>Location</th>
                                        <th>Status</th>
                                        <th>Time</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <?php if ($recent_emergencies->num_rows > 0): ?>
                                        <?php while($emergency = $recent_emergencies->fetch_assoc()): ?>
                                        <tr>
                                            <td>
                                                <div>
                                                    <strong><?php echo htmlspecialchars($emergency['first_name'] . ' ' . $emergency['last_name']); ?></strong><br>
                                                    <small class="text-muted">@<?php echo htmlspecialchars($emergency['username']); ?></small>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="badge bg-danger"><?php echo htmlspecialchars($emergency['emergency_type']); ?></span>
                                            </td>
                                            <td>
                                                <small><?php echo htmlspecialchars($emergency['location_name'] ?? 'Location not provided'); ?></small>
                                            </td>
                                            <td>
                                                <?php
                                                $status_colors = [
                                                    'pending' => 'warning',
                                                    'help_coming' => 'info',
                                                    'completed' => 'success',
                                                    'cancelled' => 'secondary'
                                                ];
                                                $color = $status_colors[$emergency['status']] ?? 'secondary';
                                                ?>
                                                <span class="badge bg-<?php echo $color; ?>"><?php echo ucfirst(str_replace('_', ' ', $emergency['status'])); ?></span>
                                            </td>
                                            <td><?php echo date('M d, g:i A', strtotime($emergency['created_at'])); ?></td>
                                            <td>
                                                <button class="btn btn-sm btn-outline-primary" onclick="viewEmergency(<?php echo $emergency['id']; ?>)">
                                                    <i class='bx bx-show'></i>
                                                </button>
                                            </td>
                                        </tr>
                                        <?php endwhile; ?>
                                    <?php else: ?>
                                        <tr>
                                            <td colspan="6" class="text-center text-muted py-4">
                                                <i class='bx bx-info-circle display-4'></i><br>
                                                No emergency requests found
                                            </td>
                                        </tr>
                                    <?php endif; ?>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xl-4 mb-4">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            <i class='bx bx-news text-info me-2'></i>Recent Posts
                        </h5>
                        <a href="manage-posts.php" class="btn btn-sm btn-outline-primary">View All</a>
                    </div>
                    <div class="card-body">
                        <?php if ($recent_posts->num_rows > 0): ?>
                            <?php while($post = $recent_posts->fetch_assoc()): ?>
                            <div class="d-flex align-items-start mb-3 pb-3 border-bottom">
                                <div class="flex-shrink-0">
                                    <div class="avatar-sm bg-primary rounded-circle d-flex align-items-center justify-content-center">
                                        <i class='bx bx-news text-white'></i>
                                    </div>
                                </div>
                                <div class="flex-grow-1 ms-3">
                                    <h6 class="mb-1"><?php echo htmlspecialchars(substr($post['title'], 0, 50)) . (strlen($post['title']) > 50 ? '...' : ''); ?></h6>
                                    <p class="text-muted mb-1 small"><?php echo htmlspecialchars(substr($post['description'], 0, 80)) . (strlen($post['description']) > 80 ? '...' : ''); ?></p>
                                    <div class="d-flex justify-content-between align-items-center">
                                        <small class="text-muted">By <?php echo htmlspecialchars($post['moderator_name']); ?></small>
                                        <div class="d-flex align-items-center">
                                            <small class="text-muted me-2">
                                                <i class='bx bx-show'></i> <?php echo $post['views']; ?>
                                            </small>
                                            <small class="text-muted"><?php echo date('M d', strtotime($post['created_at'])); ?></small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <?php endwhile; ?>
                        <?php else: ?>
                            <div class="text-center text-muted py-4">
                                <i class='bx bx-news display-4'></i><br>
                                <p>No posts available</p>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Custom Styles -->
<style>
.bg-gradient-primary {
    background: linear-gradient(45deg, #4e73df, #224abe);
}
.bg-gradient-success {
    background: linear-gradient(45deg, #1cc88a, #13855c);
}
.bg-gradient-info {
    background: linear-gradient(45deg, #36b9cc, #258391);
}
.bg-gradient-danger {
    background: linear-gradient(45deg, #e74a3b, #be2617);
}
.stat-card {
    background: white;
    border-radius: 15px;
    padding: 1.5rem;
    box-shadow: 0 0.15rem 1.75rem 0 rgba(58, 59, 69, 0.15);
    transition: all 0.3s;
    border: 0;
    height: 100%;
}
.stat-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 0.25rem 2rem 0 rgba(58, 59, 69, 0.2);
}
.stat-icon {
    opacity: 0.1;
}
.status-indicator {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    display: inline-block;
}
.avatar-sm {
    width: 32px;
    height: 32px;
}
.card {
    border: 0;
    box-shadow: 0 0.15rem 1.75rem 0 rgba(58, 59, 69, 0.15);
    border-radius: 15px;
}
.card-header {
    background: transparent;
    border-bottom: 1px solid rgba(0,0,0,0.05);
    border-radius: 15px 15px 0 0 !important;
}
</style>

<!-- Chart.js Scripts -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
// Emergency Trends Chart
const emergencyCtx = document.getElementById('emergencyTrendsChart').getContext('2d');
const emergencyChart = new Chart(emergencyCtx, {
    type: 'line',
    data: {
        labels: <?php echo json_encode(array_reverse(array_column($trends_data, 'date'))); ?>,
        datasets: [{
            label: 'Emergency Requests',
            data: <?php echo json_encode(array_reverse(array_column($trends_data, 'count'))); ?>,
            borderColor: '#e74a3b',
            backgroundColor: 'rgba(231, 74, 59, 0.1)',
            borderWidth: 3,
            fill: true,
            tension: 0.4
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                display: false
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                grid: {
                    color: 'rgba(0,0,0,0.05)'
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        }
    }
});

// Status Pie Chart
const statusCtx = document.getElementById('statusPieChart').getContext('2d');
const statusChart = new Chart(statusCtx, {
    type: 'doughnut',
    data: {
        labels: ['Pending', 'Help Coming', 'Completed'],
        datasets: [{
            data: [
                <?php echo $emergency_stats['pending_emergencies'] ?? 0; ?>,
                <?php echo $emergency_stats['active_emergencies'] ?? 0; ?>,
                <?php echo $emergency_stats['completed_emergencies'] ?? 0; ?>
            ],
            backgroundColor: ['#f6c23e', '#36b9cc', '#1cc88a'],
            borderWidth: 0
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                display: false
            }
        }
    }
});

function viewEmergency(id) {
    // Add functionality to view emergency details
    alert('View emergency details for ID: ' + id);
}
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
