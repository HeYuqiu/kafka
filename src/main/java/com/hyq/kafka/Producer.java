package com.hyq.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendWithKey(String key, String content) {
        kafkaTemplate.send("test11", key, content);
    }

    public void send(String content) {
        kafkaTemplate.send("test11", content);
    }
}
