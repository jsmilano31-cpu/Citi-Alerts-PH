<?php
session_start();
if (!isset($_SESSION['admin_id'])) {
    header("Location: index.php");
    exit();
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CitiAlertsPH Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/boxicons@2.1.4/css/boxicons.min.css" rel="stylesheet">
    <style>
        .sidebar {
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            width: 240px;
            background: #2c3e50;
            padding: 20px;
            transition: all 0.3s ease;
        }
        .sidebar .logo {
            color: white;
            font-size: 24px;
            text-align: center;
            margin-bottom: 30px;
        }
        .sidebar .nav-link {
            color: rgba(255,255,255,0.8);
            padding: 12px 15px;
            border-radius: 8px;
            margin-bottom: 5px;
        }
        .sidebar .nav-link:hover,
        .sidebar .nav-link.active {
            background: rgba(255,255,255,0.1);
            color: white;
        }
        .sidebar .nav-link i {
            margin-right: 10px;
        }
        .main-content {
            margin-left: 240px;
            padding: 20px;
        }
        .stat-card {
            background: white;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        .stat-card:hover {
            transform: translateY(-5px);
        }
    </style>
</head>
<body class="bg-light">
    <div class="sidebar">
        <div class="logo">
            CitiAlertsPH
        </div>
        <nav class="nav flex-column">
            <a href="dashboard.php" class="nav-link <?php echo basename($_SERVER['PHP_SELF']) == 'dashboard.php' ? 'active' : ''; ?>">
                <i class='bx bxs-dashboard'></i> Dashboard
            </a>
            <a href="manage-users.php" class="nav-link <?php echo basename($_SERVER['PHP_SELF']) == 'manage-users.php' ? 'active' : ''; ?>">
                <i class='bx bxs-user-detail'></i> Manage Users
            </a>
            <a href="manage-reports.php" class="nav-link <?php echo basename($_SERVER['PHP_SELF']) == 'manage-reports.php' ? 'active' : ''; ?>">
                <i class='bx bxs-report'></i> Manage Reports
            </a>
            <a href="manage-posts.php" class="nav-link <?php echo basename($_SERVER['PHP_SELF']) == 'manage-posts.php' ? 'active' : ''; ?>">
                <i class='bx bxs-news'></i> Manage Posts
            </a>
            <a href="manage-locations.php" class="nav-link <?php echo basename($_SERVER['PHP_SELF']) == 'manage-locations.php' ? 'active' : ''; ?>">
                <i class='bx bxs-map'></i> Manage Locations
            </a>
            <div class="mt-auto">
                <a href="logout.php" class="nav-link text-danger">
                    <i class='bx bxs-log-out'></i> Logout
                </a>
            </div>
        </nav>
    </div>
</head>
<body>
