package report.framework;

/** A first-class, composable piece of a report (paragraph, table, chart, ...). */
public interface ReportSection {
    void render(ReportContext context);
}
