import os
import json
import pika
import time
import threading
import asyncio
from typing import List
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from database_module import create_table_if_not_exists, insert_measurement, get_hourly_consumption, insert_device, delete_device

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
QUEUE_NAME = "measurements.queue"
DEVICE_QUEUE_NAME = "device.create.queue"
DEVICE_DELETE_QUEUE_NAME = "device.delete.queue"

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# WebSocket Connection Manager
class ConnectionManager:
    def __init__(self):
        # Map device_id to list of websockets
        self.active_connections: dict[str, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, device_id: str):
        await websocket.accept()
        if device_id not in self.active_connections:
            self.active_connections[device_id] = []
        self.active_connections[device_id].append(websocket)

    def disconnect(self, websocket: WebSocket, device_id: str):
        if device_id in self.active_connections:
            if websocket in self.active_connections[device_id]:
                self.active_connections[device_id].remove(websocket)
            if not self.active_connections[device_id]:
                del self.active_connections[device_id]

    async def broadcast(self, message: str, device_id: str):
        if device_id in self.active_connections:
            for connection in self.active_connections[device_id]:
                try:
                    await connection.send_text(message)
                except Exception:
                    pass

manager = ConnectionManager()

# RabbitMQ Consumer
def rabbitmq_consumer():
    print("Starting RabbitMQ Consumer...")
    connection = None
    while connection is None:
        try:
            # Using PlainCredentials as seen in simulator
            creds = pika.PlainCredentials('kalo', 'kalo')
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(host=RABBITMQ_HOST, credentials=creds)
            )
        except pika.exceptions.AMQPConnectionError:
            print("RabbitMQ not ready, retrying...")
            time.sleep(5)

    channel = connection.channel()
    channel.queue_declare(queue=QUEUE_NAME, durable=True)

    def callback(ch, method, properties, body):
        print(f" [x] Received {body}")
        try:
            data = json.loads(body)
            timestamp = data.get("timestamp")
            device_id = data.get("device_id")
            measurement_value = data.get("measurement_value")

            if timestamp and device_id and measurement_value is not None:
                insert_measurement(timestamp, device_id, measurement_value)
                print(f" [x] Saved measurement for device {device_id}")
                
                # Broadcast to WebSockets
                # Since this is running in a separate thread, we need to run the async broadcast
                # We can use a loop or run_coroutine_threadsafe if we had the loop
                # For simplicity, we can't easily await here without an event loop.
                # A common pattern is to use a shared queue or just fire and forget if possible,
                # but here we need to bridge sync to async.
                # Let's try to get the running loop if possible, or create a new one.
                # Actually, uvicorn runs the main loop. We can use asyncio.run_coroutine_threadsafe
                
                # We need a reference to the main loop. 
                # Ideally, we should run the consumer as an async task if possible, 
                # but pika blocking connection is blocking.
                # We can use a global variable for the loop.
                if loop:
                    asyncio.run_coroutine_threadsafe(
                        manager.broadcast(json.dumps(data), device_id), loop
                    )

            else:
                print(" [!] Invalid data format")
        except Exception as e:
            print(f" [!] Error processing message: {e}")

    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=callback, auto_ack=True)
    print(' [*] Waiting for messages.')
    channel.start_consuming()

# Device RabbitMQ Consumer
def device_rabbitmq_consumer():
    print("Starting Device RabbitMQ Consumer...")
    connection = None
    while connection is None:
        try:
            creds = pika.PlainCredentials('kalo', 'kalo')
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(host=RABBITMQ_HOST, credentials=creds)
            )
        except pika.exceptions.AMQPConnectionError:
            print("RabbitMQ not ready for device consumer, retrying...")
            time.sleep(5)

    channel = connection.channel()
    channel.queue_declare(queue=DEVICE_QUEUE_NAME, durable=True)

    def callback(ch, method, properties, body):
        print(f" [x] Received device event: {body}")
        try:
            # Parse device UUID from message
            device_id = body.decode('utf-8').strip('"')
            insert_device(device_id)
            print(f" [x] Synchronized device {device_id} in monitoring database")
        except Exception as e:
            print(f" [!] Error processing device event: {e}")

    channel.basic_consume(queue=DEVICE_QUEUE_NAME, on_message_callback=callback, auto_ack=True)
    print(' [*] Waiting for device events.')
    channel.start_consuming()

# Device Delete RabbitMQ Consumer
def device_delete_rabbitmq_consumer():
    print("Starting Device Delete RabbitMQ Consumer...")
    connection = None
    while connection is None:
        try:
            creds = pika.PlainCredentials('kalo', 'kalo')
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(host=RABBITMQ_HOST, credentials=creds)
            )
        except pika.exceptions.AMQPConnectionError:
            print("RabbitMQ not ready for device delete consumer, retrying...")
            time.sleep(5)

    channel = connection.channel()
    channel.queue_declare(queue=DEVICE_DELETE_QUEUE_NAME, durable=True)

    def callback(ch, method, properties, body):
        print(f" [x] Received device delete event: {body}")
        try:
            # Parse device UUID from message
            device_id = body.decode('utf-8').strip('"')
            delete_device(device_id)
            print(f" [x] Deleted device {device_id} from monitoring database")
        except Exception as e:
            print(f" [!] Error processing device delete event: {e}")

    channel.basic_consume(queue=DEVICE_DELETE_QUEUE_NAME, on_message_callback=callback, auto_ack=True)
    print(' [*] Waiting for device delete events.')
    channel.start_consuming()

# Global loop variable
loop = None

@app.on_event("startup")
async def startup_event():
    global loop
    loop = asyncio.get_running_loop()
    
    # Initialize DB
    create_table_if_not_exists()
    
    # Start RabbitMQ consumer in a separate thread
    t = threading.Thread(target=rabbitmq_consumer, daemon=True)
    t.start()
    
    # Start Device RabbitMQ consumer in a separate thread
    t2 = threading.Thread(target=device_rabbitmq_consumer, daemon=True)
    t2.start()
    
    # Start Device Delete RabbitMQ consumer in a separate thread
    t3 = threading.Thread(target=device_delete_rabbitmq_consumer, daemon=True)
    t3.start()

@app.get("/")
async def health_check():
    return {"status": "ok"}

@app.get("/consumption/{device_id}/{date}")
async def get_consumption(device_id: str, date: str):
    """
    Get hourly consumption for a device on a specific date.
    Date format: YYYY-MM-DD
    """
    return get_hourly_consumption(device_id, date)

@app.websocket("/ws/{device_id}")
async def websocket_endpoint(websocket: WebSocket, device_id: str):
    await manager.connect(websocket, device_id)
    try:
        while True:
            # Keep connection alive
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket, device_id)

if __name__ == '__main__':
    # For local testing
    uvicorn.run(app, host="0.0.0.0", port=8080)
