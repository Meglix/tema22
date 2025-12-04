const auth = {
    login: async (username, password) => {
        try {
            const token = await api.login(username, password);
            localStorage.setItem('jwt_token', token);

            const role = auth.getUserRole();
            if (role === 'ROLE_ADMIN') {
                window.location.href = 'admin-dashboard.html';
            } else {
                window.location.href = 'client-dashboard.html';
            }
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    },

    register: async (userData) => {
        try {
            await api.register(userData);
            window.location.href = 'login.html?registered=true';
        } catch (error) {
            console.error('Registration error:', error);
            throw error;
        }
    },

    logout: () => {
        localStorage.removeItem('jwt_token');
        window.location.href = 'login.html';
    },

    isAuthenticated: () => {
        const token = localStorage.getItem('jwt_token');
        if (!token) return false;

        // Simple check if token is expired
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const exp = payload.exp * 1000;
            return Date.now() < exp;
        } catch (e) {
            return false;
        }
    },

    getUserRole: () => {
        const token = localStorage.getItem('jwt_token');
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            // Assuming the scope claim contains the role, e.g., "ROLE_ADMIN"
            return payload.scope || 'ROLE_CLIENT';
        } catch (e) {
            return null;
        }
    },

    getUserId: () => {
        const token = localStorage.getItem('jwt_token');
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.userId;
        } catch (e) {
            return null;
        }
    },

    getUsername: () => {
        const token = localStorage.getItem('jwt_token');
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.sub;
        } catch (e) {
            return null;
        }
    },

    checkAuth: (requiredRole = null) => {
        if (!auth.isAuthenticated()) {
            window.location.href = 'login.html';
            return;
        }

        if (requiredRole) {
            const role = auth.getUserRole();
            if (role !== requiredRole) {
                // Redirect to appropriate dashboard if role mismatch
                if (role === 'ROLE_ADMIN') {
                    window.location.href = 'admin-dashboard.html';
                } else {
                    window.location.href = 'client-dashboard.html';
                }
            }
        }
    }
};
