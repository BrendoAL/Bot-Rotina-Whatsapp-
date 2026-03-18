package com.lambda.activity_service.service;

import com.lambda.activity_service.activitymodule.*;
import com.lambda.activity_service.dto.*;
import com.lambda.activity_service.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService")
class GoalServiceTest {

    @Mock GoalRepository goalRepository;
    @Mock UserServiceClient userServiceClient;
    @InjectMocks GoalService goalService;

    // ─────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve salvar meta e retornar ID quando dados válidos")
        void shouldSaveAndReturnId() {
            GoalRequestDTO request = new GoalRequestDTO(1L, "ESTUDO", 60, "DIARIO");
            Goal saved = Goal.builder().id(5L).userId(1L).category("ESTUDO").targetMinutes(60).period("DIARIO").active(true).build();

            doNothing().when(userServiceClient).validateUser(1L);
            when(goalRepository.save(any())).thenReturn(saved);

            assertThat(goalService.create(request)).isEqualTo(5L);
            verify(goalRepository).save(any());
        }

        @Test
        @DisplayName("deve validar userId no user-service antes de salvar")
        void shouldValidateUserBeforeSaving() {
            GoalRequestDTO request = new GoalRequestDTO(1L, "ESTUDO", 60, "DIARIO");
            doNothing().when(userServiceClient).validateUser(1L);
            when(goalRepository.save(any())).thenReturn(Goal.builder().id(1L).build());

            goalService.create(request);

            verify(userServiceClient).validateUser(1L);
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando user-service retorna 404")
        void shouldThrowWhenUserNotFound() {
            GoalRequestDTO request = new GoalRequestDTO(99L, "ESTUDO", 60, "DIARIO");
            doThrow(new UserNotFoundException(99L)).when(userServiceClient).validateUser(99L);

            assertThatThrownBy(() -> goalService.create(request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve salvar meta com active = true por padrão")
        void shouldSaveAsActiveByDefault() {
            GoalRequestDTO request = new GoalRequestDTO(1L, "TREINO", 30, "SEMANAL");
            doNothing().when(userServiceClient).validateUser(1L);
            when(goalRepository.save(any())).thenReturn(Goal.builder().id(1L).build());

            goalService.create(request);

            verify(goalRepository).save(argThat(g -> g.isActive()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByUserId()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("deve retornar apenas metas ativas do usuário")
        void shouldReturnOnlyActiveGoals() {
            Goal active = Goal.builder().id(1L).userId(1L).category("ESTUDO").targetMinutes(60).period("DIARIO").active(true).build();
            when(goalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(active));

            List<GoalResponseDTO> result = goalService.findByUserId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).active()).isTrue();
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há metas ativas")
        void shouldReturnEmptyWhenNoActiveGoals() {
            when(goalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

            assertThat(goalService.findByUserId(1L)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deactivate()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("deve desativar meta quando encontrada")
        void shouldDeactivateGoal() {
            Goal goal = Goal.builder().id(1L).userId(1L).active(true).build();
            when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any())).thenReturn(goal);

            goalService.deactivate(1L);

            verify(goalRepository).save(argThat(g -> !g.isActive()));
        }

        @Test
        @DisplayName("deve lançar GoalNotFoundException quando meta não encontrada")
        void shouldThrowWhenGoalNotFound() {
            when(goalRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.deactivate(99L))
                    .isInstanceOf(GoalNotFoundException.class);

            verify(goalRepository, never()).save(any());
        }
    }
}
