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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cayenne.project.CayenneUserDir;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ModelerPreferences class supports persistent user preferences. Preferences are saved in
 * the user home directory in "<code>$HOME/.cayenne/modeler.preferences</code>" file.
 * <p>
 * <i>This class is obsolete; its users will be migrated to use preference service. </i>
 * </p>
 */
public class ModelerPreferences extends ExtendedProperties {

    private static final Log logObj = LogFactory.getLog(ModelerPreferences.class);

    /** Name of the preferences file. */
    public static final String PREFERENCES_NAME = "modeler.preferences";

    /** Name of the log file. */
    public static final String LOGFILE_NAME = "modeler.log";

    // Keys for the preference file.

    /** List of the last 12 opened project files. */
    public static final String LAST_PROJ_FILES = "Editor.lastSeveralProjectFiles";
    public static final int LAST_PROJ_FILES_SIZE = 12;

    /** GUI layout */
    public static final String EDITOR_LAFNAME = "Editor.lookAndFeel";
    public static final String EDITOR_THEMENAME = "Editor.theme";

    /** Log file */
    public static final String EDITOR_LOGFILE_ENABLED = "Editor.logfileEnabled";
    public static final String EDITOR_LOGFILE = "Editor.logfile";
    
    /*
     * Number of items in combobox visible without scrolling 
     */
    public static final int COMBOBOX_MAX_VISIBLE_SIZE = 12;
    
    protected static ModelerPreferences sharedInstance;

    protected ModelerPreferences() {
    }

    /**
     * Returns Cayenne preferences singleton.
     */
    public static ModelerPreferences getPreferences() {
        if (sharedInstance == null) {
            sharedInstance = new ModelerPreferences();
            sharedInstance.loadPreferences();
        }

        return sharedInstance;
    }

    /**
     * Returns preferences directory <code>$HOME/.cayenne</code>. If such directory
     * does not exist, it is created as a side effect of this method.
     */
    public File preferencesDirectory() {
        return CayenneUserDir.getInstance().getDirectory();
    }

    /**
     * Saves preferences. Preferences stored in
     * <code>$HOME/.cayenne/modeler.preferences</code> file.
     */
    public void storePreferences() {
        File prefFile = new File(preferencesDirectory(), PREFERENCES_NAME);
        try {
            if (!prefFile.exists()) {
                logObj.debug("Cannot save preferences - file "
                        + prefFile
                        + " does not exist");
                return;
            }
            save(new FileOutputStream(prefFile), "");
        }
        catch (IOException e) {
            logObj.debug("Error saving preferences: ", e);
        }
    }

    /**
     * Loads preferences from <code>$HOME/.cayenne/modeler.preferences</code> file.
     */
    public void loadPreferences() {
        try {
            File prefsFile = new File(preferencesDirectory(), PREFERENCES_NAME);
            if (!prefsFile.exists()) {
                if (!prefsFile.createNewFile()) {
                    logObj.warn("Can't create preferences file " + prefsFile);
                }
            }

            load(new FileInputStream(prefsFile));
        }
        catch (IOException e) {
            logObj.warn("Error creating preferences file.", e);
        }
    }

}
