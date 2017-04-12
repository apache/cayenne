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
package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.graph.DataDomainGraphTab;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.FileFilters;
import org.slf4j.Logger;
import org.jgraph.JGraph;
import org.slf4j.LoggerFactory;

/**
 * Action for saving graph as image
 */
public class SaveAsImageAction extends CayenneAction {
	private static final Logger logObj = LoggerFactory.getLogger(SaveAsImageAction.class);

	private final DataDomainGraphTab dataDomainGraphTab;

	public SaveAsImageAction(DataDomainGraphTab dataDomainGraphTab, Application application) {
		super("Save As Image", application);
		this.dataDomainGraphTab = dataDomainGraphTab;
		setEnabled(true);
	}

	@Override
	public String getIconName() {
		return "icon-save-as-image.png";
	}

	@Override
	public void performAction(ActionEvent e) {
		// find start directory in preferences
		FSPath lastDir = getApplication().getFrameController().getLastDirectory();

		// configure dialog
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		lastDir.updateChooser(chooser);

		chooser.setAcceptAllFileFilterUsed(false);

		String ext = "png";
		chooser.addChoosableFileFilter(FileFilters.getExtensionFileFilter(ext, "PNG Images"));

		int status = chooser.showSaveDialog(Application.getFrame());
		if (status == JFileChooser.APPROVE_OPTION) {
			lastDir.updateFromChooser(chooser);

			String path = chooser.getSelectedFile().getPath();
			if (!path.endsWith("." + ext)) {
				path += "." + ext;
			}

			try {

				JGraph graph = dataDomainGraphTab.getGraph();
				BufferedImage img = graph.getImage(null, 0);

				try (OutputStream out = new FileOutputStream(path);) {
					ImageIO.write(img, ext, out);
					out.flush();
				}

			} catch (IOException ex) {
				logObj.error("Could not save image", ex);
				JOptionPane.showMessageDialog(Application.getFrame(), "Could not save image.", "Error saving image",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}