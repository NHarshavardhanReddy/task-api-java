package com.example.taskapi.service;

import com.example.taskapi.dto.PageResponse;
import com.example.taskapi.dto.TaskRequest;
import com.example.taskapi.dto.TaskResponse;
import com.example.taskapi.exception.BadRequestException;
import com.example.taskapi.exception.NotFoundException;
import com.example.taskapi.model.Task;
import com.example.taskapi.model.TaskStatus;
import com.example.taskapi.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    static final int MAX_LIMIT = 100;

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public TaskResponse create(TaskRequest req) {
        Task task = new Task();
        apply(task, req);
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(TaskStatus status, int page, int limit) {
        if (page < 1) throw new BadRequestException("page must be >= 1");
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id"));
        Page<Task> result = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return new PageResponse<>(result.map(TaskResponse::from).getContent(), page, limit, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    public TaskResponse update(Long id, TaskRequest req) {
        Task task = find(id);
        apply(task, req);
        return TaskResponse.from(repository.save(task));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Task find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
    }

    private void apply(Task task, TaskRequest req) {
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setStatus(req.status() == null ? TaskStatus.TODO : req.status());
        task.setDueDate(req.dueDate());
    }
}
