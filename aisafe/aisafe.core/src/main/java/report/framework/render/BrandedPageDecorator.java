package report.framework.render;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import report.framework.ReportBranding;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Draws the shared header and footer on every page. Branding lives here, once. */
public class BrandedPageDecorator extends PdfPageEventHelper {

    private final String reportType;
    private final String subtitle;
    private final String generatedAt =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

    public BrandedPageDecorator(final String reportType, final String subtitle) {
        this.reportType = reportType;
        this.subtitle = subtitle;
    }

    @Override
    public void onEndPage(final PdfWriter writer, final Document doc) {
        final PdfContentByte cb = writer.getDirectContent();
        final Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, java.awt.Color.WHITE);
        final Font metaFont = new Font(Font.HELVETICA, 8, Font.NORMAL, ReportBranding.PRIMARY);

        // header band
        cb.setColorFill(ReportBranding.PRIMARY);
        cb.rectangle(doc.left(), doc.top() + 8, doc.right() - doc.left(), 22);
        cb.fill();
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(ReportBranding.SYSTEM_NAME + " — " + reportType, headerFont),
                doc.left() + 6, doc.top() + 13, 0);

        // meta line: subtitle + generated + page
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(subtitle, metaFont),
                doc.left(), doc.top() - 2, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("generated " + generatedAt + " · page " + writer.getPageNumber(), metaFont),
                doc.right(), doc.top() - 2, 0);

        // footer
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(ReportBranding.FOOTER_TEXT, metaFont),
                doc.left(), doc.bottom() - 12, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("page " + writer.getPageNumber(), metaFont),
                doc.right(), doc.bottom() - 12, 0);
    }
}
