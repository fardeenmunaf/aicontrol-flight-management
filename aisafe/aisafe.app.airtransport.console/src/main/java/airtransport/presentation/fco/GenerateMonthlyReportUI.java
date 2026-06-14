package airtransport.presentation.fco;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import report.monthly.GenerateMonthlyStatisticsReportController;
import report.monthly.MonthlyStatisticsCollector;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Console UI for generating the monthly statistics PDF report (FCO).
 * US -- Generate Monthly Statistics Report
 */
public class GenerateMonthlyReportUI extends AbstractUI {

    private final GenerateMonthlyStatisticsReportController controller =
            new GenerateMonthlyStatisticsReportController(
                    new MonthlyStatisticsCollector(
                            PersistenceContext.repositories().simulations(),
                            PersistenceContext.repositories().flightPlans(),
                            PersistenceContext.repositories().aircraft()),
                    Path.of("reports"));

    @Override
    protected boolean doShow() {
        System.out.println();

        final String yearStr = Console.readLine("Year (default: current year)");
        final int year;
        if (yearStr == null || yearStr.isBlank()) {
            year = LocalDate.now().getYear();
        } else {
            try {
                year = Integer.parseInt(yearStr.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid year. Please enter a numeric value.");
                return true;
            }
        }

        final String monthStr = Console.readLine("Month (1-12, default: current month)");
        final int month;
        if (monthStr == null || monthStr.isBlank()) {
            month = LocalDate.now().getMonthValue();
        } else {
            try {
                month = Integer.parseInt(monthStr.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid month. Please enter a number between 1 and 12.");
                return true;
            }
        }

        try {
            final Path reportPath = controller.generate(year, month);
            System.out.println();
            System.out.println("Report generated successfully.");
            System.out.println("File: " + reportPath.toAbsolutePath());
        } catch (final IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }

        return true;
    }

    @Override
    public String headline() {
        return "Generate Monthly Statistics Report";
    }
}
