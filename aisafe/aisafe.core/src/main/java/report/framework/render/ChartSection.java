package report.framework.render;

import com.lowagie.text.*;
import report.framework.ReportContext;
import report.framework.ReportSection;

public class ChartSection implements ReportSection {
    private final byte[] pngBytes;
    public ChartSection(final byte[] pngBytes) { this.pngBytes = pngBytes; }
    @Override public void render(final ReportContext ctx) {
        try {
            final Image img = Image.getInstance(pngBytes);
            img.scaleToFit(520, 240);
            img.setSpacingBefore(8);
            ctx.document().add(img);
        } catch (final Exception e) { throw new IllegalStateException(e); }
    }
}
