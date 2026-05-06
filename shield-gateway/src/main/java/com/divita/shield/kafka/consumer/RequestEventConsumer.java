package com.divita.shield.kafka.consumer;

import com.divita.shield.kafka.event.AnomalyEvent;
import com.divita.shield.kafka.event.RequestEvent;
import com.divita.shield.kafka.producer.AnomalyEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestEventConsumer {
    private final Map<String, Integer> failedLoginCounts = new ConcurrentHashMap<>();
    private final AnomalyEventPublisher anomalyEventPublisher;

    public RequestEventConsumer(AnomalyEventPublisher anomalyEventPublisher) {
        this.anomalyEventPublisher = anomalyEventPublisher;
    }

    @KafkaListener(
            topics = "shield.requests",
            groupId = "shield-analyzer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumer(RequestEvent event) {
        System.out.println("EVENT_RECEIVED ==> " +
                event.getClientId() + " | " +
                event.getEndpoint() + " | " +
                event.getDecision()
                );

        if("/login".equals(event.getEndpoint()) &&
            "FAILED_LOGIN".equals(event.getDecision())
        ) {
            int count = failedLoginCounts.merge(event.getClientId(), 1, Integer::sum);

            System.out.println("FAILED_LOGIN ==> " + event.getClientId() + " = " + count);

            if(count >= 3) {
                System.out.println("ANOMALY DETECTED ==> Possible brute-force attack from client " + event.getClientId());
                AnomalyEvent anomalyEvent = AnomalyEvent.builder()
                        .clientId(event.getClientId())
                        .anomalyType("BRUTE_FORCE_LOGIN")
                        .reason("Multiple failed login attempts detected")
                        .severity(8)
                        .build();

                anomalyEventPublisher.publish(anomalyEvent);
            }
        }
    }
}
