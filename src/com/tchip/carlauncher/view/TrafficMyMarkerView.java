
package com.tchip.carlauncher.view;

import android.content.Context;
import android.widget.TextView;

import com.tchip.carlauncher.lib.chart.CandleEntry;
import com.tchip.carlauncher.lib.chart.Entry;
import com.tchip.carlauncher.lib.chart.MarkerView;
import com.tchip.carlauncher.lib.chart.Utils;
import com.tchip.carlauncher.R;

/**
 * Custom implementation of the MarkerView.
 */
public class TrafficMyMarkerView extends MarkerView {

    private TextView tvContent;

    public TrafficMyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);

        tvContent = (TextView) findViewById(R.id.tvContent);
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, int dataSetIndex) {

        if (e instanceof CandleEntry) {

            CandleEntry ce = (CandleEntry) e;

            tvContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, true));
        } else {

            tvContent.setText("" + Utils.formatNumber(e.getVal(), 0, true));
        }
    }

    @Override
    public int getXOffset() {
        // this will center the marker-view horizontally
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset() {
        // this will cause the marker-view to be above the selected value
        return -getHeight();
    }
}
