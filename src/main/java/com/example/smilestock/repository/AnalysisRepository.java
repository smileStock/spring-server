package com.example.smilestock.repository;

import com.example.smilestock.entity.AnalysisEntity;
import com.example.smilestock.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<AnalysisEntity,Long> {
    Optional<AnalysisEntity> findByStockEntity(StockEntity stockEntity);
}
