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

import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShapeFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Component
public class MapPlottingUtilities {
    private GeoShapeFactory geoShapeFactory;

    public MapPlottingUtilities(GeoShapeFactory geoShapeFactory) {
        super();
        this.geoShapeFactory = geoShapeFactory;
    }

    public Icon createEventIcon(Event event) {
        return createEventIcon(event.getEventId(), event, IconStyles.DEFAULT);
    }

    public Icon createEventIconForeground(Event event) {
        return createEventIcon(Icon.FOCUS_TAG + event.getEventId(), event, IconStyles.FOCUSED);
    }

    public Icon createEventIconBackground(Event event) {
        return createEventIcon(null, event, IconStyles.BACKGROUND);
    }

    private Icon createEventIcon(String id, Event event, IconStyles style) {
        if (id != null) {
            return geoShapeFactory.newIcon(id, IconTypes.CIRCLE, new Location(event.getLatitude(), event.getLongitude()), event.getEventId(), style);
        } else {
            return geoShapeFactory.newIcon(IconTypes.CIRCLE, new Location(event.getLatitude(), event.getLongitude()), event.getEventId(), style);
        }
    }

    public Icon createStationIconForeground(Station station) {
        return createStationIcon(Icon.FOCUS_TAG + station.getStationName(), station, IconStyles.FOCUSED);
    }

    public Icon createStationIconBackground(Station station) {
        return createStationIcon(null, station, IconStyles.BACKGROUND);
    }

    public Icon createStationIcon(Station station) {
        return createStationIcon(station.getStationName(), station, IconStyles.DEFAULT);
    }

    private Icon createStationIcon(String id, Station station, IconStyles style) {
        if (id != null) {
            return geoShapeFactory.newIcon(id, IconTypes.TRIANGLE_UP, new Location(station.getLatitude(), station.getLongitude()), station.getStationName(), style);
        } else {
            return geoShapeFactory.newIcon(IconTypes.TRIANGLE_UP, new Location(station.getLatitude(), station.getLongitude()), station.getStationName(), style);
        }
    }

    public GeoShape createStationToEventLine(Station station, Event event) {
        return geoShapeFactory.newLine(new Location(station.getLatitude(), station.getLongitude()), new Location(event.getLatitude(), event.getLongitude()));
    }
}
