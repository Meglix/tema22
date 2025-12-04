import os
import psycopg2
from psycopg2 import sql
from datetime import datetime

DB_HOST = os.getenv("DB_HOST", "monitoring_db")
DB_NAME = os.getenv("DB_NAME", "example-db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "postgres")

import time

def get_connection():
    while True:
        try:
            conn = psycopg2.connect(
                host=DB_HOST,
                database=DB_NAME,
                user=DB_USER,
                password=DB_PASS
            )
            return conn
        except psycopg2.OperationalError:
            print("Database not ready, retrying in 2 seconds...")
            time.sleep(2)

def create_table_if_not_exists():
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS measurements (
            id SERIAL PRIMARY KEY,
            timestamp BIGINT,
            device_id UUID,
            measurement_value DOUBLE PRECISION
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS hourly_consumption (
            id SERIAL PRIMARY KEY,
            device_id UUID,
            hour BIGINT,
            total_consumption DOUBLE PRECISION,
            UNIQUE(device_id, hour)
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

def insert_measurement(timestamp, device_id, measurement_value):
    conn = get_connection()
    cur = conn.cursor()
    
    # Insert raw measurement
    cur.execute("""
        INSERT INTO measurements (timestamp, device_id, measurement_value)
        VALUES (%s, %s, %s)
    """, (timestamp, device_id, measurement_value))
    
    # Update hourly consumption
    # Assuming timestamp is in milliseconds, convert to hour (remove minutes, seconds, millis)
    # 3600000 ms in an hour
    hour_timestamp = (timestamp // 3600000) * 3600000
    
    cur.execute("""
        INSERT INTO hourly_consumption (device_id, hour, total_consumption)
        VALUES (%s, %s, %s)
        ON CONFLICT (device_id, hour) 
        DO UPDATE SET total_consumption = hourly_consumption.total_consumption + EXCLUDED.total_consumption
    """, (device_id, hour_timestamp, measurement_value))
    
    conn.commit()
    cur.close()
    conn.close()

def get_hourly_consumption(device_id, date):
    """
    Fetch hourly consumption for a specific device and date.
    date should be a string in 'YYYY-MM-DD' format.
    Returns a list of dictionaries: [{'hour': h, 'total_consumption': val}, ...]
    """
    conn = get_connection()
    cur = conn.cursor()
    
    # Calculate start and end timestamps for the day in milliseconds
    # Assuming date is 'YYYY-MM-DD'
    # We need to filter by the 'hour' column which is a timestamp (start of the hour)
    
    try:
        dt = datetime.strptime(date, '%Y-%m-%d')
        start_ts = int(dt.timestamp() * 1000)
        end_ts = start_ts + (24 * 3600 * 1000)
    except ValueError:
        return []

    cur.execute("""
        SELECT hour, total_consumption 
        FROM hourly_consumption 
        WHERE device_id = %s AND hour >= %s AND hour < %s
        ORDER BY hour ASC
    """, (device_id, start_ts, end_ts))
    
    rows = cur.fetchall()
    cur.close()
    conn.close()
    
    result = []
    for row in rows:
        result.append({
            "hour": row[0],
            "total_consumption": row[1]
        })
    return result
