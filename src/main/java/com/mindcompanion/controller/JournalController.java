package com.mindcompanion.controller;

import com.mindcompanion.dto.request.JournalRequest;
import com.mindcompanion.dto.response.JournalResponse;
import com.mindcompanion.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
@Slf4j
public class JournalController {

    private final JournalService journalService;

    // ─── Save journal entry ──────────────────────────
    @PostMapping("/entry")
    public ResponseEntity<JournalResponse> saveJournalEntry(
            @Valid @RequestBody JournalRequest request,
            Principal principal) {

        log.debug("Journal entry from: {}", principal.getName());
        JournalResponse response = journalService
                .saveJournalEntry(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    // ─── Get today's entry ───────────────────────────
    @GetMapping("/today")
    public ResponseEntity<JournalResponse> getTodayEntry(
            Principal principal) {

        JournalResponse response = journalService
                .getTodayEntry(principal.getName());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    // ─── Get journal history ─────────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<JournalResponse>> getJournalHistory(
            Principal principal) {

        List<JournalResponse> history = journalService
                .getJournalHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    // ─── Get today's AI prompt ───────────────────────
    @GetMapping("/prompt")
    public ResponseEntity<String> getTodayPrompt(
            Principal principal) {

        String prompt = journalService
                .getTodayPrompt(principal.getName());
        return ResponseEntity.ok(prompt);
    }
    // ─── Update journal entry ────────────────────────
    @PutMapping("/entry/{id}")
    public ResponseEntity<JournalResponse> updateJournalEntry(
            @PathVariable Long id,
            @RequestBody JournalRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                journalService.updateJournalEntry(id, request, principal.getName()));
    }

    // ─── Delete journal entry ────────────────────────
    @DeleteMapping("/entry/{id}")
    public ResponseEntity<?> deleteJournalEntry(
            @PathVariable Long id,
            Principal principal) {
        journalService.deleteJournalEntry(id, principal.getName());
        return ResponseEntity.ok(new com.mindcompanion.dto.response.MessageResponse("Entry deleted."));
    }

    // ─── Toggle star journal entry ───────────────────
    @PutMapping("/entry/{id}/star")
    public ResponseEntity<JournalResponse> toggleStar(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(
                journalService.toggleStarJournalEntry(id, principal.getName()));
    }
}