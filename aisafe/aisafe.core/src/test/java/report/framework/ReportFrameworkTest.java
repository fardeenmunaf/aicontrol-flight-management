package report.framework;

import org.junit.jupiter.api.Test;
import report.framework.render.OpenPdfReportRenderer;
import report.framework.render.ParagraphSection;

import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReportFrameworkTest {

    /** A second report subtype created WITHOUT touching framework classes (TC05). */
    static class DummyReport extends Report {
        @Override protected String reportType() { return "Dummy Report"; }
        @Override protected String fileNameStem() { return "dummy"; }
        @Override protected String subtitle() { return "unit test"; }
        @Override protected List<ReportSection> sections() {
            return List.of(new ParagraphSection("hello", false));
        }
    }

    @Test
    void generatesNonEmptyPdf() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DummyReport().generate(new OpenPdfReportRenderer(), out);
        final byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
