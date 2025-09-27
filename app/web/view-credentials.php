<?php
require_once 'includes/header.php';
require_once 'config.php';

if (!isset($_GET['user_id'])) {
    header("Location: manage-users.php");
    exit();
}

$user_id = $conn->real_escape_string($_GET['user_id']);
$user_query = "SELECT * FROM users WHERE id = $user_id";
$user_result = $conn->query($user_query);
$user = $user_result->fetch_assoc();

// Handle credential upload
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['credentials'])) {
    $target_dir = "uploads/credentials/";
    if (!file_exists($target_dir)) {
        mkdir($target_dir, 0777, true);
    }

    $file_extension = strtolower(pathinfo($_FILES["credentials"]["name"], PATHINFO_EXTENSION));
    $new_filename = "moderator_" . $user_id . "_" . time() . "." . $file_extension;
    $target_file = $target_dir . $new_filename;

    if (move_uploaded_file($_FILES["credentials"]["tmp_name"], $target_file)) {
        // Update database with new credential file
        $update_query = "UPDATE users SET credential_file = ? WHERE id = ?";
        $stmt = $conn->prepare($update_query);
        $stmt->bind_param("si", $new_filename, $user_id);
        $stmt->execute();
        $success_message = "Credentials uploaded successfully.";
    } else {
        $error_message = "Sorry, there was an error uploading your file.";
    }
}
?>

<div class="main-content">
    <div class="container-fluid">
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">Moderator Credentials - <?php echo htmlspecialchars($user['username']); ?></h3>
            </div>
            <div class="card-body">
                <?php if (isset($success_message)): ?>
                    <div class="alert alert-success"><?php echo $success_message; ?></div>
                <?php endif; ?>
                <?php if (isset($error_message)): ?>
                    <div class="alert alert-danger"><?php echo $error_message; ?></div>
                <?php endif; ?>

                <div class="row">
                    <div class="col-md-6">
                        <h4>User Details</h4>
                        <table class="table">
                            <tr>
                                <th>Username:</th>
                                <td><?php echo htmlspecialchars($user['username']); ?></td>
                            </tr>
                            <tr>
                                <th>Full Name:</th>
                                <td><?php echo htmlspecialchars($user['first_name'] . ' ' . $user['last_name']); ?></td>
                            </tr>
                            <tr>
                                <th>Email:</th>
                                <td><?php echo htmlspecialchars($user['email']); ?></td>
                            </tr>
                            <tr>
                                <th>Role:</th>
                                <td><?php echo htmlspecialchars($user['user_type']); ?></td>
                            </tr>
                            <tr>
                                <th>Status:</th>
                                <td>
                                    <span class="badge bg-<?php echo $user['is_verified'] ? 'success' : 'warning'; ?>">
                                        <?php echo $user['is_verified'] ? 'Verified' : 'Unverified'; ?>
                                    </span>
                                </td>
                            </tr>
                        </table>
                    </div>
                    <div class="col-md-6">
                        <h4>Credentials</h4>
                        <?php if ($user['credential_file']): ?>
                            <div class="mb-3">
                                <h5>Current Credentials:</h5>
                                <?php
                                $file_path = "uploads/credentials/" . $user['credential_file'];
                                $file_extension = strtolower(pathinfo($user['credential_file'], PATHINFO_EXTENSION));
                                if (in_array($file_extension, ['jpg', 'jpeg', 'png', 'gif'])):
                                ?>
                                    <img src="<?php echo $file_path; ?>" class="img-fluid mb-3" style="max-width: 300px;">
                                <?php else: ?>
                                    <p>
                                        <a href="<?php echo $file_path; ?>" class="btn btn-primary" target="_blank">
                                            <i class='bx bx-file'></i> View Document
                                        </a>
                                    </p>
                                <?php endif; ?>
                            </div>
                        <?php endif; ?>

                        <form method="POST" enctype="multipart/form-data">
                            <div class="mb-3">
                                <label for="credentials" class="form-label">Upload New Credentials</label>
                                <input type="file" class="form-control" id="credentials" name="credentials" accept=".jpg,.jpeg,.png,.pdf,.doc,.docx">
                            </div>
                            <button type="submit" class="btn btn-primary">Upload Credentials</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
