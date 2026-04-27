package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.model.LevelStatistics;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LevelStatisticsRepository extends MongoRepository<LevelStatistics, String> {
    Optional<LevelStatistics> findByProgressionLevel(Integer progressionLevel);
    List<LevelStatistics> findAllByOrderByProgressionLevelAsc();
}
