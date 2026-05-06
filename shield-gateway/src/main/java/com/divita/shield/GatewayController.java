package com.divita.shield;

import com.divita.shield.kafka.event.RequestEvent;
import com.divita.shield.kafka.producer.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@RestController
public class GatewayController {

    private final WebClient webClient;
    private final RateLimiterService rateLimiterService;
    private final TrustScoreService trustScoreService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public GatewayController(@Value("${backend.base-url}") String backendBaseUrl,
                             RateLimiterService rateLimiterService,
                             TrustScoreService trustScoreService,
                             KafkaEventPublisher kafkaEventPublisher) {
        this.trustScoreService = trustScoreService;
        this.rateLimiterService = rateLimiterService;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.webClient = WebClient.builder()
                .baseUrl(backendBaseUrl)
                .build();
    }

    private ResponseEntity<?> checkRateLimit(String clientId, String endpoint) {
        if(trustScoreService.isBlocked(clientId)) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "message", "Access blocked due to low trust score.",
                            "clientId", clientId
                    )
            );
        }

        boolean allowed = rateLimiterService.isAllowed(clientId, endpoint);

        if(!allowed) {
            int updatedScore = trustScoreService.decreaseScore(clientId, 10);

            if(updatedScore < 20) {
                trustScoreService.applyProgressiveBlock(clientId);
            }

            return ResponseEntity.status(429).body(
                    Map.of(
                            "message", "Too many requests. Please try again later.",
                            "clientId", clientId,
                            "endpoint", endpoint,
                            "trustScore", updatedScore
                    )
            );
        }

        return null;
    }

    private void publishEvent(String clientId, String endpoint, String method, String decision, int statusCode) {
        RequestEvent event = RequestEvent.builder()
                .clientId(clientId)
                .endpoint(endpoint)
                .method(method)
                .decision(decision)
                .statusCode(statusCode)
                .trustScore(trustScoreService.getScore(clientId))
                .timestamp(Instant.now())
                .build();

        kafkaEventPublisher.publishRequestEvent(event);

    }

    @GetMapping("/products")
    public ResponseEntity<?> products(@RequestHeader(value = "X-Client-Id", defaultValue = "anonymous") String clientId) {

        ResponseEntity<?> blocked = checkRateLimit(clientId, "/products");
        if(blocked != null) {
            publishEvent(clientId, "/products", "GET", "THROTTLE", 429);
            return blocked;
        }

        Object response = webClient.get()
                .uri("/products")
                .retrieve()
                .bodyToMono(Object.class)
                .block();

        publishEvent(clientId, "/products", "GET", "ALLOWED", 200);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/shield/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "shield-gateway running",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> orders(
            @RequestHeader(value = "X-Client-Id", defaultValue = "anonymous") String clientId,
            @RequestBody(required = false) Map<String, Object> body) {

        ResponseEntity<?> blocked = checkRateLimit(clientId, "/orders");
        if(blocked != null) {
            publishEvent(clientId, "/orders", "POST", "THROTTLE", 429);
            return blocked;
        }

        Object response = webClient.post()
                .uri("/orders")
                .bodyValue(body == null ? Map.of() : body)
                .retrieve()
                .bodyToMono(Object.class)
                .block();

        publishEvent(clientId, "/orders", "POST", "ALLOWED", 200);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestHeader(value = "X-Client-Id", defaultValue = "anonymous") String clientId,
            @RequestBody Map<String, String> body) {

        ResponseEntity<?> blocked = checkRateLimit(clientId, "/login");
        if(blocked != null) {
            return blocked;
        }

        ResponseEntity<Object> response = webClient.post()
                .uri("/login")
                .bodyValue(body)
                .exchangeToMono(res ->
                        res.bodyToMono(Object.class)
                                .map(responseBody ->
                                        ResponseEntity
                                                .status(res.statusCode())
                                                .body(responseBody)
                                )
                )
                .block();

        if (response != null && response.getStatusCode().value() == 401) {
            trustScoreService.decreaseScore(clientId, 15);
            publishEvent(clientId, "/login", "POST", "FAILED_LOGIN", 401);
        } else {
            publishEvent(clientId, "/login", "POST", "ALLOW", response == null ? 200 : response.getStatusCode().value());
        }


        return response;
    }

    @GetMapping("/shield/status/{clientId}")
    public ResponseEntity<?> clientStatus(@PathVariable String clientId) {
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "trustScore", trustScoreService.getScore(clientId),
                "blocked", trustScoreService.isBlocked(clientId),
                "offenseCount", trustScoreService.getOffenseCount(clientId)
        ));
    }


}
