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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Worker;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;

public class LeafletMap implements GeoMap {

    private WebView webView;
    private ObservableSet<Icon> icons = FXCollections.observableSet(new HashSet<>());
    private Pane parent;
    private Set<WMSLayerDescriptor> layers = new HashSet<>();
    private AtomicBoolean mapReady = new AtomicBoolean(false);

    public LeafletMap() {
        Platform.runLater(() -> {
            webView = new WebView();
            webView.getEngine().setJavaScriptEnabled(true);
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    mapReady.set(true);
                    layers.forEach(this::addLayerToMap);
                    return;
                }
            });

            webView.getEngine().load(getClass().getResource("/leaflet/leaflet.html").toExternalForm());
            if (parent != null) {
                parent.getChildren().add(webView);
            }
        });
    }

    @Override
    public long getIconCount() {
        return icons.size();
    }

    public void attach(Pane parent) {
        if (parent != null) {
            if (this.parent != null && webView != null) {
                this.parent.getChildren().remove(webView);
            }
            this.parent = parent;
            if (webView != null) {
                parent.getChildren().add(webView);
            }
        }
    }

    @Override
    public void clearIcons() {
        icons.clear();
        clearIconLayer();
    }

    private void clearIconLayer() {
        if (mapReady.get()) {
            Platform.runLater(() -> webView.getEngine().executeScript("clearIcons();"));
        }
    }

    @Override
    public void addLayer(WMSLayerDescriptor layer) {
        if (layer != null) {
            layers.add(layer);
            if (mapReady.get()) {
                addLayerToMap(layer);
            }
        }
    }

    public void addLayerToMap(WMSLayerDescriptor layer) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();

            sb.append("layerControl.addOverlay(");
            sb.append("L.tileLayer.wms('");
            sb.append(layer.getUrl());
            sb.append("', { layers: '");
            for (int i = 0; i < layer.getLayers().size(); i++) {
                sb.append(layer.getLayers().get(i));
                if (i != layer.getLayers().size() - 1) {
                    sb.append(":");
                }
            }
            sb.append("'}),\"");
            sb.append(layer.getName());
            sb.append("\");");
            webView.getEngine().executeScript(sb.toString());
        });
    }

    @Override
    public boolean addIcon(Icon icon) {
        if (mapReady.get()) {
            addIconsToMap(Collections.singleton(icon));
        }
        return icons.add(icon);
    }

    @Override
    public boolean removeIcon(Icon icon) {
        if (mapReady.get()) {
            removeIconsFromMap(Collections.singleton(icon));
        }
        return icons.remove(icon);
    }

    @Override
    public void addIcons(Collection<Icon> icons) {
        this.icons.addAll(icons);
        if (mapReady.get()) {
            addIconsToMap(icons);
        }
    }

    @Override
    public void removeIcons(Collection<Icon> icons) {
        this.icons.removeAll(icons);
        if (mapReady.get()) {
            removeIconsFromMap(icons);
        }
    }

    private void addIconsToMap(Collection<? extends Icon> icons) {
        List<? extends Icon> iconCollection = new ArrayList<>(icons);
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            for (Icon icon : iconCollection) {
                sb.append(createJsIconRepresentation(icon));
            }
            webView.getEngine().executeScript(sb.toString());
        });
    }

    private void removeIconsFromMap(Collection<? extends Icon> icons) {
        Platform.runLater(() -> icons.forEach(icon -> {
            webView.getEngine().executeScript("removeIcon(\"" + icon.getId() + "\");");
        }));
    }

    private String createJsIconRepresentation(Icon icon) {
        //TODO: Encapsulate this better...
        StringBuilder sb = new StringBuilder();

        sb.append("if (!markers.has(\"");
        sb.append(icon.getId());
        sb.append("\")) {");

        switch (icon.getType()) {
        case TRIANGLE_UP:
            sb.append("marker = L.marker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(",");
            sb.append(icon.getLocation().getLongitude());
            sb.append("], {icon: L.icon({");
            sb.append("iconUrl: ");
            sb.append(getTriangleStyle(icon.getStyle()));
            sb.append("iconSize: [12, 12],");
            sb.append("iconAnchor: [6, 6],");
            sb.append("popupAnchor: [-3, -3]");
            sb.append("})");
            sb.append(getTriangleStyleZIndex(icon.getStyle()));
            sb.append("}).bindPopup('");
            sb.append(icon.getFriendlyName());
            sb.append("').addTo(iconGroup);");
            break;
        case LINE:
        case CIRCLE:
        case DEFAULT:
        default:
            sb.append("marker = L.circleMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(",");
            sb.append(icon.getLocation().getLongitude());
            sb.append("]");
            sb.append(getCircleStyle(icon.getStyle()));
            sb.append(")");
            sb.append(".bindPopup('");
            sb.append(icon.getFriendlyName());
            sb.append("').addTo(iconGroup);");
            break;
        }
        sb.append("marker._uid = \"");
        sb.append(icon.getId());
        sb.append("\"; markers.set(marker._uid, iconGroup.getLayerId(marker)); }");
        return sb.toString();
    }

    private String getTriangleStyleZIndex(IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = ", zIndexOffset: 800000";
            break;
        case BACKGROUND:
            jsonStyle = ", zIndexOffset: -800000";
            break;
        case DEFAULT:
        default:
            jsonStyle = "";
            break;
        }
        return jsonStyle;
    }

    private String getTriangleStyle(IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = "'images/triangle-up-focused.png',";
            break;
        case BACKGROUND:
            jsonStyle = "'images/triangle-up-background.png',";
            break;
        case DEFAULT:
        default:
            jsonStyle = "'images/triangle-up.png',";
            break;
        }
        return jsonStyle;
    }

    private String getCircleStyle(IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#ffffff', opacity: 1, fillOpacity: 1, pane: 'important-event-pane' }";
            break;
        case BACKGROUND:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#505050', opacity: 1, fillOpacity: 1 }";
            break;
        case DEFAULT:
        default:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#ff0000', opacity: 1, fillOpacity: 1 }";
            break;
        }
        return jsonStyle;
    }
}