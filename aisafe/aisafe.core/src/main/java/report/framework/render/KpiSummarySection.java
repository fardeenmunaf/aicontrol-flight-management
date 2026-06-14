package report.framework.render;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import report.framework.ReportContext;
import report.framework.ReportSection;

import java.util.List;

public class KpiSummarySection implements ReportSection {
    public record Kpi(String label, String value) {}
    private final List<Kpi> kpis;
    public KpiSummarySection(final List<Kpi> kpis) { this.kpis = kpis; }
    @Override public void render(final ReportContext ctx) {
        try {
            final PdfPTable t = new PdfPTable(kpis.size());
            t.setWidthPercentage(100);
            t.setSpacingBefore(6);
            for (final Kpi k : kpis) {
                final PdfPCell cell = new PdfPCell(
                        new Phrase(k.value(), new Font(Font.HELVETICA, 14, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(report.framework.ReportBranding.LIGHT);
                cell.setPadding(6);
                t.addCell(cell);
            }
            for (final Kpi k : kpis) {
                final PdfPCell cell = new PdfPCell(
                        new Phrase(k.label(), new Font(Font.HELVETICA, 8)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4);
                t.addCell(cell);
            }
            ctx.document().add(t);
        } catch (final DocumentException e) { throw new IllegalStateException(e); }
    }
}
