package com.govconnect.dashboard.service;

import com.govconnect.dashboard.dto.*;
import com.govconnect.dashboard.repository.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final DashboardQueryRepository dashboardRepository;

    public DashboardSummaryDTO getSummary() {
        return dashboardRepository.getSummary();
    }

    public List<MonthlyCollectionResponse> getMonthlyCollections() {
        return dashboardRepository.getMonthlyCollections();
    }

    public List<ContractExpiringResponse> getContractsExpiring() {
        return dashboardRepository.getContractsExpiring();
    }

    public List<BudgetExecutionResponse> getBudgetExecution() {
        return dashboardRepository.getBudgetExecution();
    }
}