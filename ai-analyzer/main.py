import os
import json
import threading

from groq import Groq
from dotenv import load_dotenv
from fastapi import FastAPI
from pydantic import BaseModel
from kafka import KafkaConsumer
from contextlib import asynccontextmanager

load_dotenv()

@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer_thread = threading.Thread(
        target=consume_anomalies,
        daemon=True
    )
    consumer_thread.start()

    yield

app = FastAPI(title="Adaptive API Shield AI Analyzer",
            lifespan=lifespan)

client = Groq(api_key=os.getenv("GROQ_API_KEY"))

class AnomalyRequest(BaseModel):
    clientId: str
    anomalyType: str
    reason: str
    severity: int

@app.get("/health")
def health():
    return {"status": "ai analyzer is running"}


def generate_ai_explanation(anomaly_data: dict):
    prompt = f"""
    You are a security incident analysis assistant.

    Analyze this API abuse anomaly:

    Client ID: {anomaly_data.get("clientId")}
    Anomaly Type: {anomaly_data.get("anomalyType")}
    Reason: {anomaly_data.get("reason")}
    Severity: {anomaly_data.get("severity")}/10

    Return a concise JSON-style explanation with:
    - risk_level
    - why_suspicious
    - recommended_action
    - confidence
    """

    response = client.chat.completions.create(
        model="llama-3.1-8b-instant",
        messages=[
            {"role": "system", "content": "You explain API abuse incidents clearly and concisely."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.2
    )

    explanation = response.choices[0].message.content

    print("\n================ AI INCIDENT ANALYSIS ================\n")
    print(explanation)
    print("\n=====================================================\n")


@app.post("/explain")
def explain_anomaly(anomaly: AnomalyRequest):
    generate_ai_explanation(anomaly.model_dump())

    return {
        "message": "AI explanation generated"
    }

def consume_anomalies():

    consumer = KafkaConsumer(
        "shield.anomalies",
        bootstrap_servers="localhost:9092",
        auto_offset_reset="latest",
        group_id="ai-analyzer-group",
        value_deserializer=lambda m: json.loads(m.decode("utf-8"))
    )

    print("AI Analyzer Kafka consumer started...")

    for message in consumer:
        anomaly_event = message.value
        print(f"\nReceived anomaly event: {anomaly_event}\n")

        try:
            generate_ai_explanation(anomaly_event)
        except Exception as e:
            print(f"Error generating AI explanation: {e}")


