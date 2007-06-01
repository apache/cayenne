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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.project.CayenneUserDir;

import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticTheme;

/**
 * Main class responsible for starting CayenneModeler.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class Main {

    private static Logger logObj = Logger.getLogger(Main.class);

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        Main main = new Main();

        // if configured, redirect all logging to the log file
        main.configureLogging();

        // check jdk version
        if (!main.checkJDKVersion()) {
            System.exit(1);
        }

        // run Mac hooks
        main.configureMacOSX();

        File projectFile = projectFileFromArgs(args);
        main.runModeler(projectFile);
    }

    protected static File projectFileFromArgs(String[] args) {
        if (args.length == 1) {
            File f = new File(args[0]);
            if (f.isDirectory()) {
                f = new File(f, Configuration.DEFAULT_DOMAIN_FILE);
            }

            if (f.isFile() && Configuration.DEFAULT_DOMAIN_FILE.equals(f.getName())) {
                return f;
            }
        }

        return null;
    }

    protected void runModeler(File projectFile) {
        logObj.info("Starting CayenneModeler.");

        // set up UI
        configureLookAndFeel();

        Application.instance = new Application(projectFile);

        // start frame and load project from EventDispatchThread...
        Runnable runnable = new Runnable() {

            public void run() {
                Application.instance.startup();
            }
        };

        SwingUtilities.invokeLater(runnable);
    }

    protected boolean checkJDKVersion() {
        try {
            Class.forName("javax.swing.SpringLayout");
            return true;
        }
        catch (Exception ex) {
            logObj.fatal("CayenneModeler requires JDK 1.4.");
            logObj.fatal("Found : '"
                    + System.getProperty("java.version")
                    + "' at "
                    + System.getProperty("java.home"));

            JOptionPane.showMessageDialog(
                    null,
                    "Unsupported JDK at "
                            + System.getProperty("java.home")
                            + ". Set JAVA_HOME to the JDK1.4 location.",
                    "Unsupported JDK Version",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    protected void configureMacOSX() {
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") < 0) {
            return;
        }

        try {
            MacOSXSetup.configureMacOSX();
        }
        catch (Exception ex) {
            // ignore... not a mac
        }
    }

    /**
     * Configures Log4J appenders to perform logging to $HOME/.cayenne/modeler.log.
     */
    protected void configureLogging() {
        // read default Cayenne log configuration
        Configuration.configureCommonLogging();

        // get preferences
        ModelerPreferences prefs = ModelerPreferences.getPreferences();

        // check whether to set up logging to a file
        boolean logfileEnabled = prefs.getBoolean(
                ModelerPreferences.EDITOR_LOGFILE_ENABLED,
                true);
        prefs.setProperty(ModelerPreferences.EDITOR_LOGFILE_ENABLED, String
                .valueOf(logfileEnabled));

        if (logfileEnabled) {
            String defaultPath = getLogFile().getPath();
            String logfilePath = prefs.getString(
                    ModelerPreferences.EDITOR_LOGFILE,
                    defaultPath);
            try {
                // use logfile from preferences or default

                File logfile = new File(logfilePath);

                if (logfile != null) {
                    if (!logfile.exists()) {
                        // create dir path first
                        File parent = logfile.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }

                        if (!logfile.createNewFile()) {
                            logObj.warn("Can't create log file, ignoring.");
                            return;
                        }
                    }

                    // remember working path
                    prefs.setProperty(ModelerPreferences.EDITOR_LOGFILE, logfilePath);

                    // replace appenders to just log to a file.
                    Logger p1 = logObj;
                    Logger p2 = null;
                    while ((p2 = (Logger) p1.getParent()) != null) {
                        p1 = p2;
                    }

                    Layout layout = new PatternLayout(
                            "CayenneModeler %-5p [%t %d{MM-dd HH:mm:ss}] %c: %m%n");
                    p1.removeAllAppenders();
                    p1.addAppender(new FileAppender(
                            layout,
                            logfile.getCanonicalPath(),
                            true));
                }
            }
            catch (IOException ioex) {
                logObj.warn("Error setting logging - " + logfilePath, ioex);
            }
        }
    }

    /**
     * Set up the UI Look & Feel according to $HOME/.cayenne/modeler.preferences
     */
    protected void configureLookAndFeel() {
        // get preferences
        ModelerPreferences prefs = ModelerPreferences.getPreferences();

        // get L&F name
        String lfName = prefs.getString(
                ModelerPreferences.EDITOR_LAFNAME,
                ModelerConstants.DEFAULT_LAF_NAME);
        // get UI theme name
        String themeName = prefs.getString(
                ModelerPreferences.EDITOR_THEMENAME,
                ModelerConstants.DEFAULT_THEME_NAME);

        try {
            // only install theme if L&F is Plastic;
            // bomb out if the L&F class cannot be found at all.
            Class lf = Class.forName(lfName);
            if (PlasticLookAndFeel.class.isAssignableFrom(lf)) {
                PlasticTheme foundTheme = themeWithName(themeName);
                if (foundTheme == null) {
                    logObj.warn("Could not set selected theme '"
                            + themeName
                            + "' - using default '"
                            + ModelerConstants.DEFAULT_THEME_NAME
                            + "'.");

                    themeName = ModelerConstants.DEFAULT_THEME_NAME;
                    foundTheme = themeWithName(themeName);
                }

                // try to configure theme
                PlasticLookAndFeel.setMyCurrentTheme(foundTheme);
            }

            // try to set set L&F
            UIManager.setLookAndFeel(lfName);
        }
        catch (Exception e) {
            logObj.warn("Could not set selected LookAndFeel '"
                    + lfName
                    + "' - using default '"
                    + ModelerConstants.DEFAULT_LAF_NAME
                    + "'.");

            // re-try with defaults
            lfName = ModelerConstants.DEFAULT_LAF_NAME;
            themeName = ModelerConstants.DEFAULT_THEME_NAME;
            PlasticTheme defaultTheme = themeWithName(themeName);
            PlasticLookAndFeel.setMyCurrentTheme(defaultTheme);

            try {
                UIManager.setLookAndFeel(lfName);
            }
            catch (Exception retry) {
                // give up, continue as-is
            }
        }
        finally {
            // remember L&F settings
            prefs.setProperty(ModelerPreferences.EDITOR_LAFNAME, UIManager
                    .getLookAndFeel()
                    .getClass()
                    .getName());

            prefs.setProperty(ModelerPreferences.EDITOR_THEMENAME, themeName);
        }
    }

    protected PlasticTheme themeWithName(String themeName) {
        List availableThemes = PlasticLookAndFeel.getInstalledThemes();
        for (Iterator i = availableThemes.iterator(); i.hasNext();) {
            PlasticTheme aTheme = (PlasticTheme) i.next();
            if (themeName.equals(aTheme.getName())) {
                return aTheme;
            }
        }
        return null;
    }

    /**
     * Returns a file correspinding to $HOME/.cayenne/modeler.log
     */
    protected File getLogFile() {
        if (!CayenneUserDir.getInstance().canWrite()) {
            return null;
        }

        return CayenneUserDir.getInstance().resolveFile("modeler.log");
    }
}