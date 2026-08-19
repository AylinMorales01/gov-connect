package com.govconnect.contracts.controller;

import com.govconnect.contracts.dto.ContractResponse;
import com.govconnect.contracts.dto.ContractSummaryResponse;
import com.govconnect.contracts.service.ContractService;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión del catálogo de contratos.
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contracts", description = "Endpoints de gestión del catálogo de contratos")
public class ContractController {

    private final ContractService service;

    @GetMapping
    @Operation(
            summary = "Lista contratos",
            description = "Devuelve los contratos con filtros opcionales por estado y búsqueda libre."
    )
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        log.info("Consultando contratos: status={}, search={}", status, search);
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.CONTRACTS_LIST_SUCCESS,
                        service.getContracts(status, search)
                )
        );
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Resumen de contratos",
            description = "Devuelve métricas agregadas del catálogo de contratos."
    )
    public ResponseEntity<ApiResponse<ContractSummaryResponse>> getSummary() {
        log.info("Consultando resumen de contratos");
        return ResponseEntity.ok(
                ApiResponse.success(
                        ApiMessages.CONTRACTS_SUMMARY_SUCCESS,
                        service.getSummary()
                )
        );
    }
}
