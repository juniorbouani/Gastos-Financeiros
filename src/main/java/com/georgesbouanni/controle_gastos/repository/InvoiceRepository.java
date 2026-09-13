package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Invoice;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCreditCardId(Long creditCardId);
    Optional<Invoice> findByCreditCardIdAndMesReferencia(Long creditCardId, YearMonth mesReferencia);
}
