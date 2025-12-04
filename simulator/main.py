import os
import sys

# Fix for Tkinter in virtual environments on Windows
if sys.platform == "win32":
    # Get the base Python installation directory
    python_dir = sys.base_prefix
    tcl_dir = os.path.join(python_dir, 'tcl')

    # Set TCL/TK environment variables
    os.environ['TCL_LIBRARY'] = os.path.join(tcl_dir, 'tcl8.6')
    os.environ['TK_LIBRARY'] = os.path.join(tcl_dir, 'tk8.6')

import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
import json
import threading
import time
from datetime import datetime, timedelta
import random
import pika
import uuid
from typing import Optional
from pika import PlainCredentials

CREDS = PlainCredentials('kalo', 'kalo')

class SmartMeterSimulator:
    """Simulates smart meter readings with realistic energy consumption patterns."""

    def __init__(self, device_id: str, base_load: float = 2.0):
        self.device_id = device_id
        self.base_load = base_load  # kWh baseline consumption
        self.current_timestamp = datetime.now()

    def get_hour_multiplier(self, hour: int) -> float:
        """Returns a multiplier based on time of day to simulate realistic patterns."""
        if 0 <= hour < 6:  # Night time - lower consumption
            return random.uniform(0.3, 0.5)
        elif 6 <= hour < 9:  # Morning - increasing consumption
            return random.uniform(0.7, 1.0)
        elif 9 <= hour < 17:  # Daytime - moderate consumption
            return random.uniform(0.6, 0.9)
        elif 17 <= hour < 22:  # Evening - peak consumption
            return random.uniform(1.2, 1.8)
        else:  # Late evening - decreasing consumption
            return random.uniform(0.8, 1.1)

    def generate_measurement(self) -> dict:
        """Generate a single smart meter measurement."""
        hour = self.current_timestamp.hour
        hour_multiplier = self.get_hour_multiplier(hour)

        # Add some random fluctuation
        fluctuation = random.uniform(-0.2, 0.2)

        # Calculate consumption for 10-minute interval
        measurement_value = round(
            self.base_load * hour_multiplier * (1 + fluctuation) * (10 / 60),  # Convert to 10-min interval
            3
        )

        # Ensure non-negative values
        measurement_value = max(0.001, measurement_value)

        measurement = {
            "device_id": self.device_id,
            "timestamp": int(self.current_timestamp.timestamp() * 1000), # Unix timestamp in milliseconds
            "measurement_value": measurement_value
        }

        # Increment timestamp by 10 minutes
        self.current_timestamp += timedelta(minutes=10)

        return measurement


class RabbitMQPublisher:
    """Handles connection and publishing to RabbitMQ."""

    def __init__(self, host: str = 'localhost', queue: str = 'measurements.queue'):
        self.host = host
        self.queue = queue
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel = None

    def connect(self) -> bool:
        """Establish connection to RabbitMQ."""
        try:
            self.connection = pika.BlockingConnection(
                pika.ConnectionParameters(host=self.host, credentials=CREDS)
            )
            self.channel = self.connection.channel()
            self.channel.queue_declare(queue=self.queue, durable=True)
            return True
        except Exception as e:
            raise ConnectionError(f"Failed to connect to RabbitMQ: {str(e)}")

    def publish(self, message: dict) -> bool:
        """Publish a message to RabbitMQ."""
        try:
            self.channel.basic_publish(
                exchange='',
                routing_key=self.queue,
                body=json.dumps(message),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                )
            )
            return True
        except Exception as e:
            raise RuntimeError(f"Failed to publish message: {str(e)}")

    def close(self):
        """Close the connection."""
        if self.connection and not self.connection.is_closed:
            self.connection.close()


class DeviceDataSimulatorApp:
    """Main application class for the Device Data Simulator GUI."""

    def __init__(self, root):
        self.root = root
        self.root.title("Smart Meter Data Simulator")
        self.root.geometry("700x650")
        self.root.resizable(False, False)

        # Simulation state
        self.is_running = False
        self.simulator: Optional[SmartMeterSimulator] = None
        self.publisher: Optional[RabbitMQPublisher] = None
        self.simulation_thread: Optional[threading.Thread] = None
        self.message_count = 0

        self.setup_ui()

    def setup_ui(self):
        """Setup the user interface."""
        # Main container
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))

        # Title
        title_label = ttk.Label(
            main_frame,
            text="Device Data Simulator (Producer)",
            font=("Arial", 16, "bold")
        )
        title_label.grid(row=0, column=0, columnspan=2, pady=(0, 20))

        # Configuration Frame
        config_frame = ttk.LabelFrame(main_frame, text="Configuration", padding="10")
        config_frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))

        # Device ID
        ttk.Label(config_frame, text="Device ID:").grid(row=0, column=0, sticky=tk.W, pady=5)
        self.device_id_entry = ttk.Entry(config_frame, width=36) # Increased width for UUID
        self.device_id_entry.grid(row=0, column=1, sticky=(tk.W, tk.E), pady=5, padx=(10, 0))
        self.device_id_entry.insert(0, str(uuid.uuid4()))

        # Period value
        ttk.Label(config_frame, text="Period Value:").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.period_value_entry = ttk.Entry(config_frame, width=30)
        self.period_value_entry.grid(row=1, column=1, sticky=(tk.W, tk.E), pady=5, padx=(10, 0))
        self.period_value_entry.insert(0, "10")

        # Period unit
        ttk.Label(config_frame, text="Period Unit:").grid(row=2, column=0, sticky=tk.W, pady=5)
        self.period_unit_var = tk.StringVar(value="seconds")
        period_unit_frame = ttk.Frame(config_frame)
        period_unit_frame.grid(row=2, column=1, sticky=tk.W, pady=5, padx=(10, 0))

        ttk.Radiobutton(
            period_unit_frame,
            text="Seconds",
            variable=self.period_unit_var,
            value="seconds"
        ).pack(side=tk.LEFT, padx=(0, 10))

        ttk.Radiobutton(
            period_unit_frame,
            text="Minutes",
            variable=self.period_unit_var,
            value="minutes"
        ).pack(side=tk.LEFT)

        config_frame.columnconfigure(1, weight=1)

        # Control Frame
        control_frame = ttk.Frame(main_frame)
        control_frame.grid(row=2, column=0, columnspan=2, pady=(0, 10))

        self.start_button = ttk.Button(
            control_frame,
            text="Start Simulation",
            command=self.start_simulation,
            width=20
        )
        self.start_button.pack(side=tk.LEFT, padx=5)

        self.stop_button = ttk.Button(
            control_frame,
            text="Stop Simulation",
            command=self.stop_simulation,
            width=20,
            state=tk.DISABLED
        )
        self.stop_button.pack(side=tk.LEFT, padx=5)

        # Status Frame
        status_frame = ttk.LabelFrame(main_frame, text="Status", padding="10")
        status_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))

        self.status_label = ttk.Label(
            status_frame,
            text="Status: Idle",
            font=("Arial", 10)
        )
        self.status_label.pack(anchor=tk.W)

        self.message_count_label = ttk.Label(
            status_frame,
            text="Messages Sent: 0",
            font=("Arial", 10)
        )
        self.message_count_label.pack(anchor=tk.W)

        # Log Frame
        log_frame = ttk.LabelFrame(main_frame, text="Activity Log", padding="10")
        log_frame.grid(row=4, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))

        self.log_text = scrolledtext.ScrolledText(
            log_frame,
            height=15,
            width=80,
            wrap=tk.WORD,
            state=tk.DISABLED
        )
        self.log_text.pack(fill=tk.BOTH, expand=True)

        # Configure grid weights
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(4, weight=1)

    def log_message(self, message: str):
        """Add a message to the log."""
        self.log_text.configure(state=tk.NORMAL)
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.log_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_text.see(tk.END)
        self.log_text.configure(state=tk.DISABLED)

    def update_status(self, status: str):
        """Update the status label."""
        self.status_label.config(text=f"Status: {status}")

    def update_message_count(self):
        """Update the message count label."""
        self.message_count_label.config(text=f"Messages Sent: {self.message_count}")

    def validate_inputs(self) -> bool:
        """Validate user inputs."""
        device_id = self.device_id_entry.get().strip()
        if not device_id:
            messagebox.showerror("Validation Error", "Please enter a Device ID.")
            return False
        
        try:
            uuid.UUID(device_id)
        except ValueError:
            messagebox.showerror("Validation Error", "Device ID must be a valid UUID.")
            return False

        try:
            period_value = float(self.period_value_entry.get().strip())
            if period_value <= 0:
                raise ValueError()
        except ValueError:
            messagebox.showerror("Validation Error", "Please enter a valid positive number for Period Value.")
            return False

        return True

    def start_simulation(self):
        """Start the simulation."""
        if not self.validate_inputs():
            return

        # Get configuration
        device_id = self.device_id_entry.get().strip()
        period_value = float(self.period_value_entry.get().strip())
        period_unit = self.period_unit_var.get()

        # Use default RabbitMQ configuration
        rabbitmq_host = "localhost"
        queue_name = "measurements.queue"

        # Convert period to seconds
        if period_unit == "minutes":
            period_seconds = period_value * 60
        else:
            period_seconds = period_value

        self.log_message(f"Starting simulation for Device ID: {device_id}")
        self.log_message(f"Period: {period_value} {period_unit} ({period_seconds} seconds)")
        self.log_message(f"RabbitMQ Host: {rabbitmq_host}, Queue: {queue_name}")

        # Initialize simulator and publisher
        try:
            self.simulator = SmartMeterSimulator(device_id)
            self.publisher = RabbitMQPublisher(host=rabbitmq_host, queue=queue_name)

            # Connect to RabbitMQ
            self.log_message("Connecting to RabbitMQ...")
            self.publisher.connect()
            self.log_message("Connected to RabbitMQ successfully!")

        except Exception as e:
            messagebox.showerror("Connection Error", str(e))
            self.log_message(f"ERROR: {str(e)}")
            return

        # Start simulation
        self.is_running = True
        self.message_count = 0
        self.update_message_count()
        self.update_status("Running")

        # Disable/enable buttons
        self.start_button.config(state=tk.DISABLED)
        self.stop_button.config(state=tk.NORMAL)
        self.device_id_entry.config(state=tk.DISABLED)
        self.period_value_entry.config(state=tk.DISABLED)

        # Start simulation thread
        self.simulation_thread = threading.Thread(
            target=self.run_simulation,
            args=(period_seconds,),
            daemon=True
        )
        self.simulation_thread.start()

    def run_simulation(self, period_seconds: float):
        """Run the simulation loop."""
        while self.is_running:
            try:
                # Generate measurement
                measurement = self.simulator.generate_measurement()

                # Publish to RabbitMQ
                self.publisher.publish(measurement)

                # Update UI
                self.message_count += 1
                self.root.after(0, self.update_message_count)

                # Log the message
                log_msg = f"Sent: {json.dumps(measurement)}"
                self.root.after(0, self.log_message, log_msg)

                # Wait for the specified period
                time.sleep(period_seconds)

            except Exception as e:
                error_msg = f"ERROR: {str(e)}"
                self.root.after(0, self.log_message, error_msg)
                self.root.after(0, self.stop_simulation)
                break

    def stop_simulation(self):
        """Stop the simulation."""
        if not self.is_running:
            return

        self.is_running = False
        self.log_message("Stopping simulation...")

        # Wait for thread to finish
        if self.simulation_thread and self.simulation_thread.is_alive():
            self.simulation_thread.join(timeout=2)

        # Close RabbitMQ connection
        if self.publisher:
            self.publisher.close()
            self.log_message("RabbitMQ connection closed.")

        self.update_status("Stopped")
        self.log_message(f"Simulation stopped. Total messages sent: {self.message_count}")

        # Enable/disable buttons
        self.start_button.config(state=tk.NORMAL)
        self.stop_button.config(state=tk.DISABLED)
        self.device_id_entry.config(state=tk.NORMAL)
        self.period_value_entry.config(state=tk.NORMAL)

    def on_closing(self):
        """Handle window closing event."""
        if self.is_running:
            if messagebox.askokcancel("Quit", "Simulation is running. Do you want to stop and quit?"):
                self.stop_simulation()
                self.root.destroy()
        else:
            self.root.destroy()


def main():
    """Main entry point."""
    root = tk.Tk()
    app = DeviceDataSimulatorApp(root)
    root.protocol("WM_DELETE_WINDOW", app.on_closing)
    root.mainloop()


if __name__ == "__main__":
    main()