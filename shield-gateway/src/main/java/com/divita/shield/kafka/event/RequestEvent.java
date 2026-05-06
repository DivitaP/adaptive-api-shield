package com.divita.shield.kafka.event;

import java.time.Instant;

public class RequestEvent {
    private String clientId;
    private String endpoint;
    private String method;
    private String decision;
    private int statusCode;
    private int trustScore;
    private Instant timestamp;

    public RequestEvent() {

    }

    private RequestEvent(Builder builder) {
        this.clientId = builder.clientId;
        this.endpoint = builder.endpoint;
        this.method = builder.method;
        this.decision = builder.decision;
        this.statusCode = builder.statusCode;
        this.trustScore = builder.trustScore;
        this.timestamp = builder.timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getDecision() {
        return decision;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientId;
        private String endpoint;
        private String method;
        private String decision;
        private int statusCode;
        private int trustScore;
        private Instant timestamp;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder trustScore(int trustScore) {
            this.trustScore = trustScore;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RequestEvent build() {
            return new RequestEvent(this);
        }
    }
}
