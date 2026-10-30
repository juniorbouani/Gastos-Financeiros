package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import com.georgesbouanni.controle_gastos.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Define preços no servidor e registra pedidos com idempotência na mesma transação da compra. */
@Service
public class ShopService {
    private final UserRepository users;
    private final ShopOrderRepository orders;
    private final CreditCardService cards;
    private static final List<ShopProductResponse> CATALOG =
            List.of(
                    new ShopProductResponse(
                            "speaker",
                            "JBL Speaker",
                            new BigDecimal("499.99"),
                            "JBLSpeaker.jpg",
                            "speaker.html"),
                    new ShopProductResponse(
                            "500", "JBL 500BT", new BigDecimal("199.99"), "JBL500.jpg", "500.html"),
                    new ShopProductResponse(
                            "mouse",
                            "Mouse Gamer",
                            new BigDecimal("99.99"),
                            "MouseGamer.jpg",
                            "mouse.html"),
                    new ShopProductResponse(
                            "teclado",
                            "Teclado RGB",
                            new BigDecimal("189.99"),
                            "TecladoRGB.jpg",
                            "teclado.html"),
                    new ShopProductResponse(
                            "watch",
                            "SmartWatch",
                            new BigDecimal("289.99"),
                            "SmartWatch.jpg",
                            "watch.html"));

    public ShopService(UserRepository users, ShopOrderRepository orders, CreditCardService cards) {
        this.users = users;
        this.orders = orders;
        this.cards = cards;
    }

    public List<ShopProductResponse> products() {
        return CATALOG;
    }

    public ShopProductResponse product(String id) {
        return CATALOG.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
    }

    @Transactional
    public ShopOrderResponse checkout(Long userId, ShopCheckoutRequest request) {
        // Serializa checkouts da mesma conta: o mesmo requestId nunca registra duas compras.
        User user =
                users.findForShopUpdate(userId)
                        .filter(u -> !u.isAccountClosed())
                        .orElseThrow(() -> new ForbiddenException("Conta indisponível."));
        if (request.requestId() == null
                || request.cardId() == null
                || request.totalParcelas() < 1
                || request.totalParcelas() > 12)
            throw new BusinessRuleException(
                    "Informe cartão, identificador do pedido e de 1 a 12 parcelas.");
        String requestId = request.requestId().toString();
        var previous = orders.findByUserIdAndRequestId(userId, requestId);
        if (previous.isPresent()) {
            ShopOrder order = previous.get();
            if (!order.getProductId().equals(request.productId())
                    || !order.getCard().getId().equals(request.cardId())
                    || order.getTotalParcelas() != request.totalParcelas())
                throw new BusinessRuleException(
                        "Este identificador já foi usado em outro pedido. Consulte o histórico.");
            return ShopOrderResponse.from(order);
        }
        ShopProductResponse product = product(request.productId());
        CreditCard card = cards.findOwnedCard(request.cardId(), userId);
        if (card.getStatus() != CreditCardStatus.ATIVO)
            throw new BusinessRuleException("Selecione um cartão ativo.");
        if (card.getDataExpiracao() == null
                || card.getDataExpiracao().isBefore(java.time.LocalDate.now()))
            throw new BusinessRuleException("O cartão está expirado.");
        InvoiceItem item =
                cards.registerPurchase(
                        card.getId(),
                        userId,
                        "Shop: " + product.nome(),
                        product.preco(),
                        request.totalParcelas());
        // A entidade está gerenciada pela transação: cartão descartável é cancelado após esta
        // compra.
        if (card.isDescartavel()) card.setStatus(CreditCardStatus.CANCELADO);
        ShopOrder order =
                orders.save(
                        new ShopOrder(
                                user,
                                card,
                                requestId,
                                product.id(),
                                product.nome(),
                                product.preco(),
                                request.totalParcelas(),
                                item.getInvoice().getId()));
        return ShopOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<ShopOrderResponse> list(Long userId, Pageable pageable) {
        return orders.findByUserId(userId, pageable).map(ShopOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public ShopOrderResponse findRequest(Long userId, UUID requestId) {
        return ShopOrderResponse.from(
                orders.findByUserIdAndRequestId(userId, requestId.toString())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Pedido ainda não encontrado. Você pode reenviar a mesma confirmação com o mesmo identificador.")));
    }
}
