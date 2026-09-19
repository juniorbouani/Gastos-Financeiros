package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.CardBlockedException;
import com.georgesbouanni.controle_gastos.exception.InsuficientBalanceException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.CreditCardRepository;
import com.georgesbouanni.controle_gastos.repository.InvoiceItemRepository;
import com.georgesbouanni.controle_gastos.repository.InvoiceRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public CreditCardService(CreditCardRepository cardRepository, InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository, UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.userRepository = userRepository;
    }

    public CreditCard requestCard(Long userId, BigDecimal limiteTotal, boolean descartavel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encotrando de id: " + userId));

        String numeroMascarado = gerarNumeroMascarado();
        LocalDate expiracao = descartavel ? LocalDate.now().plusDays(1) : LocalDate.now().plusYears(5);

        CreditCard card = new CreditCard(user, numeroMascarado, limiteTotal, descartavel, expiracao);
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

    public CreditCard blockCard(Long cardId) {
        CreditCard card = findCardorThrow(cardId);
        card.setStatus(CreditCardStatus.BLOQUEADO);
        return cardRepository.save(card);
    }

    public CreditCard unblockCard(Long cardId) {
        CreditCard card = findCardorThrow(cardId);
        card.setStatus(CreditCardStatus.ATIVO);
        return cardRepository.save(card);
    }

    public CreditCard setSpendingLimit(Long cardId, BigDecimal novoLimite) {
        CreditCard card = findCardorThrow(cardId);
        BigDecimal diferenca = novoLimite.subtract(card.getLimiteTotal());
        card.setLimiteTotal(novoLimite);
        card.setLimiteTotal(card.getLimiteDisponivel().add(diferenca));
        return cardRepository.save(card);
    }

    public InvoiceItem registerPurchase(Long cardId, String descricao, BigDecimal valor, int totalParcelas) {
        CreditCard card = findCardorThrow(cardId);

        if (card.getStatus() == CreditCardStatus.BLOQUEADO) {
            throw new CardBlockedException("Este cartão está bloqueado.");
        }

        if (card.getStatus() == CreditCardStatus.CANCELADO) {
            throw new CardBlockedException("Este cartão está cancelado.");
        }

        if (card.getLimiteDisponivel().compareTo(valor) < 0) {
            throw new InsuficientBalanceException("Limite de credito insuficiente.");
        }

        card.setLimiteDisponivel(card.getLimiteDisponivel().subtract(valor));
        cardRepository.save(card);

        BigDecimal valorParcela = valor.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.HALF_UP);
        YearMonth mesAtual = YearMonth.now();
        InvoiceItem primeiroItem = null;

        for (int i = 1; i<= totalParcelas; i++) {
            YearMonth mesParcela = mesAtual.plusMonths(i - 1);
            Invoice invoice = findCreateInvoice(card, mesParcela);

            InvoiceItem item = new InvoiceItem(invoice, descricao, valorParcela, LocalDate.now(), i, totalParcelas);
            invoiceItemRepository.save(item);

            invoice.setValorTotal(invoice.getValorTotal().add(valorParcela));
            invoiceRepository.save(invoice);

            if (i == 1) primeiroItem = item;
        }

        return primeiroItem;
    }

    private Invoice findCreateInvoice(CreditCard card, YearMonth mesReferencia) {
        return invoiceRepository.findByCreditCardIdAndMesReferencia(card.getId(), mesReferencia)
                .orElseGet(() -> {
                    LocalDate vencimento = mesReferencia.plusMonths(1).atDay(10);
                    Invoice novaFatura = new Invoice(card, mesReferencia, vencimento);
                    return invoiceRepository.save(novaFatura);
                });
    }

    public Invoice payInvoice(Long invoiceId, BigDecimal valorPagamento) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->  new ResourceNotFoundException("Fatura não encontrada com id: " + invoiceId));

        User user = invoice.getCreditCard().getUser();

        if (user.getBalance().compareTo(valorPagamento) < 0) {
            throw new InsuficientBalanceException("Saldo insuficiente para pagar a fatura.");

        }

        user.setBalance(user.getBalance().subtract(valorPagamento));

        CreditCard card = invoice.getCreditCard();
        card.setLimiteDisponivel(card.getLimiteDisponivel().add(valorPagamento));
        cardRepository.save(card);

        if (invoice.getValorPago().compareTo(invoice.getValorTotal()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAGA);
        }
        else {
            invoice.setStatus(InvoiceStatus.PARCIAL);
        }

        return invoiceRepository.save(invoice);
    }

    public InvoiceItem disputeItem(Long itemId) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado com id: " + itemId));

        item.setEstornado(true);

        Invoice invoice = item.getInvoice();
        invoice.setValorTotal(invoice.getValorTotal().subtract(item.getValor()));
        invoiceRepository.save(invoice);

        CreditCard card = invoice.getCreditCard();
        invoice.setValorTotal(invoice.getValorTotal().subtract(item.getValor()));
        cardRepository.save(card);

        return invoiceItemRepository.save(item);
    }

    private CreditCard findCardorThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado com id:" + cardId));
    }
}