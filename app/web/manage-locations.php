<?php
// manage-locations.php
// Endpoints for managing evacuation centers

// Handle API requests first
if (isset($_GET['action'])) {
    require_once 'config.php';
    header('Content-Type: application/json');

    $action = $_GET['action'] ?? '';
    $id = isset($_GET['id']) ? intval($_GET['id']) : null;

    function respond($data, $code = 200) {
        http_response_code($code);
        echo json_encode($data);
        exit;
    }

    switch ($action) {
        case 'list':
            $result = $conn->query("SELECT * FROM evacuation_centers ORDER BY created_at DESC");
            $centers = [];
            while ($row = $result->fetch_assoc()) $centers[] = $row;
            respond($centers);
            break;

        case 'create':
            $data = json_decode(file_get_contents('php://input'), true);
            $stmt = $conn->prepare("INSERT INTO evacuation_centers (name, address, latitude, longitude, description, capacity, contact_number, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->bind_param("ssddssis", $data['name'], $data['address'], $data['latitude'], $data['longitude'], $data['description'], $data['capacity'], $data['contact_number'], $data['status']);
            $stmt->execute();
            respond(['success' => $stmt->affected_rows > 0, 'id' => $conn->insert_id]);
            break;

        case 'update':
            if (!$id) respond(['error' => 'Missing id'], 400);
            $data = json_decode(file_get_contents('php://input'), true);
            $stmt = $conn->prepare("UPDATE evacuation_centers SET name=?, address=?, latitude=?, longitude=?, description=?, capacity=?, contact_number=?, status=? WHERE id=?");
            $stmt->bind_param("ssddssisi", $data['name'], $data['address'], $data['latitude'], $data['longitude'], $data['description'], $data['capacity'], $data['contact_number'], $data['status'], $id);
            $stmt->execute();
            respond(['success' => $stmt->affected_rows > 0]);
            break;

        case 'delete':
            if (!$id) respond(['error' => 'Missing id'], 400);
            $stmt = $conn->prepare("DELETE FROM evacuation_centers WHERE id=?");
            $stmt->bind_param("i", $id);
            $stmt->execute();
            respond(['success' => $stmt->affected_rows > 0]);
            break;

        default:
            respond(['error' => 'Invalid action'], 400);
    }
}

// Web interface - only show if no API action is requested
require_once 'includes/header.php';
require_once 'config.php';
?>

<div class="main-content">
    <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Manage Evacuation Centers</h2>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#locationModal" onclick="openAddModal()">
                <i class='bx bx-plus'></i> Add New Location
            </button>
        </div>

        <!-- Locations Table -->
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">Evacuation Centers</h5>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped" id="locationsTable">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Address</th>
                                <th>Coordinates</th>
                                <th>Capacity</th>
                                <th>Contact</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <!-- Data will be loaded via JavaScript -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Modal for Add/Edit Location -->
<div class="modal fade" id="locationModal" tabindex="-1">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="modalTitle">Add Evacuation Center</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <form id="locationForm">
                    <input type="hidden" id="locationId">
                    <div class="row">
                        <div class="col-md-6">
                            <!-- Form Fields -->
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="name" class="form-label">Name *</label>
                                    <input type="text" class="form-control" id="name" required>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label for="status" class="form-label">Status *</label>
                                    <select class="form-select" id="status" required>
                                        <option value="active">Active</option>
                                        <option value="inactive">Inactive</option>
                                    </select>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label for="address" class="form-label">Address *</label>
                                <textarea class="form-control" id="address" rows="2" required placeholder="Click on the map to auto-fill coordinates and search for address"></textarea>
                                <div class="form-text">
                                    <i class="bx bx-info-circle"></i> Tip: Click on the map to automatically set coordinates
                                </div>
                            </div>
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="latitude" class="form-label">Latitude *</label>
                                    <input type="number" class="form-control" id="latitude" step="any" required readonly>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label for="longitude" class="form-label">Longitude *</label>
                                    <input type="number" class="form-control" id="longitude" step="any" required readonly>
                                </div>
                            </div>
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label for="capacity" class="form-label">Capacity</label>
                                    <input type="number" class="form-control" id="capacity" placeholder="Number of people">
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label for="contact_number" class="form-label">Contact Number</label>
                                    <input type="text" class="form-control" id="contact_number" placeholder="+63 XXX XXX XXXX">
                                </div>
                            </div>
                            <div class="mb-3">
                                <label for="description" class="form-label">Description</label>
                                <textarea class="form-control" id="description" rows="3" placeholder="Describe the facilities, amenities, or special instructions..."></textarea>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <!-- Map Container -->
                            <div class="mb-3">
                                <label class="form-label">
                                    <i class="bx bx-map"></i> Select Location on Map *
                                </label>
                                <div id="mapContainer" style="height: 400px; border: 2px solid #dee2e6; border-radius: 8px; position: relative;">
                                    <div id="map" style="height: 100%; width: 100%; border-radius: 6px;"></div>
                                    <div id="mapLoading" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); display: none;">
                                        <div class="spinner-border text-primary" role="status">
                                            <span class="visually-hidden">Loading map...</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="form-text mt-2">
                                    <i class="bx bx-mouse"></i> Click anywhere on the map to set the evacuation center location
                                </div>
                            </div>
                            <!-- Map Controls -->
                            <div class="d-flex gap-2 mb-3">
                                <button type="button" class="btn btn-outline-secondary btn-sm" onclick="getCurrentLocation()">
                                    <i class="bx bx-current-location"></i> Use My Location
                                </button>
                                <button type="button" class="btn btn-outline-info btn-sm" onclick="searchLocation()">
                                    <i class="bx bx-search"></i> Search Address
                                </button>
                                <button type="button" class="btn btn-outline-warning btn-sm" onclick="resetMap()">
                                    <i class="bx bx-refresh"></i> Reset Map
                                </button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" onclick="saveLocation()">
                    <i class="bx bx-save"></i> Save Location
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Search Address Modal -->
<div class="modal fade" id="searchModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Search Address</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3">
                    <label for="searchInput" class="form-label">Enter address to search:</label>
                    <input type="text" class="form-control" id="searchInput" placeholder="e.g., Manila City Hall, Philippines">
                </div>
                <div id="searchResults" class="list-group" style="max-height: 300px; overflow-y: auto;"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" onclick="performSearch()">
                    <i class="bx bx-search"></i> Search
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Include Leaflet CSS and JS for OpenStreetMap -->
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
let isEditMode = false;
let map;
let marker;
let mapInitialized = false;

// Default center (Manila, Philippines)
const defaultLat = 14.5995;
const defaultLng = 120.9842;

// Load locations on page load
document.addEventListener('DOMContentLoaded', function() {
    loadLocations();
});

function loadLocations() {
    fetch('manage-locations.php?action=list')
        .then(response => response.json())
        .then(data => {
            const tbody = document.querySelector('#locationsTable tbody');
            tbody.innerHTML = '';

            data.forEach(location => {
                const row = `
                    <tr>
                        <td>${location.id}</td>
                        <td>${location.name}</td>
                        <td>${location.address}</td>
                        <td>
                            <small class="text-muted">
                                <i class="bx bx-map-pin"></i> ${location.latitude}, ${location.longitude}
                            </small>
                        </td>
                        <td>${location.capacity || 'N/A'}</td>
                        <td>${location.contact_number || 'N/A'}</td>
                        <td>
                            <span class="badge ${location.status === 'active' ? 'bg-success' : 'bg-secondary'}">
                                ${location.status}
                            </span>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary" onclick="editLocation(${location.id})" title="Edit">
                                <i class='bx bx-edit'></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger ms-1" onclick="deleteLocation(${location.id})" title="Delete">
                                <i class='bx bx-trash'></i>
                            </button>
                        </td>
                    </tr>
                `;
                tbody.innerHTML += row;
            });
        })
        .catch(error => {
            console.error('Error loading locations:', error);
        });
}

function initializeMap() {
    if (mapInitialized) return;

    try {
        // Show loading
        document.getElementById('mapLoading').style.display = 'block';

        // Initialize map
        map = L.map('map').setView([defaultLat, defaultLng], 13);

        // Add OpenStreetMap tiles
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(map);

        // Add click event to map
        map.on('click', function(e) {
            setMapLocation(e.latlng.lat, e.latlng.lng);
            reverseGeocode(e.latlng.lat, e.latlng.lng);
        });

        mapInitialized = true;

        // Hide loading
        setTimeout(() => {
            document.getElementById('mapLoading').style.display = 'none';
            map.invalidateSize(); // Ensure map renders properly
        }, 500);

    } catch (error) {
        console.error('Error initializing map:', error);
        document.getElementById('mapLoading').style.display = 'none';
    }
}

function setMapLocation(lat, lng) {
    // Remove existing marker
    if (marker) {
        map.removeLayer(marker);
    }

    // Add new marker
    marker = L.marker([lat, lng], {
        draggable: true
    }).addTo(map);

    // Update form fields
    document.getElementById('latitude').value = lat.toFixed(6);
    document.getElementById('longitude').value = lng.toFixed(6);

    // Add drag event to marker
    marker.on('drag', function(e) {
        const position = e.target.getLatLng();
        document.getElementById('latitude').value = position.lat.toFixed(6);
        document.getElementById('longitude').value = position.lng.toFixed(6);
    });

    marker.on('dragend', function(e) {
        const position = e.target.getLatLng();
        reverseGeocode(position.lat, position.lng);
    });

    // Center map on marker
    map.setView([lat, lng], 15);
}

function reverseGeocode(lat, lng) {
    // Use Nominatim for reverse geocoding
    fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1`)
        .then(response => response.json())
        .then(data => {
            if (data && data.display_name) {
                document.getElementById('address').value = data.display_name;
            }
        })
        .catch(error => {
            console.error('Error in reverse geocoding:', error);
        });
}

function getCurrentLocation() {
    if (navigator.geolocation) {
        document.getElementById('mapLoading').style.display = 'block';
        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                setMapLocation(lat, lng);
                reverseGeocode(lat, lng);
                document.getElementById('mapLoading').style.display = 'none';
            },
            function(error) {
                console.error('Geolocation error:', error);
                alert('Unable to get your current location. Please select manually on the map.');
                document.getElementById('mapLoading').style.display = 'none';
            }
        );
    } else {
        alert('Geolocation is not supported by this browser.');
    }
}

function searchLocation() {
    const searchModal = new bootstrap.Modal(document.getElementById('searchModal'));
    searchModal.show();
}

function performSearch() {
    const query = document.getElementById('searchInput').value.trim();
    if (!query) return;

    const resultsContainer = document.getElementById('searchResults');
    resultsContainer.innerHTML = '<div class="text-center p-3"><div class="spinner-border spinner-border-sm" role="status"></div> Searching...</div>';

    fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=5&addressdetails=1`)
        .then(response => response.json())
        .then(data => {
            resultsContainer.innerHTML = '';

            if (data.length === 0) {
                resultsContainer.innerHTML = '<div class="text-center p-3 text-muted">No results found</div>';
                return;
            }

            data.forEach(result => {
                const item = document.createElement('a');
                item.className = 'list-group-item list-group-item-action';
                item.href = '#';
                item.innerHTML = `
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">${result.display_name}</h6>
                        <small><i class="bx bx-map-pin"></i> ${parseFloat(result.lat).toFixed(4)}, ${parseFloat(result.lon).toFixed(4)}</small>
                    </div>
                `;
                item.onclick = function(e) {
                    e.preventDefault();
                    setMapLocation(parseFloat(result.lat), parseFloat(result.lon));
                    document.getElementById('address').value = result.display_name;
                    bootstrap.Modal.getInstance(document.getElementById('searchModal')).hide();
                };
                resultsContainer.appendChild(item);
            });
        })
        .catch(error => {
            console.error('Search error:', error);
            resultsContainer.innerHTML = '<div class="text-center p-3 text-danger">Search failed. Please try again.</div>';
        });
}

function resetMap() {
    if (marker) {
        map.removeLayer(marker);
        marker = null;
    }
    map.setView([defaultLat, defaultLng], 13);
    document.getElementById('latitude').value = '';
    document.getElementById('longitude').value = '';
}

function openAddModal() {
    isEditMode = false;
    document.getElementById('modalTitle').textContent = 'Add Evacuation Center';
    document.getElementById('locationForm').reset();
    document.getElementById('locationId').value = '';

    // Initialize map when modal is shown
    setTimeout(() => {
        initializeMap();
        resetMap();
    }, 300);
}

function editLocation(id) {
    isEditMode = true;
    document.getElementById('modalTitle').textContent = 'Edit Evacuation Center';

    // Find the location data from the table or fetch it
    fetch('manage-locations.php?action=list')
        .then(response => response.json())
        .then(data => {
            const location = data.find(loc => loc.id == id);
            if (location) {
                document.getElementById('locationId').value = location.id;
                document.getElementById('name').value = location.name;
                document.getElementById('address').value = location.address;
                document.getElementById('latitude').value = location.latitude;
                document.getElementById('longitude').value = location.longitude;
                document.getElementById('description').value = location.description || '';
                document.getElementById('capacity').value = location.capacity || '';
                document.getElementById('contact_number').value = location.contact_number || '';
                document.getElementById('status').value = location.status;

                // Show modal and initialize map
                const modal = new bootstrap.Modal(document.getElementById('locationModal'));
                modal.show();

                // Initialize map and set location after modal is shown
                setTimeout(() => {
                    initializeMap();
                    if (location.latitude && location.longitude) {
                        setMapLocation(parseFloat(location.latitude), parseFloat(location.longitude));
                    }
                }, 300);
            }
        });
}

function saveLocation() {
    const formData = {
        name: document.getElementById('name').value,
        address: document.getElementById('address').value,
        latitude: parseFloat(document.getElementById('latitude').value),
        longitude: parseFloat(document.getElementById('longitude').value),
        description: document.getElementById('description').value,
        capacity: parseInt(document.getElementById('capacity').value) || null,
        contact_number: document.getElementById('contact_number').value,
        status: document.getElementById('status').value
    };

    // Validation
    if (!formData.name || !formData.address || !formData.latitude || !formData.longitude) {
        alert('Please fill in all required fields and select a location on the map.');
        return;
    }

    const url = isEditMode
        ? `manage-locations.php?action=update&id=${document.getElementById('locationId').value}`
        : 'manage-locations.php?action=create';

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Close modal
            bootstrap.Modal.getInstance(document.getElementById('locationModal')).hide();
            // Reload table
            loadLocations();
            // Show success message
            alert(isEditMode ? 'Location updated successfully!' : 'Location added successfully!');
        } else {
            alert('Error saving location: ' + (data.error || 'Unknown error'));
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error saving location');
    });
}

function deleteLocation(id) {
    if (confirm('Are you sure you want to delete this evacuation center?')) {
        fetch(`manage-locations.php?action=delete&id=${id}`, {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                loadLocations();
                alert('Location deleted successfully!');
            } else {
                alert('Error deleting location');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting location');
        });
    }
}

// Add search functionality on Enter key
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('searchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            performSearch();
        }
    });
});
</script>

</body>
</html>
