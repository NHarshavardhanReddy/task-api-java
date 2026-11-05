package com.example.taskapi.controller;

import com.example.taskapi.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-tasks.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TaskApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private String token() throws Exception {
        String res = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(res).get("accessToken").asText();
    }

    private long createTask(String auth, String json) throws Exception {
        String res = mvc.perform(post("/tasks").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(res);
        assertTrue(node.has("createdAt"));
        return node.get("id").asLong();
    }

    @Test
    void tasksRequireAuth() throws Exception {
        mvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/tasks/1").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badLoginIs401() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crudLifecycle() throws Exception {
        String auth = token();
        long id = createTask(auth, "{\"title\":\"Write report\",\"dueDate\":\"2026-12-31\"}");

        mvc.perform(get("/tasks/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write report"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-31"));

        mvc.perform(put("/tasks/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(delete("/tasks/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/tasks/" + id).header("Authorization", auth)).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsAre400() throws Exception {
        String auth = token();
        mvc.perform(post("/tasks").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("title"));

        mvc.perform(post("/tasks").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/tasks?status=NOPE").header("Authorization", auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/tasks?page=0").header("Authorization", auth)).andExpect(status().isBadRequest());
    }

    @Test
    void filterAndPagination() throws Exception {
        String auth = token();
        for (int i = 0; i < 5; i++) {
            createTask(auth, "{\"title\":\"t" + i + "\",\"status\":\"" + (i < 3 ? "TODO" : "DONE") + "\"}");
        }

        mvc.perform(get("/tasks?status=DONE").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mvc.perform(get("/tasks?page=2&limit=2").header("Authorization", auth))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("t2"))
                .andExpect(jsonPath("$.total").value(5));
    }
}
