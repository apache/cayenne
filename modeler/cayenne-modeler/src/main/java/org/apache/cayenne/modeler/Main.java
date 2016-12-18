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

import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.init.CayenneModelerModule;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.cayenne.project.ProjectModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Main class responsible for starting CayenneModeler.
 */
public class Main {

    private static Log logger = LogFactory.getLog(Main.class);

    protected String[] args;

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        try {
            new Main(args).launch();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Main(String[] args) {
        this.args = args;
    }

    protected void launch() {

        // TODO: use module auto-loading...
        final Injector injector = DIBootstrap.createInjector(appendModules(new ArrayList<Module>()));

        logger.info("Starting CayenneModeler.");
        logger.info("JRE v."
                + System.getProperty("java.version")
                + " at "
                + System.getProperty("java.home"));

        // init look and feel before starting any Swing classes...
        injector.getInstance(PlatformInitializer.class).initLookAndFeel();

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                Application application = injector.getInstance(Application.class);
                Application.setInstance(application);
                application.startup();

                // start initial project AFTER the app startup, as we need Application
                // preferences to be bootstrapped.

                File project = initialProjectFromArgs();
                if (project == null) {
                    project = initialProjectFromPreferences();
                }

                if (project != null) {
                    new OpenProjectAction(application).openProject(project);
                }
            }
        });

    }

    protected Collection<Module> appendModules(Collection<Module> modules) {
        // TODO: this is dirty... "CayenneModeler" is not a project name, and ServerModule is out of place inside
        // the Modeler... If we need ServerRuntime for certain operations, those should start their own stack...
        modules.add(new ServerModule("CayenneModeler"));

        modules.add(new ProjectModule());
        modules.add(new DbSyncModule());
        modules.add(new CayenneModelerModule());

        return modules;
    }

    protected File initialProjectFromPreferences() {

        Preferences autoLoadLastProject = Application.getInstance().getPreferencesNode(
                GeneralPreferences.class,
                "");

        if ((autoLoadLastProject != null)
                && autoLoadLastProject.getBoolean(
                        GeneralPreferences.AUTO_LOAD_PROJECT_PREFERENCE,
                        false)) {

            List<String> lastFiles = ModelerPreferences.getLastProjFiles();
            if (!lastFiles.isEmpty()) {
                return new File(lastFiles.get(0));
            }
        }

        return null;
    }

    protected File initialProjectFromArgs() {
        if (args != null && args.length == 1) {
            File f = new File(args[0]);

            if (f.isFile()
                    && f.getName().startsWith("cayenne")
                    && f.getName().endsWith(".xml")) {
                return f;
            }
        }

        return null;
    }
}
