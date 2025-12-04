// Check auth
auth.checkAuth();

// Load Data
async function loadMyData() {
    const userId = auth.getUserId();
    const username = auth.getUsername();

    if (username) {
        document.getElementById('welcomeMsg').textContent = `Welcome, ${username}`;
    }

    if (!userId) {
        console.error('User ID not found in token');
        return;
    }

    try {
        const devices = await api.getUserDevices(userId);
        const tbody = document.querySelector('#myDevicesTable tbody');
        tbody.innerHTML = '';

        if (devices && devices.length > 0) {
            devices.forEach(device => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${device.id}</td>
                    <td>${device.name}</td>
                    <td>${device.manufacturer || '-'}</td>
                    <td>${device.consumption}</td>
                    <td>
                        <button onclick="openConsumptionModal('${device.id}')" class="btn btn-info">View Consumption</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center">No devices assigned.</td></tr>';
        }
    } catch (error) {
        console.error('Error loading devices:', error);
    }
}

loadMyData();
