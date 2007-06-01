/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.project.CayenneUserDir;

/**
 * ModelerPreferences class supports persistent user preferences. Preferences are saved in
 * the user home directory in "<code>$HOME/.cayenne/modeler.preferences</code>" file.
 * <p>
 * <i>This class is obsolete; its users will be migrated to use preference service. </i>
 * </p>
 */
public class ModelerPreferences extends ExtendedProperties {

    private static final Logger logObj = Logger.getLogger(ModelerPreferences.class);

    /** Name of the preferences file. */
    public static final String PREFERENCES_NAME = "modeler.preferences";

    /** Name of the log file. */
    public static final String LOGFILE_NAME = "modeler.log";

    // Keys for the preference file.

    /** List of the last 4 opened project files. */
    public static final String LAST_PROJ_FILES = "Editor.lastSeveralProjectFiles";

    /** GUI layout */
    public static final String EDITOR_LAFNAME = "Editor.lookAndFeel";
    public static final String EDITOR_THEMENAME = "Editor.theme";

    /** Log file */
    public static final String EDITOR_LOGFILE_ENABLED = "Editor.logfileEnabled";
    public static final String EDITOR_LOGFILE = "Editor.logfile";

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