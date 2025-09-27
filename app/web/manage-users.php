<?php
require_once 'includes/header.php';
require_once 'config.php';

// Handle user actions
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['action'])) {
        $user_id = $conn->real_escape_string($_POST['user_id']);

        switch ($_POST['action']) {
            case 'verify':
                $conn->query("UPDATE users SET is_verified = 1 WHERE id = $user_id");
                break;
            case 'unverify':
                $conn->query("UPDATE users SET is_verified = 0 WHERE id = $user_id");
                break;
            case 'delete':
                $conn->query("DELETE FROM users WHERE id = $user_id");
                break;
            case 'change_role':
                $new_role = $conn->real_escape_string($_POST['new_role']);
                $conn->query("UPDATE users SET user_type = '$new_role' WHERE id = $user_id");
                break;
        }
    }
}

// Get users with search and filtering
$search = isset($_GET['search']) ? $conn->real_escape_string($_GET['search']) : '';
$filter = isset($_GET['filter']) ? $conn->real_escape_string($_GET['filter']) : 'all';

$query = "SELECT * FROM users WHERE 1=1";
if ($search) {
    $query .= " AND (username LIKE '%$search%' OR email LIKE '%$search%' OR first_name LIKE '%$search%' OR last_name LIKE '%$search%')";
}
if ($filter !== 'all') {
    if ($filter === 'verified') {
        $query .= " AND is_verified = 1 AND user_type = 'moderator'";
    } else if ($filter === 'unverified') {
        $query .= " AND is_verified = 0 AND user_type = 'moderator'";
    } else if ($filter === 'moderator') {
        $query .= " AND user_type = 'moderator'";
    }
}
$query .= " ORDER BY created_at DESC";

$users = $conn->query($query);
?>

<div class="main-content">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Manage Users</h2>
            <div class="d-flex gap-2">
                <form class="d-flex gap-2">
                    <input type="text" name="search" class="form-control" placeholder="Search users..." value="<?php echo htmlspecialchars($_GET['search'] ?? ''); ?>">
                    <select name="filter" class="form-select">
                        <option value="all" <?php echo ($filter === 'all') ? 'selected' : ''; ?>>All Users</option>
                        <option value="moderator" <?php echo ($filter === 'moderator') ? 'selected' : ''; ?>>All Moderators</option>
                        <option value="verified" <?php echo ($filter === 'verified') ? 'selected' : ''; ?>>Verified Moderators</option>
                        <option value="unverified" <?php echo ($filter === 'unverified') ? 'selected' : ''; ?>>Unverified Moderators</option>
                    </select>
                    <button type="submit" class="btn btn-primary">Filter</button>
                </form>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Username</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Joined</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php while($user = $users->fetch_assoc()): ?>
                            <tr>
                                <td><?php echo htmlspecialchars($user['username']); ?></td>
                                <td><?php echo htmlspecialchars($user['first_name'] . ' ' . $user['last_name']); ?></td>
                                <td><?php echo htmlspecialchars($user['email']); ?></td>
                                <td>
                                    <form method="POST" class="d-inline">
                                        <input type="hidden" name="user_id" value="<?php echo $user['id']; ?>">
                                        <input type="hidden" name="action" value="change_role">
                                        <select name="new_role" class="form-select form-select-sm" onchange="this.form.submit()">
                                            <option value="user" <?php echo $user['user_type'] === 'user' ? 'selected' : ''; ?>>User</option>
                                            <option value="moderator" <?php echo $user['user_type'] === 'moderator' ? 'selected' : ''; ?>>Moderator</option>
                                        </select>
                                    </form>
                                </td>
                                <td>
                                    <?php if ($user['user_type'] === 'moderator'): ?>
                                        <span class="badge bg-<?php echo $user['is_verified'] ? 'success' : 'warning'; ?>">
                                            <?php echo $user['is_verified'] ? 'Verified' : 'Unverified'; ?>
                                        </span>
                                    <?php else: ?>
                                        <span class="badge bg-info">Regular User</span>
                                    <?php endif; ?>
                                </td>
                                <td><?php echo date('M d, Y', strtotime($user['created_at'])); ?></td>
                                <td>
                                    <div class="btn-group">
                                        <?php if ($user['user_type'] === 'moderator'): ?>
                                            <a href="view-credentials.php?user_id=<?php echo $user['id']; ?>" class="btn btn-sm btn-info" title="View Credentials">
                                                <i class='bx bx-file'></i>
                                            </a>
                                            <form method="POST" class="d-inline">
                                                <input type="hidden" name="user_id" value="<?php echo $user['id']; ?>">
                                                <?php if (!$user['is_verified']): ?>
                                                    <button type="submit" name="action" value="verify" class="btn btn-sm btn-success" title="Verify Moderator">
                                                        <i class='bx bx-check'></i>
                                                    </button>
                                                <?php else: ?>
                                                    <button type="submit" name="action" value="unverify" class="btn btn-sm btn-warning" title="Unverify Moderator">
                                                        <i class='bx bx-x'></i>
                                                    </button>
                                                <?php endif; ?>
                                            </form>
                                        <?php endif; ?>
                                        <form method="POST" class="d-inline">
                                            <input type="hidden" name="user_id" value="<?php echo $user['id']; ?>">
                                            <button type="submit" name="action" value="delete" class="btn btn-sm btn-danger" onclick="return confirm('Are you sure you want to delete this user?')" title="Delete User">
                                                <i class='bx bx-trash'></i>
                                            </button>
                                        </form>
                                    </div>
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
</body>
</html>
