package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.ContractDepartmentBreakdownResponse;
import com.govconnect.analytics.dto.ContractStatusBreakdownResponse;
import com.govconnect.analytics.dto.TopContractorResponse;
import com.govconnect.analytics.repository.ContractAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de analítica de contratos desde DuckDB.
 */
@Service
@RequiredArgsConstructor
public class ContractAnalyticsService {

    private final ContractAnalyticsRepository repository;

    public List<ContractStatusBreakdownResponse> getByStatus() throws SQLException {
        return repository.getByStatus();
    }

    public List<ContractDepartmentBreakdownResponse> getByDepartment() throws SQLException {
        return repository.getByDepartment();
    }

    public List<TopContractorResponse> getTopContractors() throws SQLException {
        return repository.getTopContractors();
    }
}
