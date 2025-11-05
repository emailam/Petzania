package com.example.friends.and.chats.module.util;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Component
public class QueueUtils {
    public int getRetryCount(Message message, String retryQueueName) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof java.util.List<?> deaths) {
            for (Object death : deaths) {
                if (death instanceof java.util.Map<?, ?> deathMap) {
                    Object queue = deathMap.get("queue");
                    Object count = deathMap.get("count");
                    if (retryQueueName.equals(queue) && count instanceof Number countNum) {
                        return countNum.intValue();
                    }
                }
            }
        }
        return 0;
    }
}
