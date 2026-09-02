package com.georgesbouanni.controle_gastos.service;

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

    public Optional<Transaction> findById(String id) {
        return repository.findById(id);
    }

    public Transaction save(Transaction transaction) {
        User sender = userRepository.findById(transaction.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Remetente não encontrado com id: " + transaction.getSenderId()));

        if (sender.getBalance().compareTo(transaction.getValue()) < 0) {
            throw new InsuficientBalanceException("Saldo insuficiente para realizar essa transação");
        }

        sender.setBalance(sender.getBalance().subtract(transaction.getValue()));
        userRepository.save(sender);

        if (transaction.getReceiverId() != null) {
            User receiver = userRepository.findById(transaction.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destinatario não encontrado com id: " + transaction.getReceiverId()));

            receiver.setBalance(receiver.getBalance().add(transaction.getValue()));
            userRepository.save(receiver);
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        return repository.save(transaction);
    }

    public Transaction update(String id, Transaction updatedData) {
        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com id: " + id));
        existing.setDescription(updatedData.getDescription());
        existing.setValue(updatedData.getValue());
        existing.setDate(updatedData.getDate());
        existing.setType(updatedData.getType());
        existing.setDestination(updatedData.getDestination());

        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public List<Transaction> listByType(TransactionType type) {
        return repository.findByType(type);
    }

    public List<Transaction> listByPeriod(LocalDate start, LocalDate end) {
        return repository.findByDateBetween(start, end);
    }

    public List<Transaction> listByUser(String userId) {
        return repository.findBySenderIdOrReceiverId(userId, userId);
    }

    public Page<Transaction> listAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
