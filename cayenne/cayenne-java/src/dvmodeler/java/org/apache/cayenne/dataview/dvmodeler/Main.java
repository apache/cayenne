/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.dataview.dvmodeler;

import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 * Main DVModeler class. Configures and starts the main application frame.
 * 
 * @author Nataliya Kholodna
 */
public class Main {

    // note that some themse (e.g. "Desert Blue") do not support Chinese and
    // Japanese chars
    public static final String DEFAULT_THEME_NAME = "Sky Bluer";
    public static final String DEFAULT_LAF_NAME = PlasticXPLookAndFeel.class.getName();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(DEFAULT_LAF_NAME);
            
            PlasticTheme foundTheme = themeWithName(DEFAULT_THEME_NAME);
            if (foundTheme != null) {
                PlasticLookAndFeel.setMyCurrentTheme(foundTheme);
            }
        }
        catch (Throwable th) {
            th.printStackTrace();
        }
   
        JFrame instance = new DVModelerFrame();

        instance.setSize(800, 600);
        instance.validate();
        instance.setLocationRelativeTo(null);
        instance.setVisible(true);
    }
    
    static PlasticTheme themeWithName(String themeName) {
        List availableThemes = PlasticLookAndFeel.getInstalledThemes();
        for (Iterator i = availableThemes.iterator(); i.hasNext();) {
            PlasticTheme aTheme = (PlasticTheme) i.next();
            if (themeName.equals(aTheme.getName())) {
                return aTheme;
            }
        }
        
        return null;
    }
}
