package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PixKeyRepository extends JpaRepository<PixKey, Long> {

    List<PixKey> findByUserId(Long userId);

    Optional<PixKey> findByKeyValue(String keyValue);

    boolean existsByKeyValue(String keyValue);
}