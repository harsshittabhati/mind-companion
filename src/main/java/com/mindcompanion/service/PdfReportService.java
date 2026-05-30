package com.mindcompanion.service;

import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final AnalyticsService analyticsService;
    private final GamificationService gamificationService;
    private final MoodEntryRepository moodEntryRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float LINE_HEIGHT = 18f;

    /**
     * Generates a PDF wellness report for the given user.
     * Returns the PDF as a byte array.
     */
    public byte[] generateWellnessReport(User user) throws IOException {

        Map<String, Object> dashboard =
                analyticsService.getFullDashboard(user);
        Map<String, Object> gamification =
                gamificationService.getGamificationProfile(user);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page)) {

                float y = PDRectangle.A4.getHeight() - MARGIN;

                // ── Header ───────────────────────────────────────
                y = drawText(cs, "Mind Companion — Wellness Report",
                        MARGIN, y, 18, true);
                y = drawText(cs,
                        "Generated: " + LocalDateTime.now().format(FORMATTER),
                        MARGIN, y - 5, 10, false);
                y = drawText(cs, "User: " + user.getUsername(),
                        MARGIN, y, 10, false);
                y -= 15;
                y = drawLine(cs, y);
                y -= 10;

                // ── Gamification ─────────────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> xpStatus =
                        (Map<String, Object>) gamification.get("xpStatus");

                y = drawText(cs, "Gamification Progress",
                        MARGIN, y, 14, true);
                y -= 5;
                y = drawText(cs, "Level: " + xpStatus.get("level")
                                + "  (" + xpStatus.get("levelTitle") + ")",
                        MARGIN, y, 11, false);
                y = drawText(cs, "Total XP: " + xpStatus.get("totalXp"),
                        MARGIN, y, 11, false);
                y = drawText(cs, "Current Streak: "
                                + gamification.get("streak") + " days",
                        MARGIN, y, 11, false);
                y = drawText(cs, "Badges Earned: "
                                + gamification.get("badgeCount"),
                        MARGIN, y, 11, false);
                y -= 10;

                // ── Session Stats ────────────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> stats =
                        (Map<String, Object>) dashboard.get("sessionStats");

                y = drawLine(cs, y);
                y -= 10;
                y = drawText(cs, "Session Statistics",
                        MARGIN, y, 14, true);
                y -= 5;
                y = drawText(cs, "Total Messages Sent: "
                        + stats.get("totalMessages"), MARGIN, y, 11, false);
                y = drawText(cs, "Positive Message Rate: "
                                + stats.get("positiveRate") + "%",
                        MARGIN, y, 11, false);
                y = drawText(cs, "Crisis Incidents: "
                        + stats.get("crisisCount"), MARGIN, y, 11, false);
                y -= 10;

                // ── Mood Summary ─────────────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> mood7 =
                        (Map<String, Object>) dashboard.get("moodAverage7Days");
                @SuppressWarnings("unchecked")
                Map<String, Object> mood30 =
                        (Map<String, Object>) dashboard.get("moodAverage30Days");

                y = drawLine(cs, y);
                y -= 10;
                y = drawText(cs, "Mood Summary",
                        MARGIN, y, 14, true);
                y -= 5;
                y = drawText(cs, "7-Day Average Mood: "
                                + mood7.get("averageMood") + " / 10",
                        MARGIN, y, 11, false);
                y = drawText(cs, "30-Day Average Mood: "
                                + mood30.get("averageMood") + " / 10",
                        MARGIN, y, 11, false);
                y = drawText(cs, "Check-ins (last 7 days): "
                        + mood7.get("totalEntries"), MARGIN, y, 11, false);
                y -= 10;

                // ── Sentiment Breakdown ──────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Long> sentiment =
                        (Map<String, Long>) dashboard.get("sentimentBreakdown");

                y = drawLine(cs, y);
                y -= 10;
                y = drawText(cs, "Sentiment Breakdown",
                        MARGIN, y, 14, true);
                y -= 5;
                for (Map.Entry<String, Long> entry : sentiment.entrySet()) {
                    y = drawText(cs, entry.getKey() + ": "
                                    + entry.getValue() + " messages",
                            MARGIN, y, 11, false);
                }
                y -= 10;

                // ── Recent Mood Entries ──────────────────────────
                List<MoodEntry> recentMoods = moodEntryRepository
                        .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                                user.getId(),
                                LocalDateTime.now().minusDays(7));

                if (!recentMoods.isEmpty()) {
                    y = drawLine(cs, y);
                    y -= 10;
                    y = drawText(cs, "Recent Mood Check-ins (Last 7 Days)",
                            MARGIN, y, 14, true);
                    y -= 5;
                    for (MoodEntry entry : recentMoods) {
                        if (y < 80) break; // stop if near page bottom
                        String line = entry.getEntryDate()
                                + "  —  Score: " + entry.getMoodScore()
                                + "/10  (" + entry.getMoodLevel() + ")";
                        y = drawText(cs, line, MARGIN, y, 10, false);
                    }
                }

                // ── Footer ───────────────────────────────────────
                drawText(cs,
                        "This report is confidential and generated by Mind Companion.",
                        MARGIN, 40f, 9, false);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ─── Drawing helpers ─────────────────────────────

    private float drawText(PDPageContentStream cs, String text,
                           float x, float y,
                           int fontSize, boolean bold) throws IOException {
        PDType1Font font = bold
                ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private float drawLine(PDPageContentStream cs,
                           float y) throws IOException {
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        return y;
    }
}