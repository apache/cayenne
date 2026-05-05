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

package org.apache.cayenne.modeler.ui.preferences.classpath.maven;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPrefsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
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

/**
 * Modal dialog that downloads a Maven Central artifact (group/artifact/version) and adds
 * the resulting JAR to the classpath preferences.
 */
public class MavenDependencyDialog extends AppDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyDialog.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final ClasspathPrefsPanel preferences;

    private final JTextField groupId;
    private final JTextField artifactId;
    private final JTextField version;
    private final JButton downloadButton;
    private final JButton cancelButton;

    private volatile boolean closing;

    public MavenDependencyDialog(Application app, Window owner, ClasspathPrefsPanel preferences) {
        super(app, owner, "Download artifact", ModalityType.APPLICATION_MODAL);
        this.preferences = preferences;

        this.groupId = new JTextField(25);
        this.artifactId = new JTextField(25);
        this.version = new JTextField(25);
        this.downloadButton = new JButton("Download");
        this.cancelButton = new JButton("Cancel");

        initLayout();
        initBindings();
        pack();
        centerOnOwner();
        makeCloseableOnEscape();
    }

    private void initLayout() {
        getContentPane().setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:min(100dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu"));
        builder.setDefaultDialogBorder();

        builder.addLabel("group id:", cc.xy(1, 1));
        builder.add(groupId, cc.xy(3, 1));

        builder.addLabel("artifact id:", cc.xy(1, 3));
        builder.add(artifactId, cc.xy(3, 3));

        builder.addLabel("version:", cc.xy(1, 5));
        builder.add(version, cc.xy(3, 5));

        getContentPane().add(builder.getPanel(), BorderLayout.NORTH);

        getRootPane().setDefaultButton(downloadButton);
        getContentPane().add(new CMButtonPanel(cancelButton, downloadButton), BorderLayout.SOUTH);
    }

    private void initBindings() {
        downloadButton.addActionListener(e -> downloadClicked(
                groupId.getText().trim(),
                artifactId.getText().trim(),
                version.getText().trim()));
        cancelButton.addActionListener(e -> close());
    }

    private void downloadClicked(String groupId, String artifactId, String version) {
        // url template: https://repo1.maven.org/maven2/org/apache/cayenne/cayenne-server/4.2.M1/cayenne-server-4.2.M1.jar
        String groupPath = groupId.replace('.', '/');

        if (groupPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empty group Id", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (artifactId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empty artifact Id", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (version.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empty version", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String urlText = "https://repo1.maven.org/maven2/" + groupPath + "/"
                + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".jar";

        app.getFrame().updateStatus("Loading " + urlText);

        String localPath = System.getProperty("user.home") + "/.cayenne/modeler/"
                + groupPath + "/" + artifactId + "-" + version + ".jar";
        File targetFile = new File(localPath);

        downloadButton.setEnabled(false);
        new Thread(() -> download(urlText, targetFile)).start();
    }

    private void close() {
        closing = true;
        setVisible(false);
        dispose();
    }

    private void download(String srcUrl, File dstFile) {
        if (!dstFile.getParentFile().exists() && !dstFile.getParentFile().mkdirs()) {
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
        finalizeDownload(dstFile, "Successfully downloaded", true, true);
    }

    private void finalizeDownload(File dstFile, String status, boolean success, boolean shouldClose) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                preferences.entryAdded(dstFile);
            } else {
                JOptionPane.showMessageDialog(this, status, "Error", JOptionPane.ERROR_MESSAGE);
            }

            downloadButton.setEnabled(true);
            app.getFrame().updateStatus(status);

            if (shouldClose) {
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
            if (closing) {
                break;
            }
        }
    }
}
