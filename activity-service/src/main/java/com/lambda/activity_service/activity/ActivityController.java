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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody ActivityRequestDTO dto) {
        Long id = activityService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityResponseDTO>> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(activityService.findByUserId(userId));
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<ActivityStatsDTO> getStats(@PathVariable Long userId) {
        return ResponseEntity.ok(activityService.getStats(userId));
    }

    // ── Exception Handlers ────────────────────────────────────
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Void> handleUserNotFound() {
        return ResponseEntity.notFound().build();
    }
}
