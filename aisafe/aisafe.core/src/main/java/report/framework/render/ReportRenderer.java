package report.framework.render;

import report.framework.ReportSection;
import java.io.OutputStream;
import java.util.List;

/** Renders a titled, branded document made of sections to an output stream. */
public interface ReportRenderer {
    void render(String reportType, String subtitle, List<ReportSection> sections, OutputStream out);
}
