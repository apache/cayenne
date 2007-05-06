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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.util.ResourceLocator;

/**
 * Modeler action that imports a DataMap into a project from an arbitrary location.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class ImportDataMapAction extends CayenneAction {

    private static Logger logObj = Logger.getLogger(ImportDataMapAction.class);

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

        try {
            // configure resource locator to take absolute path
            MapLoader mapLoader = new MapLoader() {

                protected ResourceLocator configLocator() {
                    ResourceLocator locator = new ResourceLocator();
                    locator.setSkipAbsolutePath(false);
                    locator.setSkipClasspath(true);
                    locator.setSkipCurrentDirectory(true);
                    locator.setSkipHomeDirectory(true);
                    return locator;
                }
            };

            DataMap newMap = mapLoader.loadDataMap(dataMapFile.getAbsolutePath());
            DataDomain domain = getProjectController().getCurrentDataDomain();

            if (newMap.getName() != null) {
                newMap.setName(NamedObjectFactory.createName(
                        DataMap.class,
                        domain,
                        newMap.getName()));
            }
            else {
                newMap.setName(NamedObjectFactory.createName(DataMap.class, domain));
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
