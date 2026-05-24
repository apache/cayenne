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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;
import org.apache.cayenne.modeler.toolkit.filechooser.FileFilters;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

/**
 * Modeler action that imports a DataMap into a project from an arbitrary
 * location.
 */
public class ImportDataMapAction extends AppAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDataMapAction.class);

    private final ConfigurationNameMapper nameMapper;

    public ImportDataMapAction(Application application, ConfigurationNameMapper nameMapper) {
        super(application, getActionName());
        this.nameMapper = nameMapper;
    }

    public static String getActionName() {
        return "Import DataMap";
    }

    public void performAction(ActionEvent e) {
        importDataMap();
    }

    protected void importDataMap() {
        File dataMapFile = selectDataMap(app.getFrame());
        if (dataMapFile == null) {
            return;
        }

        DataMap newMap;

        try {
            URL url = dataMapFile.toURI().toURL();
            DataMapLoader loader = app.getDataMapLoader();
            newMap = loader.load(new URLResource(url));

            ConfigurationNode root = getProjectSession().project().getRootNode();
            newMap.setName(NameBuilder
                    .builder(newMap, root)
                    .baseName(newMap.getName())
                    .name());

            Resource baseResource = ((DataChannelDescriptor) root).getConfigurationSource();

            if (baseResource != null) {
                Resource dataMapResource = baseResource.getRelativeResource(nameMapper.configurationLocation(newMap));
                newMap.setConfigurationSource(dataMapResource);
            }

            CreateDataMapAction.onMapCreated(app.getFrame(), getProjectSession(), newMap);
        } catch (Exception ex) {
            LOGGER.info("Error importing DataMap.", ex);
            JOptionPane.showMessageDialog(app.getFrame(), "Error reading DataMap: " + ex.getMessage(),
                    "Can't Open DataMap", JOptionPane.OK_OPTION);
        }
    }

    protected File selectDataMap(Frame f) {
        FileChooserPrefs prefs = new FileChooserPrefs(app.getPrefsManager().uiNode("importDataMap/lastDir"));
        return app.getFileChooserFactory().openFile(f, "Select DataMap", prefs, FileFilters.getDataMapFilter());
    }
}
