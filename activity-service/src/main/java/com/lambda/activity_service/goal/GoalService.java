package com.lambda.activity_service.goal;

import com.lambda.activity_service.activity.UserServiceClient;
import com.lambda.activity_service.exception.GoalNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserServiceClient userServiceClient;

    public Long create(GoalRequestDTO dto) {
        userServiceClient.validateUser(dto.userId());

        Goal goal = Goal.builder()
                .userId(dto.userId())
                .category(dto.category())
                .targetMinutes(dto.targetMinutes())
                .period(dto.period())
                .active(true)
                .build();

        return goalRepository.save(goal).getId();
    }

    public List<GoalResponseDTO> findByUserId(Long userId) {
        return goalRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void deactivate(Long id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));
        goal.setActive(false);
        goalRepository.save(goal);
    }

    private GoalResponseDTO toDTO(Goal g) {
        return new GoalResponseDTO(
                g.getId(), g.getUserId(), g.getCategory(),
                g.getTargetMinutes(), g.getPeriod(), g.isActive()
        );
    }
}
