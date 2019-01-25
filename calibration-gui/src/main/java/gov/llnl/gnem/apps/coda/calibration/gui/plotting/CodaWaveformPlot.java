/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.VertAlignment;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickMovedState;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.VPickLine;
import llnl.gnem.core.gui.plotting.plotobject.AbstractLine;
import llnl.gnem.core.gui.plotting.plotobject.JDataRectangle;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PinnedText;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.waveform.SeriesPlot;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class CodaWaveformPlot extends SeriesPlot {

    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlot.class);

    private NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private Map<VPickLine, WaveformPick> pickLineMap = new HashMap<>();

    private WaveformClient waveformClient;

    private ParameterClient paramClient;

    private ShapeMeasurementClient shapeClient;

    private PeakVelocityClient velocityClient;

    private enum PLOT_ORDERING {
        BACKGROUND(0), NOISE_BOX(1), WAVEFORM(2), NOISE_LINE(3), SHAPE_FIT(4), MODEL_FIT(5), PICKS(6);

        private int zorder;

        private PLOT_ORDERING(int zorder) {
            this.zorder = zorder;
        }

        public int getZOrder() {
            return zorder;
        }
    }

    public CodaWaveformPlot(WaveformClient waveformClient, ShapeMeasurementClient shapeClient, ParameterClient paramClient, PeakVelocityClient velocityClient, TimeSeries... seismograms) {
        super("Time (seconds from origin)", seismograms);
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
    }

    private static final long serialVersionUID = 1L;

    private static final int FULL_LENGTH = 1000;

    private static final Color LIGHT_RED = new Color(250, 100, 100);

    private static final Color PINK_HALF_TRANS = new Color(238, 200, 200, 128);

    public void setWaveform(Waveform waveform) {
        setWaveform(waveform, null);
    }

    public void setWaveform(SyntheticCoda synth) {
        setWaveform(synth.getSourceWaveform(), synth);
    }

    public void setWaveform(Waveform waveform, SyntheticCoda synth) {
        this.clear();
        pickLineMap.clear();

        if (waveform != null && waveform.getSegment() != null && waveform.getBeginTime() != null) {
            final TimeT beginTime = new TimeT(waveform.getBeginTime());
            final float[] waveformSegment = doublesToFloats(waveform.getSegment());
            final TimeSeries rawSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
            this.addSeismogram(rawSeries);

            JSubplot subplot = this.getSubplot(rawSeries);
            subplot.setYlimits(subplot.getYaxis().getMin() - 1.0, subplot.getYaxis().getMax() + 1.0);
            Station station = waveform.getStream().getStation();
            Event event = waveform.getEvent();

            double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
            double baz = EModel.getBAZ(station.getLatitude(), station.getLongitude(), event.getLatitude(), event.getLongitude());
            String labelText = waveform.getEvent().getEventId()
                    + "_"
                    + waveform.getStream().getStation().getStationName()
                    + "_"
                    + waveform.getLowFrequency()
                    + "_"
                    + waveform.getHighFrequency()
                    + "; Distance: "
                    + dfmt4.format(distance)
                    + "km BAz: "
                    + dfmt4.format(baz)
                    + "° ";

            PinnedText legend = createLegend(labelText);
            PlotObject legendRef = subplot.AddPlotObject(legend);

            List<WaveformPick> picks = waveform.getAssociatedPicks();
            if (picks != null) {
                // 1221: Plotting throws a runtime error if
                // a pick is before/after the bounds of the
                // waveform so we need to check that
                for (WaveformPick pick : picks) {
                    double pickTime = new TimeT(waveform.getEvent().getOriginTime()).getEpochTime() + pick.getPickTimeSecFromOrigin();
                    if (pickTime >= new TimeT(waveform.getBeginTime()).getEpochTime() && pickTime <= new TimeT(waveform.getEndTime()).getEpochTime()) {
                        Collection<VPickLine> pickLines = this.addPick(pick.getPickName(), pickTime);
                        for (VPickLine pickLine : pickLines) {
                            pickLine.setDraggable(true);
                            pickLineMap.put(pickLine, pick);
                        }
                    }
                }
            }

            shapeClient.getMeasuredShape(waveform.getId()).subscribe(shape -> {
                if (shape != null && shape.getId() != null) {
                    try {
                        TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                        interpolatedSeries.interpolate(1.0);
                        float[] interpolatedData = interpolatedSeries.getData();
                        float[] fitSegment = new float[interpolatedData.length];

                        double gamma = shape.getMeasuredGamma();
                        double beta = shape.getMeasuredBeta();
                        double intercept = shape.getMeasuredIntercept();

                        int timeShift = (int) (new TimeT(shape.getMeasuredTime()).subtractD(beginTime) - 0.5);
                        for (int i = 0; i < interpolatedData.length; i++) {
                            fitSegment[i] = (float) (intercept - gamma * Math.log10(i + 1) + beta * (i + 1));
                        }

                        TimeSeries fitSeries = new TimeSeries(fitSegment, interpolatedSeries.getSamprate(), interpolatedSeries.getTime());
                        subplot.AddPlotObject(createLine(timeShift, 0.0, fitSeries, Color.GRAY), PLOT_ORDERING.SHAPE_FIT.getZOrder());
                        repaint();
                    } catch (IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            });

            paramClient.getSharedFrequencyBandParametersForFrequency(new FrequencyBand(waveform.getLowFrequency(), waveform.getHighFrequency())).subscribe(params -> {
                if (params != null) {
                    try {
                        velocityClient.getNoiseForWaveform(waveform.getId()).subscribe(measurement -> {
                            if (measurement != null && measurement.getNoiseEndSecondsFromOrigin() != 0.0) {
                                int lineLength = (int) (waveform.getSegment().length / waveform.getSampleRate()) + 10;
                                //                                int timeStart = (int) (new TimeT(measurement.getNoiseStartSecondsFromOrigin()).subtractD(beginTime) - 0.5);
                                //                                int timeEnd = (int) (new TimeT(measurement.getNoiseEndSecondsFromOrigin()).subtractD(beginTime) - 0.5);
                                //                                subplot.AddPlotObject(createRectangle(timeStart, timeEnd, -FULL_LENGTH, FULL_LENGTH, PINK_HALF_TRANS), PLOT_ORDERING.NOISE_BOX.getZOrder());
                                subplot.AddPlotObject(createFixedLine(measurement.getNoiseLevel(), lineLength, LIGHT_RED, PenStyle.DASH), PLOT_ORDERING.NOISE_LINE.getZOrder());
                                subplot.AddPlotObject(createFixedLine(measurement.getNoiseLevel() + params.getMinSnr(), lineLength, LIGHT_RED, PenStyle.SOLID), PLOT_ORDERING.NOISE_LINE.getZOrder());
                                repaint();
                            }
                        });

                        if (synth != null) {
                            TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                            float[] synthSegment = doublesToFloats(synth.getSegment());
                            TimeSeries synthSeries = new TimeSeries(synthSegment, synth.getSampleRate(), new TimeT(synth.getBeginTime()));
                            WaveformPick endPick = null;
                            for (WaveformPick p : waveform.getAssociatedPicks()) {
                                if (PICK_TYPES.F.name().equals(p.getPickType())) {
                                    endPick = p;
                                    break;
                                }
                            }
                            TimeT endTime;
                            if (endPick != null && endPick.getPickTimeSecFromOrigin() > 0 && waveform.getEvent() != null) {
                                endTime = new TimeT(waveform.getEvent().getOriginTime()).add(endPick.getPickTimeSecFromOrigin());
                            } else {
                                endTime = new TimeT(synth.getEndTime());
                            }

                            interpolatedSeries.interpolate(synthSeries.getSamprate());

                            double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance);
                            if (vr == 0.0) {
                                vr = 1.0;
                            }
                            TimeT originTime = new TimeT(event.getOriginTime());
                            TimeT startTime;
                            TimeT trimTime = originTime.add(distance / vr);
                            TimeSeries trimmedWaveform = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                            try {
                                trimmedWaveform.cutBefore(trimTime);
                                trimmedWaveform.cutAfter(trimTime.add(30.0));
                                startTime = new TimeT(trimTime.getEpochTime() + trimmedWaveform.getMaxTime()[0]);
                            } catch (IllegalArgumentException e) {
                                startTime = trimTime;
                            }

                            if (startTime.lt(endTime)) {
                                interpolatedSeries.cut(startTime, endTime);
                                synthSeries.cut(startTime, endTime);

                                TimeSeries diffSeis = interpolatedSeries.subtract(synthSeries);
                                int timeShift = (int) (startTime.subtractD(beginTime) - 0.5);
                                double median = diffSeis.getMedian();

                                subplot.DeletePlotObject(legendRef);
                                subplot.AddPlotObject(createLegend(labelText + "Shift: " + dfmt4.format(median)));
                                subplot.AddPlotObject(createLine(timeShift, median, synthSeries, Color.GREEN), PLOT_ORDERING.MODEL_FIT.getZOrder());
                                repaint();
                            }
                        }

                    } catch (IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }

                repaint();
            });
        }
    }

    private PinnedText createLegend(String text) {
        return new PinnedText(5d, 5d, text, HorizPinEdge.RIGHT, VertPinEdge.TOP, getTitle().getFontName(), getTitle().getFontSize(), Color.black, HorizAlignment.RIGHT, VertAlignment.TOP);
    }

    //Only used for Waveforms
    @Override
    public AbstractLine addLine(TimeSeries seismogram, Color lineColor) {
        Line line = new Line(0.0, seismogram.getDelta(), seismogram.getData(), 1);
        line.setColor(lineColor);
        getSubplot(seismogram).AddPlotObject(line, PLOT_ORDERING.WAVEFORM.getZOrder());
        return line;
    }

    //Only used for Picks
    @Override
    protected void addPlotObject(JSubplot subplot, PlotObject object) {
        subplot.AddPlotObject(object, PLOT_ORDERING.PICKS.getZOrder());
    }

    private PlotObject createRectangle(int timeStart, int timeEnd, int heightMin, int heightMax, Color pink) {
        JDataRectangle rect = new JDataRectangle(timeStart, heightMin, timeEnd - timeStart, heightMax - heightMin);
        rect.setFillColor(pink);
        return rect;
    }

    private PlotObject createLine(int timeShift, double valueShift, TimeSeries timeSeries, Color lineColor) {
        Line line = new Line(timeShift, timeSeries.getDelta(), SeriesMath.add(timeSeries.getData(), valueShift), 1);
        line.setColor(lineColor);
        line.setWidth(3);
        return line;
    }

    private PlotObject createFixedLine(double value, int length, Color lineColor, PenStyle penStyle) {
        float[] data = new float[length];
        Arrays.fill(data, (float) value);
        Line line = new Line(0, 1.0, data, 1);
        line.setColor(lineColor);
        line.setPenStyle(penStyle);
        line.setWidth(3);
        return line;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * llnl.gnem.core.gui.waveform.WaveformPlot#handlePickMovedState(java.lang.
     * Object)T
     */
    @Override
    protected void handlePickMovedState(Object obj) {
        super.handlePickMovedState(obj);
        PickMovedState pms = (PickMovedState) obj;
        VPickLine vpl = pms.getPickLine();

        if (vpl != null) {
            try {
                WaveformPick pick = pickLineMap.get(vpl);
                if (pick != null) {
                    pick.setPickTimeSecFromOrigin((float) (vpl.getXval() - new TimeT(pick.getWaveform().getEvent().getOriginTime()).subtractD(new TimeT(pick.getWaveform().getBeginTime()))));
                    waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                }
            } catch (ClassCastException | JsonProcessingException e) {
                log.info("Error updating Waveform, {}", e);
            }
        }
    }

    public static float[] doublesToFloats(Double[] x) {
        float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = x[i].floatValue();
        }
        return xfloats;
    }
}
