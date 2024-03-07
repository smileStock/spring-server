package com.example.smilestock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class StockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String corpCode;

    private String stockCode;

    @OneToOne(mappedBy = "stockEntity")
    private AnalysisEntity analysisEntity;
}
