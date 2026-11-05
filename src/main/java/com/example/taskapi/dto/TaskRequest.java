package com.example.taskapi.dto;

import com.example.taskapi.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Payload for POST and PUT (PUT is a full replacement). */
public record TaskRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        TaskStatus status,   // optional, defaults to TODO

        LocalDate dueDate    // optional, ISO format yyyy-MM-dd
) {}
