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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.swing.control.FileChooser;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.3
 */
public class CreateTemplateFromDefaultView extends JDialog {

    protected JTextField prefix;
    protected FileChooser folderChooser;
    protected JCheckBox selectAll;
    protected JButton createButton;
    protected JButton cancelButton;
    protected JCheckBox standardServerSuperclass;
    protected JCheckBox standardServerSubclass;
    protected JCheckBox standardEmbeddableSuperclass;
    protected JCheckBox standardEmbeddableSubclass;
    protected JCheckBox standardServerDataMapSuperclass;
    protected JCheckBox standardServerDataMapSubclass;


    public CreateTemplateFromDefaultView(JDialog parent) {
        super(parent, "Create from defaults templates");

        this.selectAll = new JCheckBox("Select all");
        this.selectAll.setFont( new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
        this.standardServerSuperclass = new JCheckBox(TemplateType.ENTITY_SUPERCLASS.readableName());
        this.standardServerSubclass = new JCheckBox(TemplateType.ENTITY_SUBCLASS.readableName());
        this.standardEmbeddableSuperclass = new JCheckBox(TemplateType.EMBEDDABLE_SUPERCLASS.readableName());
        this.standardEmbeddableSubclass = new JCheckBox(TemplateType.EMBEDDABLE_SUBCLASS.readableName());
        this.standardServerDataMapSuperclass = new JCheckBox(TemplateType.DATAMAP_SUPERCLASS.readableName());
        this.standardServerDataMapSubclass = new JCheckBox(TemplateType.DATAMAP_SUBCLASS.readableName());
        this.prefix = new JTextField(150);
        this.folderChooser = new FileChooser("Select template's folder", false, true);
        this.createButton = new JButton("Create");
        this.cancelButton = new JButton("Cancel");

        buildView();
    }

    private void buildView() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "left:90dlu, 10dlu, 90dlu",
                "p, 10dlu, 4*(p, 3dlu), 10dlu, p, 10dlu, 4*(p, 3dlu)"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("Select templates for generating", cc.xyw(1, 1,3));
        builder.add(selectAll, cc.xy(1, 3));
        builder.add(standardServerSuperclass, cc.xy(1, 5));
        builder.add(standardServerSubclass, cc.xy(3, 5));
        builder.add(standardEmbeddableSuperclass, cc.xy(1, 7));
        builder.add(standardEmbeddableSubclass, cc.xy(3, 7));
        builder.add(standardServerDataMapSuperclass, cc.xy(1, 9));
        builder.add(standardServerDataMapSubclass, cc.xy(3, 9));
        builder.addSeparator("Output options",cc.xyw(1,12,3));
        builder.addLabel("Enter prefix for the generated templates", cc.xyw(1, 14,3));
        builder.add(prefix, cc.xyw(1, 16,3));
        builder.addLabel("Select output folder", cc.xy(1, 18));
        builder.add(folderChooser, cc.xyw(1, 20,3));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        getRootPane().setDefaultButton(createButton);
        buttons.add(cancelButton);
        buttons.add(createButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getCreateButton() {
        return createButton;
    }

    public FileChooser getFolderChooser() {
        return folderChooser;
    }

    public JTextField getPrefix() {
        return prefix;
    }

    public List<JCheckBox> getTemplatesCheckboxes() {
        ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
        checkBoxes.add(standardServerSuperclass);
        checkBoxes.add(standardServerSubclass);
        checkBoxes.add(standardEmbeddableSuperclass);
        checkBoxes.add(standardEmbeddableSubclass);
        checkBoxes.add(standardServerDataMapSuperclass);
        checkBoxes.add(standardServerDataMapSubclass);
        return checkBoxes;
    }

    public List<JCheckBox> getSelectedTemplates() {
        List<JCheckBox> templates = getTemplatesCheckboxes();
        templates.removeIf(checkBox -> (!checkBox.isSelected()));
        return templates;
    }

    public JCheckBox getSelectAll() {
        return selectAll;
    }
}
