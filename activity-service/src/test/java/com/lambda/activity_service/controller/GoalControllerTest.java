package com.lambda.activity_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambda.activity_service.activitymodule.GoalController;
import com.lambda.activity_service.activitymodule.GoalService;
import com.lambda.activity_service.dto.*;
import com.lambda.activity_service.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
@DisplayName("GoalController")
class GoalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean GoalService goalService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/goals
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/goals")
    class CreateGoal {

        @Test
        @DisplayName("deve retornar 201 com ID quando dados válidos")
        void shouldReturn201WhenValid() throws Exception {
            GoalRequestDTO request = new GoalRequestDTO(1L, "ESTUDO", 60, "DIARIO");
            when(goalService.create(any())).thenReturn(5L);

            mockMvc.perform(post("/api/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(5L));
        }

        @Test
        @DisplayName("deve retornar 404 quando userId não existe")
        void shouldReturn404WhenUserNotFound() throws Exception {
            GoalRequestDTO request = new GoalRequestDTO(99L, "ESTUDO", 60, "DIARIO");
            when(goalService.create(any())).thenThrow(new UserNotFoundException(99L));

            mockMvc.perform(post("/api/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando targetMinutes é zero")
        void shouldReturn400WhenTargetMinutesZero() throws Exception {
            GoalRequestDTO request = new GoalRequestDTO(1L, "ESTUDO", 0, "DIARIO");

            mockMvc.perform(post("/api/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando categoria é nula")
        void shouldReturn400WhenCategoryNull() throws Exception {
            GoalRequestDTO request = new GoalRequestDTO(1L, null, 60, "DIARIO");

            mockMvc.perform(post("/api/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando period é nulo")
        void shouldReturn400WhenPeriodNull() throws Exception {
            GoalRequestDTO request = new GoalRequestDTO(1L, "ESTUDO", 60, null);

            mockMvc.perform(post("/api/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/goals/user/{userId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/goals/user/{userId}")
    class ListByUser {

        @Test
        @DisplayName("deve retornar 200 com lista de metas ativas")
        void shouldReturn200WithGoals() throws Exception {
            GoalResponseDTO goal = new GoalResponseDTO(1L, 1L, "ESTUDO", 60, "DIARIO", true);
            when(goalService.findByUserId(1L)).thenReturn(List.of(goal));

            mockMvc.perform(get("/api/goals/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("ESTUDO"))
                    .andExpect(jsonPath("$[0].active").value(true));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando sem metas")
        void shouldReturn200WhenEmpty() throws Exception {
            when(goalService.findByUserId(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/goals/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/goals/{id}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/goals/{id}")
    class DeleteGoal {

        @Test
        @DisplayName("deve retornar 204 quando meta desativada com sucesso")
        void shouldReturn204WhenDeactivated() throws Exception {
            doNothing().when(goalService).deactivate(1L);

            mockMvc.perform(delete("/api/goals/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando meta não encontrada")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new GoalNotFoundException(99L)).when(goalService).deactivate(99L);

            mockMvc.perform(delete("/api/goals/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
