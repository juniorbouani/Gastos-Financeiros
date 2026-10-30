package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.smartcardio.Card;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

@Service
public class CreditCardService {

    private final CreditCardRepository cardRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public CreditCardService(
            CreditCardRepository cardRepository,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.cardRepository = cardRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public CreditCard requestCard(Long userId, BigDecimal limiteTotal, boolean descartavel) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Usuário não encotrando de id: " + userId));

        String numeroMascarado = gerarNumeroMascarado();
        LocalDate expiracao =
                descartavel ? LocalDate.now().plusDays(1) : LocalDate.now().plusYears(5);

        CreditCard card =
                new CreditCard(user, numeroMascarado, limiteTotal, descartavel, expiracao);
        return cardRepository.save(card);
    }

    private String gerarNumeroMascarado() {
        Random random = new Random();
        int ultimosDigitos = 1000 + random.nextInt(9000);
        return " **** **** **** " + ultimosDigitos;
    }

    public List<CreditCard> listByUser(Long userId) {
        return cardRepository.findByUserId(userId);
    }

    @Transactional
    public CreditCard blockCard(Long cardId, Long userId) {
        CreditCard card = findOwnedCard(cardId, userId);

        if (card.getStatus() == CreditCardStatus.CANCELADO) {
            throw new BusinessRuleException("Este cartão está cancelado.");
        }
        if (card.getStatus() != CreditCardStatus.BLOQUEADO) {
            card.setStatus(CreditCardStatus.BLOQUEADO);
            cardRepository.save(card);
            notificationService.create(
                    card.getUser(), "Cartão " + card.getNumeroMascarado() + " bloqueado.");
        }
        return card;
    }

    @Transactional
    public CreditCard unblockCard(Long cardId, Long userId) {
        CreditCard card = findOwnedCard(cardId, userId);

        if (card.getStatus() == CreditCardStatus.CANCELADO) {
            throw new BusinessRuleException("Este cartão está cancelado.");
        }
        if (card.getStatus() != CreditCardStatus.ATIVO) {
            card.setStatus(CreditCardStatus.ATIVO);
            cardRepository.save(card);
            notificationService.create(
                    card.getUser(), "Cartão " + card.getNumeroMascarado() + " desbloqueado.");
        }
        return card;
    }

    @Transactional
    public CreditCard setSpendingLimit(Long cardId, Long userId, BigDecimal novoLimite) {
        CreditCard card = findOwnedCard(cardId, userId);

        BigDecimal limiteAnterior = card.getLimiteTotal();
        BigDecimal utilizado = limiteAnterior.subtract(card.getLimiteDisponivel());

        if (novoLimite.compareTo(utilizado) < 0) {
            throw new BusinessRuleException(
                    "O novo limite é menor que o valor já utilizado ("
                            + MoneyFormatter.format(utilizado)
                            + ").");
        }

        card.setLimiteTotal(novoLimite);
        card.setLimiteDisponivel(
                card.getLimiteDisponivel().add(novoLimite.subtract(limiteAnterior)));
        cardRepository.save(card);

        notificationService.create(
                card.getUser(),
                "Limite do cartão "
                        + card.getNumeroMascarado()
                        + " alterado de "
                        + MoneyFormatter.format(limiteAnterior)
                        + " para "
                        + MoneyFormatter.format(novoLimite)
                        + ".");
        return card;
    }

    @Transactional
    public InvoiceItem registerPurchase(
            Long cardId, Long userId, String descricao, BigDecimal valor, int totalParcelas) {
        CreditCard card = findOwnedCard(cardId, userId);

        if (card.getStatus() == CreditCardStatus.BLOQUEADO) {
            throw new CardBlockedException("Este cartão está bloqueado.");
        }

        if (card.getStatus() == CreditCardStatus.CANCELADO) {
            throw new CardBlockedException("Este cartão está cancelado.");
        }

        if (card.getLimiteDisponivel().compareTo(valor) < 0) {
            throw new InsuficientBalanceException("Limite de credito insuficiente.");
        }

        if (totalParcelas <= 0
                || valor.scale() > 2
                || valor.compareTo(BigDecimal.valueOf(totalParcelas, 2)) < 0) {
            throw new BusinessRuleException(
                    "Informe um valor com até duas casas decimais e parcelas de pelo menos R$ 0,01.");
        }

        card.setLimiteDisponivel(card.getLimiteDisponivel().subtract(valor));
        cardRepository.save(card);

        BigDecimal valorParcela =
                valor.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.DOWN);
        YearMonth mesAtual = YearMonth.now();
        InvoiceItem primeiroItem = null;

        for (int i = 1; i <= totalParcelas; i++) {
            YearMonth mesParcela = mesAtual.plusMonths(i - 1);
            Invoice invoice = findCreateInvoice(card, mesParcela);

            // A última parcela recebe os centavos restantes, preservando o total da compra.
            BigDecimal valorDestaParcela =
                    i == totalParcelas
                            ? valor.subtract(
                                    valorParcela.multiply(BigDecimal.valueOf(totalParcelas - 1)))
                            : valorParcela;
            InvoiceItem item =
                    new InvoiceItem(
                            invoice,
                            descricao,
                            valorDestaParcela,
                            LocalDate.now(),
                            i,
                            totalParcelas);
            invoiceItemRepository.save(item);

            invoice.setValorTotal(invoice.getValorTotal().add(valorDestaParcela));
            invoiceRepository.save(invoice);

            if (i == 1) primeiroItem = item;
        }

        notificationService.create(
                card.getUser(),
                "Compra de "
                        + MoneyFormatter.format(valor)
                        + " no cartão "
                        + card.getNumeroMascarado()
                        + (totalParcelas > 1 ? " em " + totalParcelas + "x" : "")
                        + ": "
                        + descricao
                        + ".");

        return primeiroItem;
    }

    private Invoice findCreateInvoice(CreditCard card, YearMonth mesReferencia) {
        return invoiceRepository
                .findByCreditCardIdAndMesReferencia(card.getId(), mesReferencia)
                .orElseGet(
                        () -> {
                            LocalDate vencimento = mesReferencia.plusMonths(1).atDay(10);
                            Invoice novaFatura = new Invoice(card, mesReferencia, vencimento);
                            return invoiceRepository.save(novaFatura);
                        });
    }

    @Transactional
    public Invoice payInvoice(Long invoiceId, Long userId, BigDecimal valorPagamento) {
        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Fatura não encontrada com id: " + invoiceId));

        CreditCard card = invoice.getCreditCard();
        User user = card.getUser();

        if (!user.getId().equals(userId)) {
            throw new ForbiddenException("Esta fatura não pertence a você.");
        }

        BigDecimal restante = invoice.getValorTotal().subtract(invoice.getValorPago());
        if (valorPagamento.compareTo(restante) > 0) {
            throw new BusinessRuleException(
                    "O valor excede o saldo devedor da fatura (" + restante + ").");
        }
        if (user.getBalance().compareTo(valorPagamento) < 0) {
            throw new InsuficientBalanceException("Saldo insuficiente para pagar a fatura.");
        }

        user.setBalance(user.getBalance().subtract(valorPagamento));
        userRepository.save(user);

        invoice.setValorPago(invoice.getValorPago().add(valorPagamento));
        card.setLimiteDisponivel(card.getLimiteDisponivel().add(valorPagamento));
        cardRepository.save(card);

        invoice.setStatus(
                invoice.getValorPago().compareTo(invoice.getValorTotal()) >= 0
                        ? InvoiceStatus.PAGA
                        : InvoiceStatus.PARCIAL);

        notificationService.create(
                user,
                "Pagamento de "
                        + MoneyFormatter.format(valorPagamento)
                        + " na fatura de "
                        + invoice.getMesReferencia()
                        + " realizado.");

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceItem disputeItem(Long itemId, Long userId) {
        InvoiceItem item =
                invoiceItemRepository
                        .findById(itemId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Lançamento não encontrado com id: " + itemId));

        Invoice invoice = item.getInvoice();
        CreditCard card = invoice.getCreditCard();

        if (!card.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Este lançamento não pertence a você.");
        }
        if (item.isEstornado()) {
            throw new BusinessRuleException("Este lançamento já foi estornado.");
        }

        item.setEstornado(true);
        invoice.setValorTotal(invoice.getValorTotal().subtract(item.getValor()));
        invoiceRepository.save(invoice);

        card.setLimiteDisponivel(card.getLimiteDisponivel().add(item.getValor()));
        cardRepository.save(card);

        notificationService.create(
                card.getUser(),
                "Lançamento estornado: "
                        + item.getDescricao()
                        + " ("
                        + MoneyFormatter.format(item.getValor())
                        + ").");

        return invoiceItemRepository.save(item);
    }

    private CreditCard findCardorThrow(Long cardId, Long userId) {
        return cardRepository
                .findById(cardId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Cartão não encontrado com id:" + cardId));
    }

    public CreditCard findOwnedCard(Long cardId, Long userId) {
        CreditCard card = findCardorThrow(cardId, userId);
        if (!card.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Esse cartão não pertence a você");
        }
        return card;
    }
}
