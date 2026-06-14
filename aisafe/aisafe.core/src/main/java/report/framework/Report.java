package report.framework;

import report.framework.render.ReportRenderer;

import java.io.OutputStream;
import java.util.List;

/** Template-method base for all reports. Branding/structure fixed; subclasses supply content. */
public abstract class Report {

    /** Fixed skeleton — subclasses cannot change ordering or branding. */
    public final void generate(final ReportRenderer renderer, final OutputStream out) {
        renderer.render(reportType(), subtitle(), sections(), out);
    }

    protected abstract String reportType();
    protected abstract String subtitle();
    protected abstract String fileNameStem();
    protected abstract List<ReportSection> sections();

    /** Default file name; concrete reports may append period info via fileNameStem(). */
    public String fileName() {
        return fileNameStem() + ".pdf";
    }
}
