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
package org.objectstyle.cayenne.modeler.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.util.ResourceLocator;

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
        catch (DataMapException ex) {
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