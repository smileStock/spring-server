package com.example.smilestock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class AnalysisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int Year;
    private int reportCode;
    private String analysisResult;

    @OneToOne
    private StockEntity stockEntity;
}
