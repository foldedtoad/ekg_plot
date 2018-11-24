package com.callender.ekg;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.lang.ref.WeakReference;

public class EKGActivity extends Activity {

    XYPlot mPlot;
    Redrawer mRedrawer;
    EKGModel mModel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ekg);

        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.LTGRAY));

        mPlot = findViewById(R.id.PlotEKG);

        mModel = new EKGModel(100, 10);

        EKGFormatter mFormatter = new EKGFormatter(100);
        mFormatter.setLegendIconEnabled(false);

        mPlot.addSeries(mModel, mFormatter);
        mPlot.setRangeBoundaries(0, 9, BoundaryMode.FIXED);
        mPlot.setDomainBoundaries(0, 99, BoundaryMode.FIXED);

        mPlot.setLinesPerRangeLabel(3);

        mModel.start(new WeakReference<>(mPlot.getRenderer(AdvancedLineAndPointRenderer.class)));

        mRedrawer = new Redrawer(mPlot, 3, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        mRedrawer.finish();
    }


    public static class EKGFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;

        EKGFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;

            if (thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            }
            else {
                offset =  latestIndex - thisIndex;
            }

            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            return getLinePaint();
        }
    }


    public static class EKGModel implements XYSeries {

        private final Number[] data;
        private final long delayMs;
        private final int blipInteral;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        EKGModel(int size, int updateFreqHz) {
            data = new Number[size];
            for(int i = 0; i < data.length; i++) {
                data[i] = 0;
            }

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            // add 7 "blips" into the signal:
            blipInteral = size / 7;

            thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (keepRunning) {

                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }

                            // generate some random data:
                            if (latestIndex % blipInteral == 0) {
                                // insert a "blip" to simulate a heartbeat:
                                data[latestIndex] = (Math.random() * 5) + 3;
                            }
                            else {
                                // insert a random sample:
                                data[latestIndex] = Math.random() * 2;
                            }

                            if (latestIndex < data.length - 1) {
                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                data[latestIndex +1] = null;
                            }

                            if (rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                Thread.sleep(delayMs);
                            }
                            else {
                                keepRunning = false;
                            }
                            latestIndex++;
                        }
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            });
        }

        void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data[index];
        }

        @Override
        public String getTitle() {
            return "Signal";
        }
    }

}
