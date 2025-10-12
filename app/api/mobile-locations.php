<?php
// mobile-locations.php
// Mobile API endpoint for fetching evacuation centers

// Database configuration for mobile endpoints
$host = 'localhost';
$dbname = 'u847001018_citialerts';
$username = 'u847001018_spencer';
$password = 'SpencerMil@no123';

// Create MySQLi connection directly
try {
    $conn = new mysqli($host, $username, $password, $dbname);

    // Check connection
    if ($conn->connect_error) {
        throw new Exception("Connection failed: " . $conn->connect_error);
    }

    // Set charset
    $conn->set_charset("utf8mb4");

} catch (Exception $e) {
    error_log("Mobile locations DB connection error: " . $e->getMessage());
    header('Content-Type: application/json');
    echo json_encode(['success' => false, 'error' => 'Database connection failed']);
    exit();
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

function respond($data, $code = 200) {
    http_response_code($code);
    echo json_encode($data);
    exit;
}

// Add debugging function
function debug_log($message) {
    error_log("Mobile-locations DEBUG: " . $message);
}

debug_log("API endpoint called - Method: " . $_SERVER['REQUEST_METHOD']);

try {
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        debug_log("Processing GET request");

        // Check if database connection exists
        if (!isset($conn) || $conn === null) {
            debug_log("Database connection not available");
            respond(['success' => false, 'error' => 'Database connection not available'], 500);
        }

        debug_log("Database connection exists");

        // Test database connection
        if ($conn->connect_error) {
            debug_log("Database connection error: " . $conn->connect_error);
            respond(['success' => false, 'error' => 'Database connection failed: ' . $conn->connect_error], 500);
        }

        debug_log("Database connection is working");

        // First, check if table exists
        $tableCheck = $conn->query("SHOW TABLES LIKE 'evacuation_centers'");
        if ($tableCheck->num_rows == 0) {
            debug_log("evacuation_centers table does not exist");
            respond(['success' => false, 'error' => 'evacuation_centers table does not exist'], 500);
        }

        debug_log("evacuation_centers table exists");

        // Check total records in table
        $countQuery = "SELECT COUNT(*) as total FROM evacuation_centers";
        $countResult = $conn->query($countQuery);
        $totalRecords = $countResult->fetch_assoc()['total'];
        debug_log("Total records in evacuation_centers: " . $totalRecords);

        // Check active records
        $activeCountQuery = "SELECT COUNT(*) as active_total FROM evacuation_centers WHERE status = 'active'";
        $activeCountResult = $conn->query($activeCountQuery);
        $activeRecords = $activeCountResult->fetch_assoc()['active_total'];
        debug_log("Active records in evacuation_centers: " . $activeRecords);

        // Fetch all active evacuation centers
        $query = "SELECT id, name, address, latitude, longitude, description, capacity, contact_number, status, created_at, updated_at
                  FROM evacuation_centers
                  WHERE status = 'active'
                  ORDER BY name ASC";

        debug_log("Executing query: " . $query);
        $result = $conn->query($query);

        if (!$result) {
            debug_log("Query failed: " . $conn->error);
            respond(['success' => false, 'error' => 'Database query failed: ' . $conn->error], 500);
        }

        debug_log("Query executed successfully, processing results");

        $locations = [];
        $rowCount = 0;
        while ($row = $result->fetch_assoc()) {
            $rowCount++;
            debug_log("Processing row " . $rowCount . ": " . $row['name']);

            // Format data for mobile consumption
            $locations[] = [
                'id' => (int)$row['id'],
                'name' => $row['name'],
                'address' => $row['address'],
                'latitude' => (float)$row['latitude'],
                'longitude' => (float)$row['longitude'],
                'description' => $row['description'] ?: '',
                'capacity' => $row['capacity'] ? (int)$row['capacity'] : null,
                'contact_number' => $row['contact_number'] ?: '',
                'status' => $row['status'],
                'created_at' => $row['created_at'],
                'updated_at' => $row['updated_at']
            ];
        }

        debug_log("Processed " . count($locations) . " locations");

        // If no active locations, provide helpful response
        if (empty($locations)) {
            debug_log("No active evacuation centers found");
            respond([
                'success' => true,
                'data' => [],
                'count' => 0,
                'message' => 'No active evacuation centers found',
                'debug_info' => [
                    'total_records' => $totalRecords,
                    'active_records' => $activeRecords
                ]
            ]);
        }

        debug_log("Returning successful response with " . count($locations) . " locations");

        respond([
            'success' => true,
            'data' => $locations,
            'count' => count($locations),
            'message' => 'Evacuation centers retrieved successfully'
        ]);

    } else {
        debug_log("Invalid method: " . $_SERVER['REQUEST_METHOD']);
        respond(['success' => false, 'error' => 'Method not allowed. Use GET request.'], 405);
    }

} catch (Exception $e) {
    debug_log("Exception caught: " . $e->getMessage());
    error_log('Mobile locations API error: ' . $e->getMessage());
    respond(['success' => false, 'error' => 'Internal server error: ' . $e->getMessage()], 500);
}
?>
