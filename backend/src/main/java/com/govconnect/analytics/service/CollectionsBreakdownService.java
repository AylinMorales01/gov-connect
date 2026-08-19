package com.govconnect.analytics.service;

import com.govconnect.analytics.dto.ConceptBreakdownResponse;
import com.govconnect.analytics.dto.PaymentMethodBreakdownResponse;
import com.govconnect.analytics.repository.CollectionsBreakdownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de desglose de recaudos por concepto y método de pago.
 */
@Service
@RequiredArgsConstructor
public class CollectionsBreakdownService {

    private final CollectionsBreakdownRepository repository;

    public List<ConceptBreakdownResponse> getByConcept() throws SQLException {
        return repository.getByConcept();
    }

    public List<PaymentMethodBreakdownResponse> getByPaymentMethod() throws SQLException {
        return repository.getByPaymentMethod();
    }
}
