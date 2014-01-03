package ikms.ui;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

/**
 * A demonstration application showing a time series chart where you can dynamically add
 * (random) data by clicking on a button.
 *
 */
public class PerformanceMeasurementGraph {

	/** The time series data. */
	private XYSeries series;

	/** The most recent value added. */
	//private double lastValue = 100.0;

	private ChartPanel chartPanel;

	XYPlot plot;
	
	public PerformanceMeasurementGraph(final String title) {
		
		this.series = new XYSeries("");
		
		final XYSeriesCollection dataset = new XYSeriesCollection(this.series);
		final JFreeChart chart = createChart(dataset, title);

		chartPanel = new ChartPanel(chart);

		// JPanel content = new JPanel(new BorderLayout());
		//content.add(chartPanel);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 200));
		//chartPanel.
	}
	
	public void AddMarker (double theoreticalvalue, String markerstring) {
        final Marker target = new ValueMarker(theoreticalvalue);
        target.setPaint(Color.blue);
        target.setLabel(markerstring);
        target.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        target.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
        plot.clearRangeMarkers();
        plot.addRangeMarker(target);
	}

	public void AddData (double time, double value) {
		//final double factor = 0.90 + 0.2 * Math.random();
		//this.lastValue = this.lastValue * factor;
		//final Millisecond now = new Millisecond();
		//System.out.println("Now = " + now.toString());
		this.series.add(time, value);
	}
	
	public void ClearData () {
		this.series.clear();
	}

	public ChartPanel GetChartPanel () {
		return chartPanel;
	}

	private JFreeChart createChart(final XYDataset dataset, String title) {
		final JFreeChart result = ChartFactory.createXYLineChart(
				"", 
				"Time", 
				title,
				dataset, 
				PlotOrientation.VERTICAL, 
				false, 
				false,
				false
				);
		plot = result.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setAutoRange(true);
		//axis.setFixedAutoRange(60000.0);  // 60 seconds
		axis = plot.getRangeAxis();
		//axis.setRange(0.0, 200.0); 
		//axis.setAutoRange(true);
		axis.setAutoRangeMinimumSize(1);

		//axis.setFixedAutoRange(100.0);
		
		return result;
	}
}
