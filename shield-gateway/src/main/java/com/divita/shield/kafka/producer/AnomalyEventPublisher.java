package com.divita.shield.kafka.producer;

import com.divita.shield.kafka.event.AnomalyEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnomalyEventPublisher {

    private static final String ANOMALY_TOPIC = "shield.anomalies";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AnomalyEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(AnomalyEvent anomalyEvent) {
        try {
            kafkaTemplate.send(ANOMALY_TOPIC, anomalyEvent.getClientId(), anomalyEvent)
                    .whenComplete((result, exception) -> {;
                        if(exception != null) {
                            System.err.println("Failed to publish anomaly event: " + exception.getMessage());
                        } else {
                            System.out.println("Anomaly event published successfully: " + anomalyEvent.getClientId() + " | " + anomalyEvent.getAnomalyType() + " | " + anomalyEvent.getReason());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Failed to publish anomaly event: " + e.getMessage());
        }
    }
}
