package report.framework;

import java.awt.Color;

/** Branding applied to every report (defined once). */
public final class ReportBranding {
    public static final String SYSTEM_NAME = "AISafe";
    public static final Color PRIMARY = new Color(0x1A, 0x2B, 0x4A);
    public static final Color ACCENT  = new Color(0x3B, 0x6D, 0xB3);
    public static final Color LIGHT   = new Color(0xEE, 0xF2, 0xF8);
    public static final String FOOTER_TEXT = "AISafe · Confidential";
    private ReportBranding() {}
}
