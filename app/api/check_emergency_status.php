<?php
header('Content-Type: application/json');
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$requestId = $_GET['request_id'] ?? null;
if (!$requestId) {
    http_response_code(400);
    echo json_encode(['error' => 'Request ID is required']);
    exit();
}

try {
    // First get the emergency request details
    $stmt = $pdo->prepare("SELECT status, responder_id, latitude as user_lat, longitude as user_lng
                          FROM emergency_requests WHERE id = :request_id");
    $stmt->execute([':request_id' => $requestId]);
    $result = $stmt->fetch();

    if ($result) {
        $response = [
            'success' => true,
            'status' => $result['status'],
            'has_responder' => !empty($result['responder_id'])
        ];

        // If there's a responder, try to get their location
        if ($result['responder_id']) {
            try {
                $locStmt = $pdo->prepare("
                    SELECT latitude as responder_lat, longitude as responder_lng
                    FROM responder_locations
                    WHERE responder_id = ?
                    ORDER BY last_update DESC LIMIT 1
                ");
                $locStmt->execute([$result['responder_id']]);
                $responderLoc = $locStmt->fetch();

                if ($responderLoc) {
                    $distance = calculateDistance(
                        $result['user_lat'],
                        $result['user_lng'],
                        $responderLoc['responder_lat'],
                        $responderLoc['responder_lng']
                    );

                    $response['responder_distance'] = $distance;
                    $response['distance_message'] = getDistanceMessage($distance);
                }
            } catch (PDOException $e) {
                // Silently handle responder location errors
                // This allows the endpoint to work even if responder_locations table doesn't exist yet
                error_log("Error getting responder location: " . $e->getMessage());
            }
        }

        echo json_encode($response);
    } else {
        http_response_code(404);
        echo json_encode(['error' => 'Emergency request not found']);
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}

// Calculate distance between two points in kilometers
function calculateDistance($lat1, $lon1, $lat2, $lon2) {
    $earthRadius = 6371; // Earth's radius in kilometers

    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);

    $a = sin($dLat/2) * sin($dLat/2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon/2) * sin($dLon/2);

    $c = 2 * atan2(sqrt($a), sqrt(1-$a));
    $distance = $earthRadius * $c;

    return round($distance, 2);
}

// Get appropriate message based on distance
function getDistanceMessage($distance) {
    if ($distance <= 0.2) { // 200 meters
        return "Responder has arrived!";
    } else if ($distance <= 0.5) { // 500 meters
        return "Responder is very near!";
    } else if ($distance <= 1) { // 1 kilometer
        return "Responder is nearby";
    } else if ($distance <= 3) { // 3 kilometers
        return "Responder is approaching";
    } else {
        return "Responder is on the way";
    }
}
?>
