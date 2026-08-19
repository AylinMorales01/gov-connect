package com.govconnect.contracts.service;

import com.govconnect.contracts.dto.ContractResponse;
import com.govconnect.contracts.dto.ContractSummaryResponse;
import com.govconnect.contracts.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de exponer el catálogo de contratos.
 */
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository repository;

    @Transactional(readOnly = true)
    public List<ContractResponse> getContracts(String status, String search) {
        return repository.findAll(status, search);
    }

    @Transactional(readOnly = true)
    public ContractSummaryResponse getSummary() {
        return repository.getSummary();
    }
}
