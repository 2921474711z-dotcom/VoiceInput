package com.voiceinput.pro.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskWorker {

    private final TaskQueueService taskQueueService;
    private final TaskProcessingService taskProcessingService;

    @Scheduled(fixedDelay = 2000L)
    public void consume() {
        for (int i = 0; i < 3; i++) {
            String taskId = taskQueueService.dequeue();
            if (taskId == null || taskId.isBlank()) {
                return;
            }
            taskProcessingService.process(taskId);
        }
    }
}

