const API_BASE_URL = '/api';

const api = {
    // Auth
    login: async (username, password) => {
        const response = await fetch(`${API_BASE_URL}/auth/token`, {
            method: 'POST',
            headers: {
                'Authorization': 'Basic ' + btoa(username + ":" + password)
            }
        });
        if (!response.ok) throw new Error('Login failed');
        return await response.text(); // Returns token string
    },

    register: async (userData) => {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        if (!response.ok) throw new Error('Registration failed');
        // Check if response has content before parsing
        const text = await response.text();
        return text ? JSON.parse(text) : {};
    },

    // Users
    getUsers: async () => {
        return request(`${API_BASE_URL}/users`);
    },

    createUser: async (user) => {
        return request(`${API_BASE_URL}/users`, {
            method: 'POST',
            body: JSON.stringify(user)
        });
    },

    updateUser: async (id, user) => {
        return request(`${API_BASE_URL}/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(user)
        });
    },

    deleteUser: async (id) => {
        return request(`${API_BASE_URL}/auth/delete/${id}`, {
            method: 'DELETE'
        });
    },

    // Devices
    getDevices: async () => {
        return request(`${API_BASE_URL}/devices`);
    },

    createDevice: async (device) => {
        return request(`${API_BASE_URL}/devices`, {
            method: 'POST',
            body: JSON.stringify(device)
        });
    },

    updateDevice: async (id, device) => {
        return request(`${API_BASE_URL}/devices/${id}`, {
            method: 'PUT',
            body: JSON.stringify(device)
        });
    },

    deleteDevice: async (id) => {
        return request(`${API_BASE_URL}/devices/${id}`, {
            method: 'DELETE'
        });
    },

    // Mappings
    assignDevice: async (userId, deviceId) => {
        return request(`${API_BASE_URL}/devices/mapping?userId=${userId}&deviceId=${deviceId}`, {
            method: 'POST'
        });
    },

    unassignDevice: async (userId, deviceId) => {
        return request(`${API_BASE_URL}/devices/mapping?userId=${userId}&deviceId=${deviceId}`, {
            method: 'DELETE'
        });
    },

    getUserDevices: async (userId) => {
        return request(`${API_BASE_URL}/devices/user/${userId}`);
    },

    getDeviceUser: async (deviceId) => {
        const response = await fetch(`${API_BASE_URL}/devices/user-mapping/${deviceId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) return null;
        return await response.text();
    },

    request: request
};

function getAuthHeaders() {
    const token = localStorage.getItem('jwt_token');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

async function request(url, options = {}) {
    const headers = getAuthHeaders();
    const config = {
        ...options,
        headers: {
            ...headers,
            ...options.headers
        }
    };

    const response = await fetch(url, config);

    if (response.status === 401) {
        auth.logout();
        return;
    }

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Request failed: ${response.status}`);
    }

    // Return empty for 204 No Content
    if (response.status === 204) return null;

    const text = await response.text();
    return text ? JSON.parse(text) : null;
}
