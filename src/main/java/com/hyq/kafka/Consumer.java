package com.hyq.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Consumer {
    @KafkaListener(topics = {"test11"})
    public void listen(ConsumerRecord record) {
        Optional<Object> key = Optional.ofNullable(record.key());
        Optional<Object> value = Optional.ofNullable(record.value());
        key.ifPresent(k -> {
            System.out.println("key is " + k);
        });
        value.ifPresent(val -> {
            System.out.println("value is" + val);
        });
    }
}
