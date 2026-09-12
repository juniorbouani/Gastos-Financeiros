package com.georgesbouanni.controle_gastos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "pix_keys")
public class PixKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O usuário é obrigatório!")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull(message = "O tipo da chave é obrigatório")
    @Enumerated(EnumType.STRING)
    private PixKeyType keyType;

    @NotBlank
    @Column(unique = true)
    private String keyValue;


    public PixKey(User user, PixKeyType keyType, String keyValue) {
        this.user = user;
        this.keyType = keyType;
        this.keyValue = keyValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(PixKeyType keyType) {
        this.keyType = keyType;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }
}
