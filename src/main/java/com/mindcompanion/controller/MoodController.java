package com.mindcompanion.controller;

import com.mindcompanion.dto.request.MoodRequest;
import com.mindcompanion.dto.response.MoodResponse;
import com.mindcompanion.service.MoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
@Slf4j
public class MoodController {

    private final MoodService moodService;

    // ─── Save today's mood check-in ─────────────────
    @PostMapping("/checkin")
    public ResponseEntity<MoodResponse> saveMoodEntry(
            @Valid @RequestBody MoodRequest request,
            Principal principal) {

        log.debug("Mood check-in from: {}", principal.getName());
        MoodResponse response = moodService
                .saveMoodEntry(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    // ─── Get today's mood ────────────────────────────
    @GetMapping("/today")
    public ResponseEntity<MoodResponse> getTodayMood(
            Principal principal) {

        MoodResponse response = moodService
                .getTodayMood(principal.getName());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    // ─── Get full mood history ───────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<MoodResponse>> getMoodHistory(
            Principal principal) {

        List<MoodResponse> history = moodService
                .getMoodHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    // ─── Get last 7 days for chart ───────────────────
    @GetMapping("/weekly")
    public ResponseEntity<List<MoodResponse>> getWeeklyMood(
            Principal principal) {

        List<MoodResponse> weekly = moodService
                .getWeeklyMood(principal.getName());
        return ResponseEntity.ok(weekly);
    }
}