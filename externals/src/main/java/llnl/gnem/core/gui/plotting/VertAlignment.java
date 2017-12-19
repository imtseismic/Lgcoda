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
package llnl.gnem.core.gui.plotting;

import java.io.Serializable;


/**
 * A type-safe enum to represent the vertical alignment of text objects.
 *
 * @author Doug Dodge
 */
public class VertAlignment implements Serializable{
    private final String name;

    private VertAlignment( String name )
    {
        this.name = name;
    }

    /**
     * Return a String description of this type.
     *
     * @return The String description
     */
    public String toString()
    {
        return name;
    }

    /**
     * Description of the Field
     */
    public final static VertAlignment BOTTOM = new VertAlignment( "bottom" );
    /**
     * Description of the Field
     */
    public final static VertAlignment CENTER = new VertAlignment( "center" );
    /**
     * Description of the Field
     */
    public final static VertAlignment TOP = new VertAlignment( "top" );

    public static VertAlignment getVertAlignment( String str )
    {
        if( str.equals( "bottom" ) )
            return BOTTOM;
        else if ( str.equals( "center" ) )
            return CENTER;
        else if( str.equals( "top" ) )
            return TOP;
        else
            throw new IllegalArgumentException( "Invalid type: " + str );

    }
}

