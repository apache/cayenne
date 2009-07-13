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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.PanelFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ObjAttributeInfoDialogView extends JDialog {

    /**
     * // * Browser to select path for attribute //
     */
    protected MultiColumnBrowser pathBrowser;

    protected JButton cancelButton;
    protected JButton saveButton;
    protected JButton selectPathButton;

    protected JTextField attributeName;
    protected JLabel currentPathLabel;
    protected JLabel sourceEntityLabel;
    protected JComboBox targCombo;
    
    static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    public ObjAttributeInfoDialogView() {

        // create widgets
        this.cancelButton = new JButton("Close");
        this.saveButton = new JButton("Done");
        this.selectPathButton = new JButton("Select path");
        this.attributeName = new JTextField(25);
        this.currentPathLabel = new JLabel();
        this.sourceEntityLabel = new JLabel();

        saveButton.setEnabled(false);
        cancelButton.setEnabled(true);
        selectPathButton.setEnabled(false);

        targCombo = new JComboBox();

        setTitle("ObjAttribute Inspector");
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, 300dlu, 3dlu, fill:min(120dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("ObjAttribute Information", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Attribute:", cc.xy(1, 3));
        builder.add(attributeName, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Current Db Path:", cc.xy(1, 5));
        builder.add(currentPathLabel, cc.xywh(3, 5, 5, 1));

        builder.addLabel("Source:", cc.xy(1, 7));
        builder.add(sourceEntityLabel, cc.xywh(3, 7, 1, 1));

        builder.addLabel("Target:", cc.xy(1, 9));
        builder.add(targCombo, cc.xywh(3, 9, 1, 1));

        builder.addSeparator("Mapping to DbRelationships", cc.xywh(1, 11, 5, 1));

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(selectPathButton);

        builder.add(buttonsPane, cc.xywh(1, 13, 5, 1));
        pathBrowser = new ObjAttributePathBrowser(selectPathButton, saveButton);
        pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        pathBrowser.setDefaultRenderer();
        builder.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 15, 5, 3));

        add(builder.getPanel(), BorderLayout.CENTER);
        add(PanelFactory.createButtonPanel(new JButton[] {
                saveButton, cancelButton
        }), BorderLayout.SOUTH);
    }
    

    
    
    public JComboBox getTargCombo() {
        return targCombo;
    }

    public MultiColumnBrowser getPathBrowser() {
        return pathBrowser;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getSelectPathButton() {
        return selectPathButton;
    }

    public JTextField getAttributeName() {
        return attributeName;
    }

    public JLabel getCurrentPathLabel() {
        return currentPathLabel;
    }

    public JLabel getSourceEntityLabel() {
        return sourceEntityLabel;
    }
}
