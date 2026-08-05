package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.DepartmentRankingResponse;
import com.govconnect.analytics.repository.DepartmentRankingRepository;
import com.govconnect.analytics.repository.DepartmentRankingRepository.DepartmentRawData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DepartmentRankingService {

    private final DepartmentRankingRepository repository;

    public List<DepartmentRankingResponse> getDepartmentRanking() throws SQLException {
        List<DepartmentRawData> rawData = repository.getRawRankingData();

        if (rawData.isEmpty()) {
            return List.of();
        }

        // 1. Encontrar el recaudo máximo para normalizar la métrica (evitar que los millones aplasten el porcentaje de ejecución)
        BigDecimal maxCollection = rawData.stream()
                .map(DepartmentRawData::totalCollections)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        if (maxCollection.compareTo(BigDecimal.ZERO) == 0) {
            maxCollection = BigDecimal.ONE;
        }

        BigDecimal finalMaxCollection = maxCollection;

        // 2. Calcular scores y ordenar
        List<DepartmentRankingResponse> rankedList = rawData.stream()
                .map(data -> {
                    // 60% del peso para Ejecución Presupuestal
                    BigDecimal executionScore = data.executionPercentage().multiply(new BigDecimal("0.60"));

                    // 40% del peso para Recaudo (Normalizado a escala 0-100)
                    BigDecimal collectionNormalized = data.totalCollections()
                            .divide(finalMaxCollection, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    BigDecimal collectionScore = collectionNormalized.multiply(new BigDecimal("0.40"));

                    // Score Final
                    BigDecimal totalScore = executionScore.add(collectionScore).setScale(1, RoundingMode.HALF_UP);

                    return new DepartmentRankingResponse(
                            null, // Posición temporal nula
                            data.department(),
                            data.executionPercentage(),
                            data.totalCollections(),
                            totalScore
                    );
                })
                .sorted(Comparator.comparing(DepartmentRankingResponse::score).reversed())
                .toList();

        // 3. Asignar posiciones definitivas
        List<DepartmentRankingResponse> finalList = new ArrayList<>();
        AtomicInteger position = new AtomicInteger(1);

        for (DepartmentRankingResponse item : rankedList) {
            finalList.add(new DepartmentRankingResponse(
                    position.getAndIncrement(),
                    item.department(),
                    item.executionPercentage(),
                    item.totalCollections(),
                    item.score()
            ));
        }

        return finalList;
    }
}