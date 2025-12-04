// Consumption Visualization Logic

let consumptionChart = null;
let currentSocket = null;
let currentDeviceId = null;

// Modal elements
const modal = document.getElementById("consumptionModal");
const span = document.getElementsByClassName("close")[0];
const dateInput = document.getElementById("consumptionDate");
const currentConsumptionValue = document.getElementById("currentConsumptionValue");

// Close modal
span.onclick = function () {
    closeModal();
}

window.onclick = function (event) {
    if (event.target == modal) {
        closeModal();
    }
}

function closeModal() {
    modal.style.display = "none";
    if (currentSocket) {
        currentSocket.close();
        currentSocket = null;
    }
    currentDeviceId = null;
}

function openConsumptionModal(deviceId) {
    currentDeviceId = deviceId;
    modal.style.display = "block";

    // Set today's date
    const today = new Date().toISOString().split('T')[0];
    dateInput.value = today;

    // Initialize Chart
    initChart();

    // Fetch initial data
    fetchHistoricalData(deviceId, today);

    // Connect WebSocket
    connectWebSocket(deviceId);
}

function initChart() {
    const ctx = document.getElementById('consumptionChart').getContext('2d');

    if (consumptionChart) {
        consumptionChart.destroy();
    }

    consumptionChart = new Chart(ctx, {
        type: 'bar', // Can be 'line' or 'bar'
        data: {
            labels: Array.from({ length: 24 }, (_, i) => i), // 0 to 23
            datasets: [{
                label: 'Hourly Consumption (kWh)',
                data: new Array(24).fill(0),
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

async function fetchHistoricalData(deviceId, date) {
    try {
        // Assuming Traefik routes /api/monitoring to monitoring-service
        const response = await fetch(`/api/monitoring/consumption/${deviceId}/${date}`);
        if (!response.ok) {
            throw new Error('Failed to fetch data');
        }
        const data = await response.json();

        // Update Chart
        const hourlyData = new Array(24).fill(0);
        data.forEach(item => {
            // item.hour is timestamp in ms
            const hour = new Date(item.hour).getHours();
            hourlyData[hour] = item.total_consumption;
        });

        consumptionChart.data.datasets[0].data = hourlyData;
        consumptionChart.update();

    } catch (error) {
        console.error("Error fetching historical data:", error);
    }
}

function connectWebSocket(deviceId) {
    if (currentSocket) {
        currentSocket.close();
    }

    // WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host; // localhost or whatever
    const wsUrl = `${protocol}//${host}/api/monitoring/ws/${deviceId}`;

    currentSocket = new WebSocket(wsUrl);

    currentSocket.onopen = function (event) {
        console.log("WebSocket Connected");
    };

    currentSocket.onmessage = function (event) {
        try {
            const data = JSON.parse(event.data);
            if (data.measurement_value !== undefined) {
                currentConsumptionValue.textContent = data.measurement_value;

                // Optionally update the chart in real-time if the date matches today
                // But for now, just showing current consumption is enough
            }
        } catch (e) {
            console.error("Error parsing WebSocket message:", e);
        }
    };

    currentSocket.onclose = function (event) {
        console.log("WebSocket Closed");
    };

    currentSocket.onerror = function (error) {
        console.error("WebSocket Error:", error);
    };
}

// Date change listener
dateInput.addEventListener('change', (e) => {
    if (currentDeviceId) {
        fetchHistoricalData(currentDeviceId, e.target.value);
    }
});
