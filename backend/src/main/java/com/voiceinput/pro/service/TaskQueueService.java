package com.voiceinput.pro.service;

import com.voiceinput.pro.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AppProperties appProperties;

    public void enqueue(String taskId) {
        stringRedisTemplate.opsForList().rightPush(appProperties.getQueueName(), taskId);
    }

    public String dequeue() {
        return stringRedisTemplate.opsForList().leftPop(appProperties.getQueueName());
    }
}

