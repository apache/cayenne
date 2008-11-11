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

package org.apache.cayenne.modeler;

import java.io.File;

/**
 * Main class to start CayenneModeler on MacOSX.
 * 
 * @since 1.2
 */
public class MacOSXMain extends Main {

    public static final String DEFAULT_LAF_OSX_NAME = "apple.laf.AquaLookAndFeel";
    public static final String DEFAULT_THEME_OSX_NAME = "Aqua";

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        MacOSXMain main = new MacOSXMain();

        // if configured, redirect all logging to the log file
        main.configureLogging();

        // check jdk version
        if (!main.checkJDKVersion()) {
            System.exit(1);
        }

        File projectFile = Main.projectFileFromArgs(args);
        main.runModeler(projectFile);
    }

    protected static boolean isMacOSX() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
    }

    protected void runModeler(File projectFile) {
        configureMacOSX();
        super.runModeler(projectFile);
    }

    protected void configureMacOSX() {
        try {
            MacOSXSetup.configureMacOSX();
        }
        catch (Exception ex) {
            // ignore... not a mac
        }
    }

    protected String getLookAndFeelName() {

        if (isMacOSX()) {
            ModelerPreferences prefs = ModelerPreferences.getPreferences();
            return prefs.getString(
                    ModelerPreferences.EDITOR_LAFNAME,
                    MacOSXMain.DEFAULT_LAF_OSX_NAME);
        }
        else
            return super.getLookAndFeelName();
    }

    protected String getThemeName() {
        if (isMacOSX()) {
            ModelerPreferences prefs = ModelerPreferences.getPreferences();
            return prefs.getString(
                    ModelerPreferences.EDITOR_THEMENAME,
                    MacOSXMain.DEFAULT_THEME_OSX_NAME);
        }
        else {
            return super.getThemeName();
        }
    }
}
