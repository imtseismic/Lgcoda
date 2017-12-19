/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.plotting.plotobject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
 * Created by: dodge1
 * Date: Jun 30, 2004
 * COPYRIGHT NOTICE
 * GnemUtils Version 1.0
 * Copyright (C) 2004 Lawrence Livermore National Laboratory.
 */


/**
 * A Symbol shaped like a Cross with triangular arms.
 *
 * @author Doug Dodge
 */
public class Cross extends Symbol {
    /**
     * Constructor for the Plus object that allows all properties to be set.
     *
     * @param X        X-coordinate of the center of the symbol
     * @param Y        Y-coordinate of the center of the symbol
     * @param size     Size of the symbol in millimeters
     * @param fillC    Fill color of the symbol
     * @param edgeC    Edge color of the symbol edge
     * @param textC    Color of the text
     * @param text     Optional text associated with the symbol.
     * @param visible  Controls whether the symbol is visible.
     * @param textVis  Controls whether the text associated with the symbol is visible.
     * @param fontsize The fontsize of the associated text.
     */
    public Cross( double X, double Y, double size, Color fillC, Color edgeC, Color textC, String text, boolean visible, boolean textVis, double fontsize )
    {
        super( X, Y, size, fillC, edgeC, textC, text, visible, textVis, fontsize );
    }

    /**
     * Constructor for the Plus object that requires only location and size.
     *
     * @param X    X-coordinate of the center of the symbol
     * @param Y    Y-coordinate of the center of the symbol
     * @param size Size of the symbol in millimeters
     */
    public Cross( double X, double Y, double size )
    {
        super( X, Y, size );
    }

    public Cross()
    {
        super();
    }


    /**
     * Paint the symbol on the canvas.
     *
     * @param g The graphics context on which to do the rendering.
     * @param x the x-position of the symbol center in pixels.
     * @param y the y-position of the symbol center in pixels.
     * @param h The height/width of the symbol in pixels.
     */
    @Override
    public void PaintSymbol( Graphics g, int x, int y, int h )
    {
        float h2 = h / 2.0F;
        float h6 = h / 5F;

        Graphics2D g2d = (Graphics2D) g;
        GeneralPath cross = new GeneralPath();
        cross.moveTo( x - h2, y + h6 );
        cross.lineTo( x - h2, y - h6 );
        cross.lineTo( x, y );
        cross.lineTo( x - h6, y - h2 );
        cross.lineTo( x + h6, y - h2 );
        cross.lineTo( x, y );
        cross.lineTo( x + h2, y - h6 );
        cross.lineTo( x + h2, y + h6 );
        cross.lineTo( x, y );
        cross.lineTo( x + h6, y + h2 );
        cross.lineTo( x - h6, y + h2 );
        cross.lineTo( x, y );
        cross.lineTo( x - h2, y + h6 );

        g2d.setColor( getFillColor() );
        g2d.fill( cross );
        g2d.setColor( _EdgeColor );
        g2d.setStroke( new BasicStroke( 1.0F ) );
        g2d.draw( cross );
        addToRegion( cross );

    }

    @Override
    public String toString()
    {
        return "Cross" + super.toString();
    }
}
