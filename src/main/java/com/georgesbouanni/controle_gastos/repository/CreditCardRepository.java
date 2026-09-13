package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findByUserId(Long userId);
}
