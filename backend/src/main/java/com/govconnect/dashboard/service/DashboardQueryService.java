package com.govconnect.dashboard.service;

import com.govconnect.dashboard.dto.*;
import com.govconnect.dashboard.repository.DashboardQueryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardQueryService {

    private final DashboardQueryRepository dashboardRepository;

    public DashboardQueryService(DashboardQueryRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    // Cambia el tipo de retorno de Summary al que estés utilizando (ej. DashboardSummaryDTO o un nuevo Record)
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