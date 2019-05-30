package com.hyq.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka")
public class SendController {
    @Autowired
    private Producer producer;

    @GetMapping(value = "/send")
        public String send(String content) {
        producer.send(content);
        return "{\"code\":0}";
    }

    @GetMapping(value = "/sendWithKey")
    public String send(String key, String content) {
        producer.sendWithKey(key, content);
        return "{\"code\":0}";
    }
}
