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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.IconFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import llnl.gnem.core.gui.plotting.PlotObjectClicked;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;

@Component
public class SiteController {

    private static final String X_AXIS_LABEL = "center freq";

    @FXML
    private SwingNode rawPlotSwingNode;
    private SpectralPlot rawPlot;

    @FXML
    private SwingNode pathPlotSwingNode;
    private SpectralPlot pathPlot;

    @FXML
    private SwingNode sitePlotSwingNode;
    private SpectralPlot sitePlot;

    @FXML
    private ComboBox<String> evidCombo;

    @FXML
    private TableView<ReferenceMwParameters> refEventTable;

    @FXML
    private TableView<MeasuredMwParameters> measuredEventTable;

    @FXML
    private TableView<LabeledPlotPoint> iconTable;

    @FXML
    private TableColumn<ReferenceMwParameters, String> evidCol;

    @FXML
    private TableColumn<ReferenceMwParameters, Double> mwCol;

    @FXML
    private TableColumn<ReferenceMwParameters, Double> stressDropCol;

    @FXML
    private TableColumn<MeasuredMwParameters, String> measuredEvidCol;

    @FXML
    private TableColumn<MeasuredMwParameters, Double> measuredMwCol;

    @FXML
    private TableColumn<MeasuredMwParameters, Double> measuredStressDropCol;

    @FXML
    private TableColumn<LabeledPlotPoint, ImageView> iconCol;

    @FXML
    private TableColumn<LabeledPlotPoint, String> stationCol;

    private SpectraClient spectraClient;
    private List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private ObservableList<String> evids = FXCollections.observableArrayList();

    private ReferenceEventClient referenceEventClient;
    private ObservableList<ReferenceMwParameters> referenceMwParameters = FXCollections.observableArrayList();
    private ObservableList<MeasuredMwParameters> measuredMwParameters = FXCollections.observableArrayList();

    private ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();

    private EventBus bus;

    protected Map<Point2D.Double, SpectraMeasurement> rawSymbolMap = new ConcurrentHashMap<>();
    protected Map<Point2D.Double, SpectraMeasurement> pathSymbolMap = new ConcurrentHashMap<>();
    protected Map<Point2D.Double, SpectraMeasurement> siteSymbolMap = new ConcurrentHashMap<>();

    private SymbolStyleMapFactory symbolStyleMapFactory;

    private Map<String, PlotPoint> symbolStyleMap;

    private GeoMap mapImpl;

    private IconFactory iconFactory;

    @Autowired
    private SiteController(SpectraClient spectraClient, ReferenceEventClient referenceEventClient, SymbolStyleMapFactory styleFactory, GeoMap map, IconFactory iconFactory, EventBus bus) {
        this.spectraClient = spectraClient;
        this.referenceEventClient = referenceEventClient;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = map;
        this.bus = bus;
        this.iconFactory = iconFactory;
    }

    @FXML
    public void initialize() {
        evidCombo.setItems(evids);

        SwingUtilities.invokeLater(() -> {
            rawPlot = new SpectralPlot();
            rawPlot.addPlotObjectObserver(new Observer() {

                @Override
                public void update(Observable observable, Object obj) {
                    if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
                        PlotObjectClicked poc = (PlotObjectClicked) obj;
                        PlotObject po = poc.getPlotObject();
                        if (po != null && po instanceof Symbol) {
                            SpectraMeasurement spectra = rawSymbolMap.get(new Point2D.Double(((Symbol) po).getXcenter(), ((Symbol) po).getYcenter()));
                            if (spectra != null) {
                                showWaveformPopup(spectra.getWaveform());
                            }
                        }
                    }
                }
            });
            rawPlotSwingNode.setContent(rawPlot);

            pathPlot = new SpectralPlot();
            pathPlot.addPlotObjectObserver(new Observer() {

                @Override
                public void update(Observable observable, Object obj) {
                    if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
                        PlotObjectClicked poc = (PlotObjectClicked) obj;
                        PlotObject po = poc.getPlotObject();
                        if (po != null && po instanceof Symbol) {
                            SpectraMeasurement spectra = pathSymbolMap.get(new Point2D.Double(((Symbol) po).getXcenter(), ((Symbol) po).getYcenter()));
                            if (spectra != null) {
                                showWaveformPopup(spectra.getWaveform());
                            }
                        }
                    }
                }
            });

            pathPlotSwingNode.setContent(pathPlot);

            sitePlot = new SpectralPlot();
            sitePlot.addPlotObjectObserver(new Observer() {

                @Override
                public void update(Observable observable, Object obj) {
                    if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
                        PlotObjectClicked poc = (PlotObjectClicked) obj;
                        PlotObject po = poc.getPlotObject();
                        if (po != null && po instanceof Symbol) {
                            SpectraMeasurement spectra = siteSymbolMap.get(new Point2D.Double(((Symbol) po).getXcenter(), ((Symbol) po).getYcenter()));
                            if (spectra != null) {
                                showWaveformPopup(spectra.getWaveform());
                            }
                        }
                    }
                }
            });
            sitePlotSwingNode.setContent(sitePlot);

            rawPlot.setLabels("Raw Plot", X_AXIS_LABEL, "log10(?)");
            rawPlot.setYaxisVisibility(true);
            rawPlot.setAllXlimits(0.0, 0.0);
            rawPlot.setDefaultYMin(-2.0);
            rawPlot.setDefaultYMax(7.0);

            pathPlot.setLabels("Path Corrected", X_AXIS_LABEL, "log10(?)");
            pathPlot.setYaxisVisibility(true);
            pathPlot.setAllXlimits(0.0, 0.0);
            pathPlot.setDefaultYMin(-2.0);
            pathPlot.setDefaultYMax(7.0);

            sitePlot.setLabels("Site Corrected", X_AXIS_LABEL, "log10(amplitude)");
            sitePlot.setYaxisVisibility(true);
        });

        evidCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                refreshView();
            }
        });

        evidCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(ReferenceMwParameters::getEventId).orElseGet(String::new)));
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        mwCol.setCellValueFactory(x -> Bindings.createDoubleBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(ReferenceMwParameters::getRefMw).orElseGet(() -> 0.0)).asObject());

        stressDropCol.setCellValueFactory(
                x -> Bindings.createDoubleBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(ReferenceMwParameters::getStressDropInMpa).orElseGet(() -> 0.0)).asObject());

        measuredEvidCol.setCellValueFactory(
                x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwParameters::getEventId).orElseGet(String::new)));
        measuredEvidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        measuredMwCol.setCellValueFactory(
                x -> Bindings.createDoubleBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwParameters::getMw).orElseGet(() -> 0.0)).asObject());

        measuredStressDropCol.setCellValueFactory(
                x -> Bindings.createDoubleBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwParameters::getStressDropInMpa).orElseGet(() -> 0.0)).asObject());

        iconCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(pp -> {
            ImageView imView = new ImageView(SwingFXUtils.toFXImage(
                    SymbolFactory.createSymbol(pp.getStyle(), 0, 0, 2, pp.getColor(), pp.getColor(), pp.getColor(), "", true, false, 10.0).getBufferedImage(256),
                        null));
            imView.setFitHeight(12);
            imView.setFitWidth(12);
            return imView;
        }).orElseGet(() -> new ImageView())));

        stationCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(LabeledPlotPoint::getLabel).orElseGet(String::new)));

        refEventTable.setItems(referenceMwParameters);
        measuredEventTable.setItems(measuredMwParameters);
        iconTable.setItems(stationSymbols);

        iconCol.prefWidthProperty().bind(iconTable.widthProperty().multiply(0.3));
        stationCol.prefWidthProperty().bind(iconTable.widthProperty().multiply(0.7));
    }

    private void showWaveformPopup(Waveform waveform) {
        bus.post(new WaveformSelectionEvent(waveform.getId()));
    }

    private void plotSpectra() {
        clearSpectraPlots();
        rawSymbolMap.clear();
        pathSymbolMap.clear();
        siteSymbolMap.clear();

        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            List<SpectraMeasurement> filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            rawSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getRawAtMeasurementTime));
            pathSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getPathCorrected));
            siteSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected));

            Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            Spectra theoreticalSpectra = spectraClient.getFitSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            rawPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getRawAtMeasurementTime), Boolean.TRUE);
            pathPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathCorrected), Boolean.TRUE);
            sitePlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.TRUE, referenceSpectra, theoreticalSpectra);
            mapImpl.addIcons(mapMeasurements(filteredMeasurements));
        } else {
            rawPlot.plotXYdata(toPlotPoints(spectralMeasurements, SpectraMeasurement::getRawAtMeasurementTime), Boolean.FALSE);
            pathPlot.plotXYdata(toPlotPoints(spectralMeasurements, SpectraMeasurement::getPathCorrected), Boolean.FALSE);
            sitePlot.plotXYdata(toPlotPoints(spectralMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.FALSE);
            mapImpl.addIcons(mapMeasurements(spectralMeasurements));
        }
    }

    private Collection<Icon> mapMeasurements(List<SpectraMeasurement> filteredMeasurements) {
        return filteredMeasurements.stream().map(meas -> meas.getWaveform()).filter(Objects::nonNull).flatMap(w -> {
            List<Icon> icons = new ArrayList<>();
            if (w.getStream() != null && w.getStream().getStation() != null) {
                Station station = w.getStream().getStation();
                icons.add(iconFactory.newIcon(IconTypes.TRIANGLE_UP, new Location(station.getLatitude(), station.getLongitude()), station.getStationName()));
            }
            if (w.getEvent() != null) {
                icons.add(iconFactory.newIcon(w.getEvent().getEventId(), IconTypes.CIRCLE, new Location(w.getEvent().getLatitude(), w.getEvent().getLongitude()), w.getEvent().getEventId()));
            }
            return icons.stream();
        }).collect(Collectors.toList());
    }

    private void clearSpectraPlots() {
        rawPlot.clearPlot();
        pathPlot.clearPlot();
        sitePlot.clearPlot();
    }

    private List<SpectraMeasurement> filterToEvent(String selectedItem, List<SpectraMeasurement> spectralMeasurements) {
        return spectralMeasurements.stream().filter(spec -> selectedItem.equalsIgnoreCase(spec.getWaveform().getEvent().getEventId())).collect(Collectors.toList());
    }

    private Map<Point2D.Double, SpectraMeasurement> mapSpectraToPoint(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        return spectralMeasurements.stream()
                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                   .collect(
                                           Collectors.toMap(
                                                   spectra -> new Point2D.Double(Math.log10(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency())),
                                                                                 func.apply(spectra)),
                                                       Function.identity(),
                                                       (a, b) -> b,
                                                       HashMap::new));
    }

    private List<PlotPoint> toPlotPoints(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        List<PlotPoint> list = spectralMeasurements.stream().filter(spectra -> !func.apply(spectra).equals(0.0)).map(spectra -> {
            PlotPoint pp = symbolStyleMap.get(spectra.getWaveform().getStream().getStation().getStationName());
            return new PlotPoint(Math.log10(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency())), func.apply(spectra), pp.getStyle(), pp.getColor());
        }).collect(Collectors.toList());
        return list;
    }

    private double centerFreq(Double lowFrequency, Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    private void reloadData() {
        clearSpectraPlots();

        referenceMwParameters.clear();
        measuredMwParameters.clear();
        referenceEventClient.getReferenceEvents().filter(ref -> ref.getId() != null).subscribe(ref -> referenceMwParameters.add(ref));
        referenceEventClient.getMeasuredEvents().filter(meas -> meas.getId() != null).subscribe(meas -> measuredMwParameters.add(meas));

        spectralMeasurements.clear();
        stationSymbols.clear();

        evids.clear();
        evids.add("All");

        spectralMeasurements.addAll(
                spectraClient.getMeasuredSpectra()
                             .filter(Objects::nonNull)
                             .filter(spectra -> spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getStream() != null)
                             .toStream()
                             .collect(Collectors.toList()));

        symbolStyleMap = symbolStyleMapFactory.build(spectralMeasurements, new Function<SpectraMeasurement, String>() {
            @Override
            public String apply(SpectraMeasurement t) {
                return t.getWaveform().getStream().getStation().getStationName();
            }
        });
        stationSymbols.addAll(symbolStyleMap.entrySet().stream().map(e -> new LabeledPlotPoint(e.getKey(), e.getValue())).collect(Collectors.toList()));

        evids.addAll(spectralMeasurements.stream().map(spec -> spec.getWaveform().getEvent().getEventId()).distinct().sorted(new MaybeNumericStringComparator()).collect(Collectors.toList()));
    }

    public void refreshView() {
        mapImpl.clearIcons();
        plotSpectra();
    }

    public void update() {
        reloadData();
    }
}
