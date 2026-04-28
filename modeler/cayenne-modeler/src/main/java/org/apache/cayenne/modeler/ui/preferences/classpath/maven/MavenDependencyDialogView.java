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
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.dialog.CMDialog;

import javax.swing.*;
import java.awt.*;

public class MavenDependencyDialogView extends CMDialog {

    private JButton downloadButton;
    private JButton cancelButton;
    private JTextField groupId;
    private JTextField artifactId;
    private JTextField version;

    public MavenDependencyDialogView(Dialog parentDialog) {
        super(parentDialog, "Download artifact", true);
        this.initView();
        this.pack();
        centerWindow();
    }

    public MavenDependencyDialogView(Frame parentFrame) {
        super(parentFrame, "Download artifact", true);
        this.initView();
        this.pack();
        centerWindow();
    }

    private void initView() {
        getContentPane().setLayout(new BorderLayout());

        {
            groupId = new JTextField(25);
            artifactId = new JTextField(25);
            version = new JTextField(25);

            CellConstraints cc = new CellConstraints();
            PanelBuilder builder = new PanelBuilder(
                    new FormLayout(
                            "right:max(50dlu;pref), 3dlu, fill:min(100dlu;pref)",
                            "p, 3dlu, p, 3dlu, p, 3dlu"
                    ));
            builder.setDefaultDialogBorder();

            builder.addLabel("group id:", cc.xy(1, 1));
            builder.add(groupId, cc.xy(3, 1));

            builder.addLabel("artifact id:", cc.xy(1, 3));
            builder.add(artifactId, cc.xy(3, 3));

            builder.addLabel("version:", cc.xy(1, 5));
            builder.add(version, cc.xy(3, 5));

            getContentPane().add(builder.getPanel(), BorderLayout.NORTH);
        }

        {
            downloadButton = new JButton("Download");
            cancelButton = new JButton("Cancel");
            getRootPane().setDefaultButton(downloadButton);

            getContentPane().add(new CMButtonPanel(cancelButton, downloadButton), BorderLayout.SOUTH);
        }
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getDownloadButton() {
        return downloadButton;
    }

    public JTextField getArtifactId() {
        return artifactId;
    }

    public JTextField getGroupId() {
        return groupId;
    }

    public JTextField getVersion() {
        return version;
    }
}