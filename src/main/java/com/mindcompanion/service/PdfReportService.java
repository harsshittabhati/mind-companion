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
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LINE_HEIGHT = 20f;

    // Brand colors (RGB 0-1)
    private static final float[] RED    = {0.914f, 0.271f, 0.376f};
    private static final float[] DARK   = {0.102f, 0.102f, 0.180f};
    private static final float[] GRAY   = {0.420f, 0.420f, 0.420f};
    private static final float[] LIGHT  = {0.969f, 0.980f, 0.980f};
    private static final float[] WHITE  = {1f, 1f, 1f};
    private static final float[] GREEN  = {0.161f, 0.651f, 0.408f};
    private static final float[] PURPLE = {0.486f, 0.227f, 0.929f};

    public byte[] generateWellnessReport(User user) throws IOException {

        Map<String, Object> dashboard = analyticsService.getFullDashboard(user);
        Map<String, Object> gamification = gamificationService.getGamificationProfile(user);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float y = PAGE_HEIGHT;

                // ── Hero header band ─────────────────────────────
                fillRect(cs, 0, PAGE_HEIGHT - 110, PAGE_WIDTH, 110, DARK);
                // Accent strip
                fillRect(cs, 0, PAGE_HEIGHT - 115, PAGE_WIDTH, 5, RED);

                // Title
                setColor(cs, WHITE);
                drawTextRaw(cs, "Mind Companion", MARGIN, PAGE_HEIGHT - 50, 24, true);
                drawTextRaw(cs, "Wellness Report", MARGIN, PAGE_HEIGHT - 76, 14, false);

                // Generated date top right
                String genDate = "Generated: " + LocalDateTime.now().format(FORMATTER);
                drawTextRaw(cs, genDate, PAGE_WIDTH - MARGIN - 200, PAGE_HEIGHT - 50, 9, false);
                String userName = "User: " + (user.getFullName() != null && !user.getFullName().isBlank()
                        ? user.getFullName() : user.getUsername());
                drawTextRaw(cs, userName, PAGE_WIDTH - MARGIN - 200, PAGE_HEIGHT - 64, 9, false);

                y = PAGE_HEIGHT - 130;

                // ── Gamification row — 3 stat boxes ─────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> xpStatus = (Map<String, Object>) gamification.get("xpStatus");
                int streak = ((Number) gamification.get("streak")).intValue();
                int badgeCount = ((Number) gamification.get("badgeCount")).intValue();
                int totalXp = ((Number) xpStatus.get("totalXp")).intValue();
                int level = ((Number) xpStatus.get("level")).intValue();
                String levelTitle = (String) xpStatus.get("levelTitle");

                float boxW = (CONTENT_WIDTH - 20) / 3;
                float boxH = 70;
                float boxY = y - boxH;

                drawStatBox(cs, MARGIN, boxY, boxW, boxH, RED,
                        "Level " + level, levelTitle, totalXp + " XP");
                drawStatBox(cs, MARGIN + boxW + 10, boxY, boxW, boxH, PURPLE,
                        streak + " Days", "Current Streak", "Keep it up!");
                drawStatBox(cs, MARGIN + 2 * (boxW + 10), boxY, boxW, boxH, GREEN,
                        badgeCount + " Badges", "Earned", "Great work!");

                y = boxY - 20;

                // ── Session Statistics ────────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) dashboard.get("sessionStats");

                y = drawSectionHeader(cs, "Session Statistics", y);
                y -= 8;

                float col2x = MARGIN + CONTENT_WIDTH / 2;
                drawLabelValue(cs, "Total Messages", String.valueOf(stats.get("totalMessages")), MARGIN, y);
                drawLabelValue(cs, "Positive Rate", stats.get("positiveRate") + "%", col2x, y);
                y -= LINE_HEIGHT;
                drawLabelValue(cs, "Crisis Incidents", String.valueOf(stats.get("crisisCount")), MARGIN, y);
                drawLabelValue(cs, "Avg Intensity", String.valueOf(stats.get("averageIntensity")), col2x, y);
                y -= 20;

                // ── Mood Summary ─────────────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Object> mood7 = (Map<String, Object>) dashboard.get("moodAverage7Days");
                @SuppressWarnings("unchecked")
                Map<String, Object> mood30 = (Map<String, Object>) dashboard.get("moodAverage30Days");

                y = drawSectionHeader(cs, "Mood Summary", y);
                y -= 8;

                double avg7 = mood7.get("averageMood") != null
                        ? ((Number) mood7.get("averageMood")).doubleValue() : 0;
                double avg30 = mood30.get("averageMood") != null
                        ? ((Number) mood30.get("averageMood")).doubleValue() : 0;

                drawLabelValue(cs, "7-Day Average", avg7 + " / 10", MARGIN, y);
                drawLabelValue(cs, "30-Day Average", avg30 + " / 10", col2x, y);
                y -= LINE_HEIGHT;
                drawLabelValue(cs, "Check-ins (7 days)", String.valueOf(mood7.get("totalEntries")), MARGIN, y);
                y -= 14;

                // Mood bars
                y = drawMoodBars(cs, avg7, avg30, y);
                y -= 20;

                // ── Sentiment Breakdown ──────────────────────────
                @SuppressWarnings("unchecked")
                Map<String, Long> sentiment = (Map<String, Long>) dashboard.get("sentimentBreakdown");

                y = drawSectionHeader(cs, "Sentiment Breakdown", y);
                y -= 8;

                long total = sentiment.values().stream().mapToLong(Long::longValue).sum();
                float[][] sentColors = {GREEN, RED, GRAY, DARK};
                int ci = 0;
                for (Map.Entry<String, Long> entry : sentiment.entrySet()) {
                    float pct = total > 0 ? (entry.getValue() * 100f / total) : 0;
                    float barW = (float)(entry.getValue() * 1.0 / Math.max(total, 1)) * (CONTENT_WIDTH - 120);
                    float[] color = sentColors[ci % sentColors.length];

                    setColor(cs, DARK);
                    drawTextRaw(cs, entry.getKey(), MARGIN, y, 10, false);
                    fillRect(cs, MARGIN + 90, y - 3, Math.max(barW, 2), 12, color);
                    setColor(cs, DARK);
                    drawTextRaw(cs, entry.getValue() + " (" + Math.round(pct) + "%)",
                            MARGIN + 100 + barW, y, 9, false);
                    y -= LINE_HEIGHT;
                    ci++;
                }
                y -= 10;

                // ── Recent Mood Check-ins ────────────────────────
                List<MoodEntry> recentMoods = moodEntryRepository
                        .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                                user.getId(), LocalDateTime.now().minusDays(7));

                if (!recentMoods.isEmpty()) {
                    y = drawSectionHeader(cs, "Recent Mood Check-ins (Last 7 Days)", y);
                    y -= 8;

                    // Table header
                    fillRect(cs, MARGIN, y - 4, CONTENT_WIDTH, 18, LIGHT);
                    setColor(cs, DARK);
                    drawTextRaw(cs, "Date", MARGIN + 4, y, 9, true);
                    drawTextRaw(cs, "Score", MARGIN + 120, y, 9, true);
                    drawTextRaw(cs, "Level", MARGIN + 180, y, 9, true);
                    drawTextRaw(cs, "Notes", MARGIN + 260, y, 9, true);
                    y -= LINE_HEIGHT;

                    for (MoodEntry entry : recentMoods) {
                        if (y < 80) break;
                        setColor(cs, DARK);
                        drawTextRaw(cs, entry.getEntryDate().toString(), MARGIN + 4, y, 9, false);
                        drawTextRaw(cs, entry.getMoodScore() + "/10", MARGIN + 120, y, 9, false);
                        drawTextRaw(cs, entry.getMoodLevel().toString().replace("_", " "),
                                MARGIN + 180, y, 9, false);
                        if (entry.getNotes() != null && !entry.getNotes().isBlank()) {
                            String note = entry.getNotes().length() > 40
                                    ? entry.getNotes().substring(0, 40) + "..." : entry.getNotes();
                            drawTextRaw(cs, note, MARGIN + 260, y, 9, false);
                        }
                        // Alternating row background
                        y -= LINE_HEIGHT;
                    }
                }

                // ── Footer ───────────────────────────────────────
                fillRect(cs, 0, 0, PAGE_WIDTH, 35, DARK);
                setColor(cs, WHITE);
                drawTextRaw(cs,
                        "This report is confidential and generated by Mind Companion  •  " + LocalDateTime.now().format(FORMATTER),
                        MARGIN, 14, 8, false);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ─── Section header ──────────────────────────────
    private float drawSectionHeader(PDPageContentStream cs, String title, float y) throws IOException {
        fillRect(cs, MARGIN, y - 5, CONTENT_WIDTH, 22, LIGHT);
        // Left accent bar
        fillRect(cs, MARGIN, y - 5, 4, 22, RED);
        setColor(cs, DARK);
        drawTextRaw(cs, title, MARGIN + 12, y + 8, 12, true);
        return y - 18;
    }

    // ─── Stat box ────────────────────────────────────
    private void drawStatBox(PDPageContentStream cs, float x, float y,
                             float w, float h, float[] color,
                             String value, String label, String sub) throws IOException {
        fillRect(cs, x, y, w, h, color);
        setColor(cs, WHITE);
        drawTextRaw(cs, value, x + 10, y + h - 22, 16, true);
        drawTextRaw(cs, label, x + 10, y + h - 40, 10, false);
        drawTextRaw(cs, sub, x + 10, y + h - 56, 9, false);
    }

    // ─── Label + value pair ───────────────────────────
    private void drawLabelValue(PDPageContentStream cs, String label,
                                String value, float x, float y) throws IOException {
        setColor(cs, GRAY);
        drawTextRaw(cs, label + ":", x, y, 9, false);
        setColor(cs, DARK);
        drawTextRaw(cs, value, x + 110, y, 10, true);
    }

    // ─── Mood progress bars ───────────────────────────
    private float drawMoodBars(PDPageContentStream cs, double avg7, double avg30, float y) throws IOException {
        float maxBarW = CONTENT_WIDTH - 80;

        setColor(cs, GRAY);
        drawTextRaw(cs, "7-day", MARGIN, y, 9, false);
        float bar7 = (float)(avg7 / 10.0) * maxBarW;
        fillRect(cs, MARGIN + 50, y - 3, maxBarW, 12, LIGHT);
        fillRect(cs, MARGIN + 50, y - 3, Math.max(bar7, 2), 12, RED);
        setColor(cs, DARK);
        drawTextRaw(cs, avg7 + "", MARGIN + 55 + maxBarW, y, 9, true);
        y -= 20;

        setColor(cs, GRAY);
        drawTextRaw(cs, "30-day", MARGIN, y, 9, false);
        float bar30 = (float)(avg30 / 10.0) * maxBarW;
        fillRect(cs, MARGIN + 50, y - 3, maxBarW, 12, LIGHT);
        fillRect(cs, MARGIN + 50, y - 3, Math.max(bar30, 2), 12, PURPLE);
        setColor(cs, DARK);
        drawTextRaw(cs, avg30 + "", MARGIN + 55 + maxBarW, y, 9, true);
        y -= 20;

        return y;
    }

    // ─── Low-level drawing helpers ────────────────────
    private void fillRect(PDPageContentStream cs, float x, float y,
                          float w, float h, float[] rgb) throws IOException {
        cs.setNonStrokingColor(new java.awt.Color(rgb[0], rgb[1], rgb[2]));
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void setColor(PDPageContentStream cs, float[] rgb) throws IOException {
        cs.setNonStrokingColor(new java.awt.Color(rgb[0], rgb[1], rgb[2]));
    }

    private void drawTextRaw(PDPageContentStream cs, String text,
                             float x, float y, int fontSize,
                             boolean bold) throws IOException {
        PDType1Font font = bold
                ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? sanitize(text) : "");
        cs.endText();
    }

    private String sanitize(String text) {
        return text.replaceAll("[^\\x20-\\x7E]", "");
    }
}