package com.example.taskapi.service;

import com.example.taskapi.dto.PageResponse;
import com.example.taskapi.dto.TaskRequest;
import com.example.taskapi.dto.TaskResponse;
import com.example.taskapi.exception.BadRequestException;
import com.example.taskapi.exception.NotFoundException;
import com.example.taskapi.model.Task;
import com.example.taskapi.model.TaskStatus;
import com.example.taskapi.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository repository;
    @InjectMocks TaskService service;

    @Test
    void createDefaultsStatusToTodoAndTrimsTitle() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = service.create(new TaskRequest("  Write report  ", "desc", null, null));

        assertEquals("Write report", res.title());
        assertEquals(TaskStatus.TODO, res.status());
    }

    @Test
    void getMissingThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(99L));
    }

    @Test
    void updateChangesFields() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setTitle("old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = service.update(1L, new TaskRequest("new", null, TaskStatus.DONE, null));

        assertEquals("new", res.title());
        assertEquals(TaskStatus.DONE, res.status());
    }

    @Test
    void deleteMissingThrowsNotFoundAndNeverDeletes() {
        when(repository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).delete(any());
    }

    @Test
    void listWithStatusUsesFilteredQuery() {
        Task t = new Task();
        t.setTitle("x");
        when(repository.findByStatus(eq(TaskStatus.DONE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        PageResponse<TaskResponse> page = service.list(TaskStatus.DONE, 1, 10);

        assertEquals(1, page.total());
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listRejectsBadPaging() {
        assertThrows(BadRequestException.class, () -> service.list(null, 0, 10));
        assertThrows(BadRequestException.class, () -> service.list(null, 1, 0));
        assertThrows(BadRequestException.class, () -> service.list(null, 1, 101));
    }
}
