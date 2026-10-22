package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.User;

public class PartyResponse {

    private final Long id;
    private final String name;

    public PartyResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
    }

    public static PartyResponse from(User user) {
        return user == null ? null : new PartyResponse(user);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
