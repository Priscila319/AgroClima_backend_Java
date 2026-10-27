package br.com.agroclima.controller;

import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.SettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasAuthority('settings.manage')")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) { this.settings = settings; }

    @GetMapping
    public JsonNode get() { return settings.get(); }

    @PatchMapping
    public JsonNode update(@RequestBody JsonNode patch, @AuthenticationPrincipal AgroUserPrincipal actor) {
        return settings.update(patch, actor);
    }
}
