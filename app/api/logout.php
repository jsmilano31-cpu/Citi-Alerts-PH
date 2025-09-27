<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Content-Type: application/json; charset=UTF-8");

include_once "config.php";

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents("php://input"));

    if (isset($data->user_id)) {
        $user_id = $data->user_id;

        // You might want to do additional cleanup here
        // For example: clear active sessions, update last_logout timestamp, etc.
        $query = "UPDATE users SET last_logout = NOW() WHERE id = ?";
        $stmt = $conn->prepare($query);
        $stmt->bind_param("i", $user_id);

        if ($stmt->execute()) {
            echo json_encode([
                "success" => true,
                "message" => "Logged out successfully"
            ]);
        } else {
            echo json_encode([
                "success" => false,
                "message" => "Error updating logout time"
            ]);
        }
    } else {
        echo json_encode([
            "success" => false,
            "message" => "User ID not provided"
        ]);
    }
} else {
    echo json_encode([
        "success" => false,
        "message" => "Invalid request method"
    ]);
}

$conn->close();
?>
