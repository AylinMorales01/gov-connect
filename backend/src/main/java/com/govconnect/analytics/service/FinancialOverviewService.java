package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.FinancialOverviewResponse;
import com.govconnect.analytics.repository.FinancialAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class FinancialOverviewService {

    private final FinancialAnalyticsRepository repository;

    public FinancialOverviewResponse getFinancialOverview() throws SQLException {
        return repository.getFinancialOverview();
    }
}