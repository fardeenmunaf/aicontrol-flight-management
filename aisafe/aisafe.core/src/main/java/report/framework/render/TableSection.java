package report.framework.render;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import report.framework.ReportContext;
import report.framework.ReportSection;

import java.util.List;

public class TableSection implements ReportSection {
    private final List<String> headers;
    private final List<List<String>> rows;
    public TableSection(final List<String> headers, final List<List<String>> rows) {
        this.headers = headers; this.rows = rows;
    }
    @Override public void render(final ReportContext ctx) {
        try {
            final PdfPTable t = new PdfPTable(headers.size());
            t.setWidthPercentage(100);
            t.setSpacingBefore(8);
            for (final String h : headers) {
                final PdfPCell c = new PdfPCell(
                        new Phrase(h, new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE)));
                c.setBackgroundColor(report.framework.ReportBranding.ACCENT);
                c.setPadding(4);
                t.addCell(c);
            }
            for (final List<String> row : rows) {
                for (final String cell : row) {
                    final PdfPCell c = new PdfPCell(new Phrase(cell, new Font(Font.HELVETICA, 9)));
                    c.setPadding(3);
                    t.addCell(c);
                }
            }
            ctx.document().add(t);
        } catch (final DocumentException e) { throw new IllegalStateException(e); }
    }
}
