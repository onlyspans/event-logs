package com.onlyspans.eventlogs.controller;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.service.ISettingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final ISettingsService settingsService;

    @Autowired
    public SettingsController(ISettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto getSettings() {
        logger.debug("Getting settings");
        return settingsService.getSettings();
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public SettingsDto updateSettings(@Valid @RequestBody SettingsDto settings) {
        logger.info("Updating settings");
        return settingsService.updateSettings(settings);
    }
}

