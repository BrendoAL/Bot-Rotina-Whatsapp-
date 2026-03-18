package com.lambda.activity_service.repository;

import com.lambda.activity_service.goal.Goal;
import com.lambda.activity_service.goal.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GoalRepository")
class GoalRepositoryTest {

    @Autowired
    GoalRepository goalRepository;

    @BeforeEach
    void setUp() {
        goalRepository.save(Goal.builder().userId(1L).category("ESTUDO").targetMinutes(60).period("DIARIO").active(true).build());
        goalRepository.save(Goal.builder().userId(1L).category("TREINO").targetMinutes(30).period("SEMANAL").active(false).build());
        goalRepository.save(Goal.builder().userId(2L).category("LEITURA").targetMinutes(20).period("DIARIO").active(true).build());
    }

    @Nested
    @DisplayName("findByUserIdAndActiveTrue()")
    class FindActive {

        @Test
        @DisplayName("deve retornar apenas metas ativas do usuário")
        void shouldReturnOnlyActiveGoals() {
            List<Goal> result = goalRepository.findByUserIdAndActiveTrue(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("ESTUDO");
            assertThat(result.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("não deve retornar metas inativas")
        void shouldNotReturnInactiveGoals() {
            List<Goal> result = goalRepository.findByUserIdAndActiveTrue(1L);
            assertThat(result).noneMatch(g -> !g.isActive());
        }

        @Test
        @DisplayName("não deve retornar metas de outros usuários")
        void shouldNotReturnOtherUsersGoals() {
            List<Goal> result = goalRepository.findByUserIdAndActiveTrue(1L);
            assertThat(result).noneMatch(g -> g.getUserId().equals(2L));
        }
    }
}
