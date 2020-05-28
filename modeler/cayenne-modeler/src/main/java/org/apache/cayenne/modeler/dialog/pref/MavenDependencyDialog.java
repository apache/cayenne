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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Objects;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDependencyDialog extends CayenneController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyDialog.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final ClasspathPreferences preferencesController;
    private final MavenDependencyDialogView view;

    private volatile boolean closing;

    public MavenDependencyDialog(ClasspathPreferences preferencesController) {
        this.preferencesController = preferencesController;
        Window parentView = preferencesController.getView() instanceof Window
                ? (Window) preferencesController.getView()
                : SwingUtilities.getWindowAncestor(preferencesController.getView());
        if(parentView instanceof Dialog) {
            view = new MavenDependencyDialogView((Dialog) parentView);
        } else {
            view = new MavenDependencyDialogView((Frame) parentView);
        }
        initBindings();
    }

    private void initBindings() {
        view.getDownloadButton().addActionListener(e -> loadArtifact());
        view.getCancelButton().addActionListener(e -> close());
    }

    private void loadArtifact() {
        // url template: https://repo1.maven.org/maven2/org/apache/cayenne/cayenne-server/4.2.M1/cayenne-server-4.2.M1.jar
        String groupPath = view.getGroupId().getText().replace('.', '/').trim();
        String artifactIdText = view.getArtifactId().getText().trim();
        String versionText = view.getVersion().getText().trim();

        if("".equals(groupPath)) {
            JOptionPane.showMessageDialog(view, "Empty group Id", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if("".equals(artifactIdText)) {
            JOptionPane.showMessageDialog(view, "Empty artifact Id", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if("".equals(versionText)) {
            JOptionPane.showMessageDialog(view, "Empty version", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String urlText = "https://repo1.maven.org/maven2/" + groupPath + "/"
                + artifactIdText + "/" + versionText + "/"
                + artifactIdText + "-" + versionText + ".jar";

        Application.getInstance().getFrameController().updateStatus("Loading " + urlText);

        String localPath = System.getProperty( "user.home" ) + "/.cayenne/modeler/"
                + groupPath + "/" + artifactIdText + "-" + versionText + ".jar";
        File targetFile = new File(localPath);

        view.getDownloadButton().setEnabled(false);
        new Thread(() -> download(urlText, targetFile)).start();
    }

    private void close() {
        this.closing = true;
        view.close();
    }

    public void download(String srcUrl, File dstFile) {
        if(!dstFile.getParentFile().exists()
                && !dstFile.getParentFile().mkdirs()) {
            finalizeDownload(dstFile, "Unable to create file " + dstFile, false, false);
            return;
        }

        try {
            BufferedInputStream is = new BufferedInputStream(new URL(srcUrl).openStream());
            OutputStream os = new FileOutputStream(dstFile);
            transferTo(is, os);
        } catch (FileNotFoundException fnf) {
            finalizeDownload(dstFile, "Url not found: " + srcUrl, false, false);
            return;
        } catch (Exception e) {
            LOGGER.warn("Failed to download Maven dependency " + srcUrl, e);
            finalizeDownload(dstFile, "Unable to download file " + dstFile, false, true);
            return;
        }
        finalizeDownload(dstFile, "Succesfully downloaded", true, true);
    }

    private void finalizeDownload(File dstFile, String status, boolean success, boolean shouldClose) {
        SwingUtilities.invokeLater(() -> {
            if(success) {
                preferencesController.addClasspathEntry(dstFile);
            } else {
                JOptionPane.showMessageDialog(view, status, "Error", JOptionPane.ERROR_MESSAGE);
            }

            view.getDownloadButton().setEnabled(true);
            Application.getInstance().getFrameController().updateStatus(status);

            if(shouldClose) {
                close();
            }
        });
    }

    private void transferTo(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(in);
        Objects.requireNonNull(out);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            if(closing) {
                break;
            }
        }
    }

    @Override
    public Component getView() {
        return view;
    }
}
