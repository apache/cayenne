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
package org.apache.cayenne.modeler.generic;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

public class GenericPlatformInitializer implements PlatformInitializer {

    private static Log logger = LogFactory.getLog(GenericPlatformInitializer.class);

    static final String DEFAULT_LAF_NAME = PlasticXPLookAndFeel.class.getName();

    // note that another theme - "Desert Blue" doesn't support Chinese and
    // Japanese chars
    static final String DEFAULT_THEME_NAME = "Sky Bluer";

    public void setupMenus(JFrame frame) {
        // noop - default menus are fine
    }

    public void initLookAndFeel() {

        PlasticTheme theme = findTheme();

        if (theme != null) {
            PlasticLookAndFeel.setCurrentTheme(theme);
        }

        try {
            UIManager.setLookAndFeel(DEFAULT_LAF_NAME);
        }
        catch (Exception e) {
            logger.warn("Error installing L&F: " + DEFAULT_LAF_NAME, e);
        }
    }

    protected PlasticTheme findTheme() {

        for (Object object : PlasticLookAndFeel.getInstalledThemes()) {
            PlasticTheme theme = (PlasticTheme) object;
            if (DEFAULT_THEME_NAME.equals(theme.getName())) {
                return theme;
            }
        }
        return null;
    }

}
