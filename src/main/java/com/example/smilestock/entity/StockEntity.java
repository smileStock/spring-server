package com.example.smilestock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToMany(mappedBy = "stockEntity")
    private List<Member> members = new ArrayList<>();
}
