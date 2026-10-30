package com.georgesbouanni.controle_gastos.controller;
import com.georgesbouanni.controle_gastos.dto.*;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.ShopService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/shop")
public class ShopController {
 private final ShopService service;private final CurrentUserService current;
 public ShopController(ShopService service,CurrentUserService current){this.service=service;this.current=current;}
 @GetMapping("/products") public List<ShopProductResponse> products(){return service.products();}
 @GetMapping("/products/{id}") public ShopProductResponse product(@PathVariable String id){return service.product(id);}
 @PostMapping("/orders") public ShopOrderResponse checkout(@Valid @RequestBody ShopCheckoutRequest request){return service.checkout(current.getId(),request);}
 @GetMapping("/orders") public Page<ShopOrderResponse> orders(@PageableDefault(size=20,sort="id",direction=Sort.Direction.DESC) Pageable pageable){return service.list(current.getId(),pageable);}
 @GetMapping("/orders/request/{requestId}") public ShopOrderResponse byRequest(@PathVariable UUID requestId){return service.findRequest(current.getId(),requestId);}
}
