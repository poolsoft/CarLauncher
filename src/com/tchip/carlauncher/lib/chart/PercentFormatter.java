package com.tchip.carlauncher.lib.chart;

import java.text.DecimalFormat;

/**
 * This ValueFormatter is just for convenience and simply puts a "%" sign after
 * each value. (Recommeded for PieChart)
 */
public class PercentFormatter implements ValueFormatter {

	protected DecimalFormat mFormat;

	public PercentFormatter() {
		mFormat = new DecimalFormat("###,###,##0.0");
	}

	@Override
	public String getFormattedValue(float value) {
		return mFormat.format(value) + " %";
	}
}
