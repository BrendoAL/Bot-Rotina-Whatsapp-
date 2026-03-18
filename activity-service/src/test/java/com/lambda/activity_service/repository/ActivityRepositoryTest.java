package com.lambda.activity_service.controller;

import com.lambda.activity_service.activitymodule.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ActivityRepository")
class ActivityRepositoryTest {

    @Autowired ActivityRepository activityRepository;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastWeek = today.minusDays(8);

        // 2 atividades hoje
        activityRepository.save(Activity.builder().userId(1L).category("ESTUDO").durationMinutes(90).date(today).source("API").build());
        activityRepository.save(Activity.builder().userId(1L).category("TREINO").durationMinutes(60).date(today).source("API").build());
        // 1 atividade ontem
        activityRepository.save(Activity.builder().userId(1L).category("LEITURA").durationMinutes(30).date(yesterday).source("WHATSAPP").build());
        // 1 atividade fora da semana
        activityRepository.save(Activity.builder().userId(1L).category("ESTUDO").durationMinutes(120).date(lastWeek).source("API").build());
        // atividade de outro usuário
        activityRepository.save(Activity.builder().userId(2L).category("ESTUDO").durationMinutes(60).date(today).source("API").build());
    }

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("deve retornar apenas atividades do usuário informado")
        void shouldReturnOnlyUserActivities() {
            List<Activity> result = activityRepository.findByUserId(1L);
            assertThat(result).hasSize(4);
            assertThat(result).allMatch(a -> a.getUserId().equals(1L));
        }

        @Test
        @DisplayName("não deve retornar atividades de outros usuários")
        void shouldNotReturnOtherUsersActivities() {
            List<Activity> result = activityRepository.findByUserId(1L);
            assertThat(result).noneMatch(a -> a.getUserId().equals(2L));
        }
    }

    @Nested
    @DisplayName("countByUserIdAndDateBetween()")
    class CountByDateRange {

        @Test
        @DisplayName("deve contar atividades de hoje corretamente")
        void shouldCountTodayActivities() {
            LocalDate today = LocalDate.now();
            int count = activityRepository.countByUserIdAndDateBetween(1L, today, today);
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("deve contar atividades da semana corretamente")
        void shouldCountWeekActivities() {
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(6);
            int count = activityRepository.countByUserIdAndDateBetween(1L, weekAgo, today);
            assertThat(count).isEqualTo(3); // hoje(2) + ontem(1), fora da semana não conta
        }
    }

    @Nested
    @DisplayName("sumDurationByUserIdAndDateBetween()")
    class SumDurationByDateRange {

        @Test
        @DisplayName("deve somar minutos de hoje corretamente")
        void shouldSumTodayMinutes() {
            LocalDate today = LocalDate.now();
            int sum = activityRepository.sumDurationByUserIdAndDateBetween(1L, today, today);
            assertThat(sum).isEqualTo(150); // 90 + 60
        }

        @Test
        @DisplayName("deve somar minutos da semana corretamente")
        void shouldSumWeekMinutes() {
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(6);
            int sum = activityRepository.sumDurationByUserIdAndDateBetween(1L, weekAgo, today);
            assertThat(sum).isEqualTo(180); // 90 + 60 + 30
        }

        @Test
        @DisplayName("deve retornar zero quando não há atividades no período")
        void shouldReturnZeroWhenNoActivities() {
            LocalDate future = LocalDate.now().plusDays(10);
            int sum = activityRepository.sumDurationByUserIdAndDateBetween(1L, future, future);
            assertThat(sum).isEqualTo(0);
        }
    }
}
