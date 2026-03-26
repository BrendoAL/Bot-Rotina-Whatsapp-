package com.lambda.activity_service.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserServiceClient userServiceClient;

    public Long create(ActivityRequestDTO dto) {
        userServiceClient.validateUser(dto.userId());

        Activity activity = Activity.builder()
                .userId(dto.userId())
                .title(dto.title())
                .description(dto.description())
                .category(dto.category())
                .durationMinutes(dto.durationMinutes())
                .date(dto.date())
                .source(dto.source() != null ? dto.source() : "API")
                .build();

        return activityRepository.save(activity).getId();
    }

    public List<ActivityResponseDTO> findByUserId(Long userId) {
        return activityRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ActivityStatsDTO getStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        int todayCount   = activityRepository.countByUserIdAndDateBetween(userId, today, today);
        int todayMinutes = activityRepository.sumDurationByUserIdAndDateBetween(userId, today, today);
        int weekCount    = activityRepository.countByUserIdAndDateBetween(userId, weekAgo, today);
        int weekMinutes  = activityRepository.sumDurationByUserIdAndDateBetween(userId, weekAgo, today);

        return new ActivityStatsDTO(todayCount, todayMinutes, weekCount, weekMinutes);
    }

    private ActivityResponseDTO toDTO(Activity a) {
        return new ActivityResponseDTO(
                a.getId(), a.getUserId(), a.getTitle(), a.getDescription(),
                a.getCategory(), a.getDurationMinutes(), a.getDate(), a.getSource()
        );
    }
}
