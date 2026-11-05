package com.example.taskapi.dto;

import com.example.taskapi.model.Task;
import com.example.taskapi.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getDueDate(), t.getCreatedAt());
    }
}
