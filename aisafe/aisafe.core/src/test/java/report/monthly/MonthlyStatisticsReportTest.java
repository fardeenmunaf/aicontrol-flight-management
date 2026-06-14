package report.monthly;

import org.junit.jupiter.api.Test;
import report.framework.render.OpenPdfReportRenderer;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MonthlyStatisticsReportTest {

    @Test
    void rendersPdfWithData() {
        final MonthlyStatistics st = new MonthlyStatistics(2026, 6, 5, 20, 3, 0.15, 17, 3,
                Map.of("LPPC", new MonthlyStatistics.AreaStats(12, 2)), List.of());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MonthlyStatisticsReport(st).generate(new OpenPdfReportRenderer(), out);
        assertEquals("%PDF", new String(out.toByteArray(), 0, 4));
    }

    @Test
    void fileNameIncludesMonthAndYear() {
        final MonthlyStatistics st = new MonthlyStatistics(2026, 6, 0, 0, 0, 0, 0, 0, Map.of(), List.of());
        assertEquals("monthly_statistics_2026_06.pdf", new MonthlyStatisticsReport(st).fileName());
    }

    @Test
    void noDataStillRenders() {
        final MonthlyStatistics st = new MonthlyStatistics(2025, 1, 0, 0, 0, 0, 0, 0, Map.of(), List.of());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MonthlyStatisticsReport(st).generate(new OpenPdfReportRenderer(), out);
        assertTrue(out.toByteArray().length > 0);
    }
}
