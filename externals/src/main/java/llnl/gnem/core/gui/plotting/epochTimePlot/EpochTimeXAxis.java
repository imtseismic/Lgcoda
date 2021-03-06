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
package llnl.gnem.core.gui.plotting.epochTimePlot;

import java.awt.Graphics;
import java.util.ArrayList;

import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.TickLabel;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.XAxis;

/**
 *
 * @author dodge
 */
public class EpochTimeXAxis extends XAxis {

    public EpochTimeXAxis(JMultiAxisPlot plot) {
        super(plot);
    }

    @Override
    protected void renderLinearTicks(Graphics g, double minIn, double maxIn) {
        double min = minIn;
        double max = maxIn;
        PlottingEpoch pe = new PlottingEpoch(minIn, maxIn);
        ArrayList<TickValue> majorTicks = pe.getTicks(getNumMinorTicks());
        TickTimeType type = TickTimeType.SECONDS;
        for (TickValue tmv : majorTicks) {
            type = tmv.getType();
            double val = min + tmv.getOffset();
            if (val >= minIn && val <= maxIn) {
                if (fullyDecorateAxis || tmv.isMajor())
                    renderTick(g, min + tmv.getOffset(), tmv.getLabel(), tmv.isMajor(), HorizAlignment.CENTER);
            }
        }
        String refString = String.format("%s (%s)", pe.getTime().toString(), type.toString());
        renderReferenceTickLabel(g, refString, pe.getTime().getEpochTime());
    }

    private void renderReferenceTickLabel(Graphics g, String refString, double time) {
        TickLabel refLabel = new TickLabel("", refString);
        TickValue tmv = new TickValue(0.0, refLabel, true, HorizAlignment.LEFT, TickTimeType.SECONDS);
        renderTick(g, time + tmv.getOffset(), tmv.getLabel(), tmv.isMajor(), tmv.getHorizAlignment());
    }

}
