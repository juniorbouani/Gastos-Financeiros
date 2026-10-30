package com.georgesbouanni.controle_gastos.repository;
import com.georgesbouanni.controle_gastos.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface ShopOrderRepository extends JpaRepository<ShopOrder,Long> {
 Optional<ShopOrder> findByUserIdAndRequestId(Long userId,String requestId);
 Page<ShopOrder> findByUserId(Long userId,Pageable pageable);
}
