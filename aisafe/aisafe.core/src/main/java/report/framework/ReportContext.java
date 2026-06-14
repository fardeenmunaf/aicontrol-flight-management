package report.framework;

import com.lowagie.text.Document;

/** Handed to each section; exposes the document being built. */
public final class ReportContext {
    private final Document document;
    public ReportContext(final Document document) { this.document = document; }
    public Document document() { return document; }
}
