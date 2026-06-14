package report.framework.render;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public final class ChartImageFactory {
    private ChartImageFactory() {}

    /** A simple vertical bar chart as PNG bytes. */
    public static byte[] barChartPng(final String title, final String categoryAxis,
            final String valueAxis, final Map<String, Number> data) {
        final DefaultCategoryDataset ds = new DefaultCategoryDataset();
        data.forEach((k, v) -> ds.addValue(v, valueAxis, k));
        final JFreeChart chart = ChartFactory.createBarChart(
                title, categoryAxis, valueAxis, ds, PlotOrientation.VERTICAL, false, false, false);
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, chart, 520, 240);
            return out.toByteArray();
        } catch (final Exception e) {
            throw new IllegalStateException("Chart render failed: " + e.getMessage(), e);
        }
    }
}
