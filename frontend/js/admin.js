// Check auth
auth.checkAuth('ROLE_ADMIN');

// Tab Logic
function openTab(tabName) {
    const tabs = document.getElementsByClassName('tab-content');
    for (let tab of tabs) {
        tab.classList.remove('active');
    }
    const btns = document.getElementsByClassName('tab-btn');
    for (let btn of btns) {
        btn.classList.remove('active');
    }
    document.getElementById(tabName).classList.add('active');
    event.currentTarget.classList.add('active');

    if (tabName === 'users') loadUsers();
    if (tabName === 'devices') loadDevices();
    if (tabName === 'assignments') loadAssignmentsData();
}

// Users Logic
async function loadUsers() {
    try {
        const users = await api.getUsers();
        const tbody = document.querySelector('#usersTable tbody');
        tbody.innerHTML = '';
        users.forEach(user => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.age}</td>
                <td>
                    <button class="btn action-btn" onclick="editUser('${user.id}')">Edit</button>
                    <button class="btn btn-danger action-btn" onclick="deleteUser('${user.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

function openUserModal(user = null) {
    const modal = document.getElementById('userModal');
    const title = document.getElementById('userModalTitle');
    const form = document.getElementById('userForm');

    if (user) {
        title.textContent = 'Edit User';
        document.getElementById('userId').value = user.id;
        document.getElementById('userName').value = user.name;
        document.getElementById('userAddress').value = user.address || ''; // Address might not be in list DTO
        document.getElementById('userAge').value = user.age;
    } else {
        title.textContent = 'Add User';
        form.reset();
        document.getElementById('userId').value = '';
    }
    modal.classList.add('active');
}

function closeUserModal() {
    document.getElementById('userModal').classList.remove('active');
}

async function editUser(id) {
    console.log('editUser called with id:', id);
    try {
        const url = `${API_BASE_URL}/users/${id}`;
        console.log('Fetching user details from:', url);
        const user = await api.request(url);
        console.log('User details received:', user);
        openUserModal(user);
    } catch (error) {
        console.error('Error fetching user details:', error);
        alert('Failed to load user details: ' + error.message);
    }
}

// Make functions global
window.editUser = editUser;
window.deleteUser = deleteUser;

async function deleteUser(id) {
    if (confirm('Are you sure you want to delete this user?')) {
        try {
            await api.deleteUser(id);
            loadUsers();
        } catch (error) {
            console.error('Error deleting user:', error);
        }
    }
}

document.getElementById('userForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('userId').value;
    const userData = {
        name: document.getElementById('userName').value,
        address: document.getElementById('userAddress').value,
        age: parseInt(document.getElementById('userAge').value)
    };

    try {
        if (id) {
            userData.id = id;
            await api.updateUser(id, userData);
        } else {
            // Note: Creating user via this form might fail if backend expects full registration (username/pass)
            // For now assuming PersonDetailsDTO is enough for update, but create might need RegisterDTO
            // If create fails, we might need to redirect to register or add username/pass fields
            alert('To create a new user, please use the Registration page or implement full user creation here.');
            return;
        }
        closeUserModal();
        loadUsers();
    } catch (error) {
        console.error('Error saving user:', error);
        alert('Error saving user: ' + error.message);
    }
});

// Devices Logic
async function loadDevices() {
    try {
        const devices = await api.getDevices();
        const tbody = document.querySelector('#devicesTable tbody');
        tbody.innerHTML = '';
        devices.forEach(device => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${device.id}</td>
                <td>${device.name}</td>
                <td>${device.manufacturer || '-'}</td>
                <td>${device.consumption}</td>
                <td>
                    <button class="btn action-btn" onclick="editDevice('${device.id}')">Edit</button>
                    <button class="btn btn-danger action-btn" onclick="deleteDevice('${device.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        console.error('Error loading devices:', error);
    }
}

function openDeviceModal(device = null) {
    const modal = document.getElementById('deviceModal');
    const title = document.getElementById('deviceModalTitle');
    const form = document.getElementById('deviceForm');

    if (device) {
        title.textContent = 'Edit Device';
        document.getElementById('deviceId').value = device.id;
        document.getElementById('deviceName').value = device.name;
        document.getElementById('deviceManufacturer').value = device.manufacturer;
        document.getElementById('deviceConsumption').value = device.consumption;
    } else {
        title.textContent = 'Add Device';
        form.reset();
        document.getElementById('deviceId').value = '';
    }
    modal.classList.add('active');
}

function closeDeviceModal() {
    document.getElementById('deviceModal').classList.remove('active');
}

async function editDevice(id) {
    console.log('editDevice called with id:', id);
    try {
        const url = `${API_BASE_URL}/devices/${id}`;
        console.log('Fetching device details from:', url);
        const device = await api.request(url);
        console.log('Device details received:', device);
        openDeviceModal(device);
    } catch (error) {
        console.error('Error fetching device details:', error);
        alert('Failed to load device details: ' + error.message);
    }
}

// Make functions global
window.editDevice = editDevice;
window.deleteDevice = deleteDevice;

async function deleteDevice(id) {
    if (confirm('Are you sure you want to delete this device?')) {
        try {
            await api.deleteDevice(id);
            loadDevices();
        } catch (error) {
            console.error('Error deleting device:', error);
        }
    }
}

document.getElementById('deviceForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('deviceId').value;
    const deviceData = {
        name: document.getElementById('deviceName').value,
        manufacturer: document.getElementById('deviceManufacturer').value,
        consumption: parseInt(document.getElementById('deviceConsumption').value)
    };

    try {
        if (id) {
            deviceData.id = id;
            await api.updateDevice(id, deviceData);
        } else {
            await api.createDevice(deviceData);
        }
        closeDeviceModal();
        loadDevices();
    } catch (error) {
        console.error('Error saving device:', error);
        alert('Error saving device: ' + error.message);
    }
});

// Assignments Logic
// Assignments Logic
async function loadAssignmentsData() {
    try {
        const [users, devices] = await Promise.all([
            api.getUsers(),
            api.getDevices()
        ]);

        // Fetch assignments for all devices
        const assignments = await Promise.all(devices.map(async (device) => {
            try {
                const userId = await api.getDeviceUser(device.id);
                return { device, userId };
            } catch (e) {
                return { device, userId: null };
            }
        }));

        const assignedDevices = assignments.filter(a => a.userId !== null && a.userId !== "");
        const unassignedDevices = assignments.filter(a => a.userId === null || a.userId === "");

        // Populate Users Dropdown
        const userSelect = document.getElementById('assignUser');
        userSelect.innerHTML = '<option value="">Select User...</option>';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = `${user.name} (${user.id})`;
            userSelect.appendChild(option);
        });

        // Populate Devices Dropdown (Only Unassigned)
        const deviceSelect = document.getElementById('assignDevice');
        deviceSelect.innerHTML = '<option value="">Select Device...</option>';
        unassignedDevices.forEach(item => {
            const device = item.device;
            const option = document.createElement('option');
            option.value = device.id;
            option.textContent = `${device.name} (${device.id})`;
            deviceSelect.appendChild(option);
        });

        // Populate Table
        const tbody = document.querySelector('#assignmentsTable tbody');
        tbody.innerHTML = '';
        assignedDevices.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${item.device.name}</td>
                <td>${item.userId}</td>
                <td>
                    <button class="btn btn-info action-btn" onclick="openConsumptionModal('${item.device.id}')">View Consumption</button>
                    <button class="btn btn-danger action-btn" onclick="unassignDevice('${item.userId}', '${item.device.id}')">Unassign</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Error loading assignment data:', error);
    }
}

document.getElementById('assignmentForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const userId = document.getElementById('assignUser').value;
    const deviceId = document.getElementById('assignDevice').value;
    const alertBox = document.getElementById('assignmentAlert');

    try {
        await api.assignDevice(userId, deviceId);
        alertBox.className = 'alert alert-success';
        alertBox.textContent = 'Device assigned successfully';
        alertBox.style.display = 'block';
        loadAssignmentsData(); // Reload table
    } catch (error) {
        alertBox.className = 'alert alert-error';
        alertBox.textContent = 'Assignment failed: ' + error.message;
        alertBox.style.display = 'block';
    }
});

async function unassignDevice(userId, deviceId) {
    if (confirm('Are you sure you want to unassign this device?')) {
        try {
            await api.unassignDevice(userId, deviceId);
            loadAssignmentsData();
        } catch (error) {
            console.error('Error unassigning device:', error);
            alert('Error unassigning device');
        }
    }
}

window.unassignDevice = unassignDevice;

// Initial Load
loadUsers();
