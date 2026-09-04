package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.exception.InsuficientBalanceException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionStatus;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<Transaction> listAll() {
        return repository.findAll();
    }

    public Page<Transaction> listAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Transaction> findById(Long id) {
        return repository.findById(id);
    }

    public Transaction save(TransactionRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Remetente não encontrado com id: " + request.getSenderId()));

        if (sender.getBalance().compareTo(request.getValue()) < 0) {
            throw new InsuficientBalanceException("Saldo insuficiente para realizar essa transação");
        }

        sender.setBalance(sender.getBalance().subtract(request.getValue()));
        userRepository.save(sender);

        User receiver = null;
        if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destinatario não encontrado com id: " + request.getReceiverId()));
            receiver.setBalance(receiver.getBalance().add(request.getValue()));
            userRepository.save(receiver);
        }

        Transaction transaction = new Transaction(
                request.getDescription(),
                request.getValue(),
                request.getDate(),
                request.getType(),
                TransactionStatus.COMPLETED,
                sender,
                receiver,
                request.getDestination()
        );

        return repository.save(transaction);
    }

    public Transaction update(Long id, Transaction updatedData) {
        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com id: " + id));

        existing.setDescription(updatedData.getDescription());
        existing.setValue(updatedData.getValue());
        existing.setDate(updatedData.getDate());
        existing.setType(updatedData.getType());
        existing.setDestination(updatedData.getDestination());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Transaction> listByType(TransactionType type) {
        return repository.findByType(type);
    }

    public List<Transaction> listByPeriod(LocalDate start, LocalDate end) {
        return repository.findByDateBetween(start, end);
    }

    public List<Transaction> listByUser(Long userId) {
        return repository.findBySenderIdOrReceiverId(userId, userId);
    }
}