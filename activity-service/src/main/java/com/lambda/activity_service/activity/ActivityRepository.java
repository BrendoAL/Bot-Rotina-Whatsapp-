package com.lambda.activity_service.activitymodule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserId(Long userId);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.userId = :userId AND a.date BETWEEN :start AND :end")
    int countByUserIdAndDateBetween(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(a.durationMinutes), 0) FROM Activity a WHERE a.userId = :userId AND a.date BETWEEN :start AND :end")
    int sumDurationByUserIdAndDateBetween(@Param("userId") Long userId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);
}
