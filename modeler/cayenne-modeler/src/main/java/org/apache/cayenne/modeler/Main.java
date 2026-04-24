/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.modeler.platform.PlatformInitializer;
import org.apache.cayenne.project.ProjectModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Main class responsible for starting CayenneModeler.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    protected String[] args;

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        try {
            new Main(args).launch();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Main(String[] args) {
        this.args = args;
    }

    protected void launch() {

        // TODO: use module auto-loading...
        Injector injector = DIBootstrap.createInjector(appendModules(new ArrayList<>()));

        // init look and feel before using any Swing classes...
        injector.getInstance(PlatformInitializer.class).initLookAndFeel();

        // logger should go after Look And Feel or Logger Console will be without style
        LOGGER.info("Starting CayenneModeler.");
        // TODO: ModelerLogger should process Slf4J {} substitutions in fallback "System.out" mode
        LOGGER.info("JRE v." + System.getProperty("java.version") + " at " + System.getProperty("java.home"));

        SwingUtilities.invokeLater(() -> {
            Application application = injector.getInstance(Application.class);
            Application.setInstance(application);
            application.startup(initialProjectFromArgs());
        });

    }

    protected Collection<Module> appendModules(Collection<Module> modules) {

        // TODO: this is dirty... CoreModule is out of place inside the Modeler...
        // If we need CayenneRuntime for certain operations, those should start their own stack...
        modules.add(new CoreModule());

        modules.add(new ProjectModule());
        modules.add(new DbSyncModule());
        modules.add(new ModelerModule());

        return modules;
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
