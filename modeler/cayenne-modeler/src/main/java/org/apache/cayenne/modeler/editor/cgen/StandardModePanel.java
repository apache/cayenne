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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

/**
 * @since 4.1
 */
public class StandardModePanel extends GeneratorControllerPanel {

    private static final int ALL_LINE_SPAN = 5;
    private static final String MAIN_LAYOUT_CS = "right:58dlu, 1dlu, 3dlu, 1dlu, left:100dlu";
    private static final String CHECK_BOX_LAYOUT_CS = "right:pref, 3dlu, 80dlu";
    private static final String DATA_FIELDS_LAYOUT_CS = "118dlu, 3dlu, 80dlu";
    private static final String OUTPUT_FOLDER_LAYOUT_CS = "fill:303dlu, 1dlu, 20dlu";
    private static final ImageIcon rightArrow = ModelerUtil.buildIcon("icon-arrow-closed.png");
    private static final ImageIcon downArrow = ModelerUtil.buildIcon("icon-arrow-open.png");
    private final JCheckBox pairs;
    private final JCheckBox overwrite;
    private final JCheckBox usePackagePath;
    private final JCheckBox createPropertyNames;
    private final JCheckBox pkProperties;
    protected final JCheckBox clientMode;
    private final TextAdapter superPkg;
    protected final TextAdapter outputPattern;
    protected JPanel checkBoxPanel;
    protected JPanel dataFieldsPanel;
    protected DefaultFormBuilder checkBoxBuilder;
    protected DefaultFormBuilder dataFieldsBuilder;


    public StandardModePanel(CodeGeneratorController codeGeneratorController) {
        super(Application.getInstance().getFrameController().getProjectController(), codeGeneratorController);
        this.codeGeneratorController = codeGeneratorController;
        this.pairs = new JCayenneCheckBox();
        this.overwrite = new JCayenneCheckBox();
        this.usePackagePath = new JCayenneCheckBox();
        this.createPropertyNames = new JCayenneCheckBox();
        this.pkProperties = new JCayenneCheckBox();
        this.clientMode = new JCayenneCheckBox();

        JTextField superPkgField = new JTextField();
        this.superPkg = new TextAdapter(superPkgField) {
            @Override
            protected void updateModel(String text) throws ValidationException {
                getCgenConfig().setSuperPkg(text);
                checkConfigDirty();
            }
        };

        JTextField outputPatternField = new JTextField();
        this.outputPattern = new TextAdapter(outputPatternField) {
            protected void updateModel(String text) {
                getCgenConfig().setOutputPattern(text);
                checkConfigDirty();
            }
        };
        buildView();
    }

    protected void buildView() {
        FormLayout outputFolderLayout = new FormLayout(OUTPUT_FOLDER_LAYOUT_CS, "");
        DefaultFormBuilder outputFolderBuilder = new DefaultFormBuilder(outputFolderLayout);
        outputFolderBuilder.setDefaultDialogBorder();

        outputFolderBuilder.append("Output Directory");
        outputFolderBuilder.nextLine();
        outputFolderBuilder.append(outputFolder.getComponent(), selectOutputFolder);
        outputFolderBuilder.nextLine();

        FormLayout checkBoxLayout = new FormLayout(CHECK_BOX_LAYOUT_CS);
        this.checkBoxBuilder = new DefaultFormBuilder(checkBoxLayout);
        checkBoxBuilder.setDefaultDialogBorder();

        FormLayout dataFieldsLayout = new FormLayout(DATA_FIELDS_LAYOUT_CS);
        this.dataFieldsBuilder = new DefaultFormBuilder(dataFieldsLayout);
        dataFieldsBuilder.setDefaultDialogBorder();

        checkBoxBuilder.append(pairs);
        checkBoxBuilder.append("Make Pairs");
        checkBoxBuilder.nextLine();

        checkBoxBuilder.append(usePackagePath);
        checkBoxBuilder.append("Use Package Path");
        checkBoxBuilder.nextLine();

        checkBoxBuilder.append(overwrite);
        checkBoxBuilder.append("Overwrite Subclasses");
        checkBoxBuilder.nextLine();

        checkBoxBuilder.append(createPropertyNames);
        checkBoxBuilder.append("Create Property Names");
        checkBoxBuilder.nextLine();

        checkBoxBuilder.append(pkProperties);
        checkBoxBuilder.append("Create PK properties:");
        checkBoxBuilder.nextLine();
        addCustomModeFields();

        dataFieldsBuilder.append(" Superclass package");
        dataFieldsBuilder.nextLine();
        dataFieldsBuilder.append(superPkg.getComponent());

        this.checkBoxPanel = checkBoxBuilder.getPanel();
        this.dataFieldsPanel = dataFieldsBuilder.getPanel();
        checkBoxPanel.setVisible(false);
        dataFieldsPanel.setVisible(false);

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(MAIN_LAYOUT_CS, ""));
        builder.append(createAdvancedOptionsHiderPanel(), ALL_LINE_SPAN);
        builder.append(checkBoxPanel, ALL_LINE_SPAN);
        builder.append(dataFieldsPanel, ALL_LINE_SPAN);
        setLayout(new BorderLayout());
        add(outputFolderBuilder.getPanel(), BorderLayout.NORTH);
        add(builder.getPanel(), BorderLayout.WEST);

        addTemplatePanel();
    }

    protected void addCustomModeFields() {//noop
    }

    protected void addTemplatePanel() { //noop
    }

    private JPanel createAdvancedOptionsHiderPanel() {
        JPanel advancedOptionsPanel = new JPanel();
        advancedOptionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton hideButton = new JButton("Advanced Options");
        hideButton.setFont(hideButton.getFont().deriveFont(12f));
        hideButton.setIcon(checkBoxPanel.isVisible() ? downArrow : rightArrow);
        hideButton.setBorderPainted(false);
        hideButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkBoxPanel.setVisible(!checkBoxPanel.isVisible());
                dataFieldsPanel.setVisible(!dataFieldsPanel.isVisible());
                hideButton.setIcon(checkBoxPanel.isVisible() ? downArrow : rightArrow);
            }
        });
        advancedOptionsPanel.add(hideButton);
        return advancedOptionsPanel;

    }


    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getOverwrite() {
        return overwrite;
    }

    public JCheckBox getUsePackagePath() {
        return usePackagePath;
    }

    public JCheckBox getCreatePropertyNames() {
        return createPropertyNames;
    }

    public JCheckBox getPkProperties() {
        return pkProperties;
    }

    public JCheckBox getClientMode() {
        return clientMode;
    }

    public TextAdapter getSuperPkg() {
        return superPkg;
    }

    public TextAdapter getOutputPattern() {
        return outputPattern;
    }



}