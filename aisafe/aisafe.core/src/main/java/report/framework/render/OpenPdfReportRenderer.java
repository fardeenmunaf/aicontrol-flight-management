package report.framework.render;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import report.framework.ReportContext;
import report.framework.ReportSection;

import java.io.OutputStream;
import java.util.List;

public class OpenPdfReportRenderer implements ReportRenderer {

    @Override
    public void render(final String reportType, final String subtitle,
            final List<ReportSection> sections, final OutputStream out) {
        final Document doc = new Document(PageSize.A4, 36, 36, 64, 36);
        try {
            final PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new BrandedPageDecorator(reportType, subtitle));
            doc.open();
            final ReportContext ctx = new ReportContext(doc);
            for (final ReportSection section : sections) {
                section.render(ctx);
            }
            doc.close();
        } catch (final DocumentException e) {
            throw new IllegalStateException("Failed to create PDF writer: " + e.getMessage(), e);
        }
    }
}
