package report.framework.render;

import com.lowagie.text.*;
import report.framework.ReportContext;
import report.framework.ReportSection;

public class ParagraphSection implements ReportSection {
    private final String text;
    private final boolean heading;
    public ParagraphSection(final String text, final boolean heading) { this.text = text; this.heading = heading; }
    @Override public void render(final ReportContext ctx) {
        final Font f = heading ? new Font(Font.HELVETICA, 11, Font.BOLD, report.framework.ReportBranding.PRIMARY)
                               : new Font(Font.HELVETICA, 10);
        try { final Paragraph p = new Paragraph(text, f); p.setSpacingBefore(8); p.setSpacingAfter(4); ctx.document().add(p); }
        catch (final DocumentException e) { throw new IllegalStateException(e); }
    }
}
