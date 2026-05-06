package com.divita.shield.kafka.event;

import java.time.Instant;

public class AnomalyEvent {
    private String clientId;
    private String anomalyType;
    private String reason;
    private int severity;
    private Instant timestamp;

    public AnomalyEvent() {

    }

    private AnomalyEvent(Builder builder) {
        this.clientId = builder.clientId;
        this.anomalyType = builder.anomalyType;
        this.reason = builder.reason;
        this.severity = builder.severity;
        this.timestamp = builder.timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public String getReason() {
        return reason;
    }

    public String getAnomalyType() {
        return anomalyType;
    }

    public int getSeverity() {
        return severity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientId;
        private String anomalyType;
        private String reason;
        private int severity;
        private Instant timestamp;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder anomalyType(String anomalyType) {
            this.anomalyType = anomalyType;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder severity(int severity) {
            this.severity = severity;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AnomalyEvent build() {
            return new AnomalyEvent(this);
        }
    }
}
