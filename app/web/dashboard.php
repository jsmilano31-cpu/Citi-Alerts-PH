<?php
require_once 'includes/header.php';
require_once 'config.php';

// Get total users count
$users_query = "SELECT COUNT(*) as total FROM users";
$users_result = $conn->query($users_query);
$total_users = $users_result->fetch_assoc()['total'];

// Get total reports count
$reports_query = "SELECT COUNT(*) as total FROM reports";
$reports_result = $conn->query($reports_query);
$total_reports = $reports_result->fetch_assoc()['total'];

// Get total verified users
$verified_query = "SELECT COUNT(*) as total FROM users WHERE is_verified = 1";
$verified_result = $conn->query($verified_query);
$total_verified = $verified_result->fetch_assoc()['total'];

// Get recent reports
$recent_reports_query = "SELECT r.*, u.username FROM reports r
                        JOIN users u ON r.user_id = u.id
                        ORDER BY r.created_at DESC LIMIT 5";
$recent_reports = $conn->query($recent_reports_query);
?>

<div class="main-content">
    <div class="container-fluid">
        <h2 class="mb-4">Dashboard Overview</h2>

        <!-- Statistics Cards -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-user display-4 text-primary'></i>
                    <h3 class="mt-2"><?php echo $total_users; ?></h3>
                    <p class="text-muted">Total Users</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-report display-4 text-warning'></i>
                    <h3 class="mt-2"><?php echo $total_reports; ?></h3>
                    <p class="text-muted">Total Reports</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-check-circle display-4 text-success'></i>
                    <h3 class="mt-2"><?php echo $total_verified; ?></h3>
                    <p class="text-muted">Verified Users</p>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card text-center">
                    <i class='bx bxs-user-x display-4 text-danger'></i>
                    <h3 class="mt-2"><?php echo $total_users - $total_verified; ?></h3>
                    <p class="text-muted">Pending Verification</p>
                </div>
            </div>
        </div>

        <!-- Recent Reports Table -->
        <div class="card">
            <div class="card-header">
                <h5 class="card-title mb-0">Recent Reports</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Type</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th>Date</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php while($report = $recent_reports->fetch_assoc()): ?>
                            <tr>
                                <td><?php echo htmlspecialchars($report['username']); ?></td>
                                <td><?php echo htmlspecialchars($report['type']); ?></td>
                                <td><?php echo htmlspecialchars($report['location']); ?></td>
                                <td>
                                    <span class="badge bg-<?php echo $report['status'] == 'verified' ? 'success' : 'warning'; ?>">
                                        <?php echo ucfirst($report['status']); ?>
                                    </span>
                                </td>
                                <td><?php echo date('M d, Y H:i', strtotime($report['created_at'])); ?></td>
                                <td>
                                    <a href="view-report.php?id=<?php echo $report['id']; ?>" class="btn btn-sm btn-primary">
                                        <i class='bx bxs-show'></i>
                                    </a>
                                </td>
                            </tr>
                            <?php endwhile; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</body>
</html>
