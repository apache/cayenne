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

package org.apache.cayenne.modeler.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.NamedObjectFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * Modeler action that imports a DataMap into a project from an arbitrary location.
 * 
 * @since 1.1
 */
public class ImportDataMapAction extends CayenneAction {

    private static Log logObj = LogFactory.getLog(ImportDataMapAction.class);

    public static String getActionName() {
        return "Import DataMap";
    }

    public ImportDataMapAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {
        importDataMap();
    }

    protected void importDataMap() {
        File dataMapFile = selectDataMap(Application.getFrame());
        if (dataMapFile == null) {
            return;
        }

        DataMap newMap;

        try {

            URL url = dataMapFile.toURI().toURL();

            InputStream in = url.openStream();

            try {
                InputSource inSrc = new InputSource(in);
                inSrc.setSystemId(dataMapFile.getAbsolutePath());
                newMap = new MapLoader().loadDataMap(inSrc);
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException ioex) {
                }
            }

            DataChannelDescriptor domain = (DataChannelDescriptor) getProjectController()
                    .getProject()
                    .getRootNode();

            if (newMap.getName() != null) {
                newMap.setName(NamedObjectFactory.createName(
                        DataMap.class,
                        domain,
                        newMap.getName()));
            }
            else {
                newMap.setName(NamedObjectFactory.createName(DataMap.class, domain));
            }
            
            Resource baseResource = domain.getConfigurationSource();

            if (baseResource != null) {
                Resource dataMapResource = baseResource.getRelativeResource(newMap.getName());
                newMap.setConfigurationSource(dataMapResource);
            }

            getProjectController().addDataMap(this, newMap);
        }
        catch (Exception ex) {
            logObj.info("Error importing DataMap.", ex);
            JOptionPane.showMessageDialog(
                    Application.getFrame(),
                    "Error reading DataMap: " + ex.getMessage(),
                    "Can't Open DataMap",
                    JOptionPane.OK_OPTION);
        }
    }

    protected File selectDataMap(Frame f) {

        // find start directory in preferences
        FSPath lastDir = getApplication().getFrameController().getLastDirectory();

        // configure dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        lastDir.updateChooser(chooser);

        chooser.addChoosableFileFilter(FileFilters.getDataMapFilter());

        int status = chooser.showDialog(f, "Select DataMap");
        if (status == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // save to preferences...
            lastDir.updateFromChooser(chooser);

            return file;
        }

        return null;
    }
}
