package report.monthly;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import report.framework.render.OpenPdfReportRenderer;

import java.io.OutputStream;
import java.nio.file.*;

public class GenerateMonthlyStatisticsReportController {

    private final MonthlyStatisticsCollector collector;
    private final Path outputDir;

    public GenerateMonthlyStatisticsReportController(final MonthlyStatisticsCollector collector,
            final Path outputDir) {
        this.collector = collector;
        this.outputDir = outputDir;
    }

    /** Authorized entry point used by the UIs. */
    public Path generate(final Integer year, final Integer month) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return generateNoAuth(year, month);
    }

    /** Validation + generation without the authz gate (for unit testing). */
    Path generateNoAuth(final Integer year, final Integer month) {
        if (year == null || month == null) {
            throw new IllegalArgumentException("Month and year are required to generate the monthly report.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("The specified month is not a valid calendar month.");
        }
        final MonthlyStatistics stats = collector.collect(year, month);
        final MonthlyStatisticsReport report = new MonthlyStatisticsReport(stats);
        try {
            Files.createDirectories(outputDir);
            final Path target = outputDir.resolve(report.fileName());
            try (OutputStream out = Files.newOutputStream(target)) {
                report.generate(new OpenPdfReportRenderer(), out);
            }
            return target;
        } catch (final java.io.IOException e) {
            throw new IllegalStateException("Could not write report: " + e.getMessage(), e);
        }
    }
}
