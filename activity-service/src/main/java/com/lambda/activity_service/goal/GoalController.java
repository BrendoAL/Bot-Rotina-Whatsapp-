package com.lambda.activity_service.activitymodule;

import com.lambda.activity_service.dto.*;
import com.lambda.activity_service.exception.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody GoalRequestDTO dto) {
        Long id = goalService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GoalResponseDTO>> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(goalService.findByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        goalService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Exception Handlers ────────────────────────────────────
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Void> handleUserNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(GoalNotFoundException.class)
    public ResponseEntity<Void> handleGoalNotFound() {
        return ResponseEntity.notFound().build();
    }
}
