package com.divita.shield.kafka.producer;

import com.divita.shield.kafka.event.RequestEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private static final String REQUESTS_TOPIC = "shield.requests";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRequestEvent(RequestEvent event) {
        try {
            kafkaTemplate.send(REQUESTS_TOPIC, event.getClientId(), event)
                    .whenComplete((result, exception) -> {
                        if(exception != null) {
                            System.err.println("Failed to publish event: " + exception.getMessage());
                        } else {
                            System.out.println("Event published successfully: " + event.getClientId() + " | " + event.getEndpoint() + " | " + event.getDecision());
                        }
                    });

        } catch (Exception e) {
            System.err.println("Failed to publish event: " + e.getMessage());
        }

    }
}
