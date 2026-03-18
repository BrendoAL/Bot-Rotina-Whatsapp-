package com.lambda.activity_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambda.activity_service.activity.*;
import com.lambda.activity_service.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
@DisplayName("ActivityController")
class ActivityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean
    ActivityService activityService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/activities
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/activities")
    class CreateActivity {

        @Test
        @DisplayName("deve retornar 201 com ID quando dados válidos")
        void shouldReturn201WhenValid() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(1L, "Estudar Java", null, "ESTUDO", 90, LocalDate.now(), "API");
            when(activityService.create(any())).thenReturn(10L);

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L));
        }

        @Test
        @DisplayName("deve retornar 404 quando userId não existe no user-service")
        void shouldReturn404WhenUserNotFound() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(99L, "Treino", null, "TREINO", 45, LocalDate.now(), "API");
            when(activityService.create(any())).thenThrow(new UserNotFoundException(99L));

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando durationMinutes é zero")
        void shouldReturn400WhenDurationIsZero() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(1L, "Treino", null, "TREINO", 0, LocalDate.now(), "API");

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando durationMinutes é negativo")
        void shouldReturn400WhenDurationIsNegative() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(1L, "Treino", null, "TREINO", -10, LocalDate.now(), "API");

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando categoria é nula")
        void shouldReturn400WhenCategoryIsNull() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(1L, "Treino", null, null, 60, LocalDate.now(), "API");

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando userId é nulo")
        void shouldReturn400WhenUserIdIsNull() throws Exception {
            ActivityRequestDTO request = new ActivityRequestDTO(null, "Treino", null, "TREINO", 60, LocalDate.now(), "API");

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/activities/user/{userId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/activities/user/{userId}")
    class ListByUser {

        @Test
        @DisplayName("deve retornar 200 com lista de atividades")
        void shouldReturn200WithList() throws Exception {
            ActivityResponseDTO activity = new ActivityResponseDTO(1L, 1L, "Estudar Java", null, "ESTUDO", 90, LocalDate.now(), "API");
            when(activityService.findByUserId(1L)).thenReturn(List.of(activity));

            mockMvc.perform(get("/api/activities/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].category").value("ESTUDO"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando usuário não tem atividades")
        void shouldReturn200WithEmptyList() throws Exception {
            when(activityService.findByUserId(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/activities/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/activities/stats/{userId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/activities/stats/{userId}")
    class Stats {

        @Test
        @DisplayName("deve retornar 200 com estatísticas do usuário")
        void shouldReturn200WithStats() throws Exception {
            ActivityStatsDTO stats = new ActivityStatsDTO(3, 150, 5, 300);
            when(activityService.getStats(1L)).thenReturn(stats);

            mockMvc.perform(get("/api/activities/stats/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayCount").value(3))
                    .andExpect(jsonPath("$.todayMinutes").value(150))
                    .andExpect(jsonPath("$.weekCount").value(5))
                    .andExpect(jsonPath("$.weekMinutes").value(300));
        }
    }
}
