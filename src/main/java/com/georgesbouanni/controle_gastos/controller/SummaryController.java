package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.MonthlySummaryResponse;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/summary")
public class SummaryController {

    private final SummaryService service;
    private final CurrentUserService currentUser;

    @Autowired
    public SummaryController(SummaryService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/monthly")

    public MonthlySummaryResponse monthly(@RequestParam(required = false) YearMonth month) {
        return service.monthly(currentUser.getId(), month != null ? month : YearMonth.now());
    }
}
