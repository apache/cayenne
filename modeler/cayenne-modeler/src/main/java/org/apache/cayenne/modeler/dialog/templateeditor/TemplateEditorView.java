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

package org.apache.cayenne.modeler.dialog.templateeditor;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

/**
 * since 4.3
 */
public class TemplateEditorView extends JFrame {

    protected RSyntaxTextArea editingTemplatePane;
    protected RSyntaxTextArea classPreviewPane;

    protected JButton previewButton;
    protected JButton saveButton;
    protected JButton findButton;
    protected JButton findAndReplaceButton;
    protected JComboBox<String> entityComboBox;
    private JSplitPane split;
    private JToolBar toolBar;
    private JPanel topPanel;
    static final String VELOCITY_KEY = "text/velocity";


    public TemplateEditorView(List<String> entityNames) {
        this.setTitle("Template editor");
        this.editingTemplatePane = new TextEditorPane();
        this.classPreviewPane = new RSyntaxTextArea();
        initToolBoxComponents(entityNames);
        mapVelocityTokenMaker();
        buildView();
        bindGeometry();
    }

    private void initToolBoxComponents(List<String> entityNames) {
        this.saveButton = new JButton(ModelerUtil.buildIcon("icon-save.png"));
        this.saveButton.setToolTipText("Save");
        this.findButton = new JButton(ModelerUtil.buildIcon("icon-query.png"));
        this.findButton.setToolTipText("Find");
        this.findAndReplaceButton = new JButton(ModelerUtil.buildIcon("icon-find_and_replace.png"));
        this.findAndReplaceButton.setToolTipText("Find and replace");
        this.previewButton = new JButton(ModelerUtil.buildIcon("icon-edit.png"));
        this.previewButton.setToolTipText("Generate preview");
        this.entityComboBox = new JComboBox<>(entityNames.toArray(new String[0]));
        this.entityComboBox.setToolTipText("Select an entity for the test");
    }

    private void buildView() {
        initSplitPanel();
        initToolBar();
        initTopPanel();

        getRootPane().setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
    }

    private void initSplitPanel() {
        editingTemplatePane.setSyntaxEditingStyle(VELOCITY_KEY);
        editingTemplatePane.setMarkOccurrences(true);
        RTextScrollPane leftPanel = new RTextScrollPane(editingTemplatePane);

        classPreviewPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        classPreviewPane.setEnabled(false);
        RTextScrollPane rightPanel = new RTextScrollPane(classPreviewPane);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setPreferredSize(new Dimension(1200, 700));
        split.setDividerSize(6);
        split.setDividerLocation(1.0);
    }

    private void initToolBar() {
        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.add(findButton);
        toolBar.add(findAndReplaceButton);
        toolBar.addSeparator();
        toolBar.add(previewButton);
        toolBar.add(entityComboBox);
    }

    private void initTopPanel() {
        CellConstraints constraintsTop = new CellConstraints();
        PanelBuilder topPanelBuilder = new PanelBuilder(new FormLayout(
                "left:pref:grow, right:pref", "p, 3dlu, p, 3dlu, p"));
        topPanelBuilder.setDefaultDialogBorder();
        topPanelBuilder.add(toolBar, constraintsTop.xy(1, 1));
        topPanelBuilder.addSeparator("", constraintsTop.xyw(1, 3, 2));
        topPanelBuilder.addLabel("Editing  template", constraintsTop.xy(1, 5));
        topPanelBuilder.addLabel("Class preview", constraintsTop.xy(2, 5));
        topPanel = topPanelBuilder.getPanel();
    }

    private void bindGeometry() {
        ComponentGeometry geometry = new ComponentGeometry(this.getClass(), "split/divider");
        geometry.bindIntProperty(split, JSplitPane.DIVIDER_LOCATION_PROPERTY, 600);
        geometry.bind(this, 1200, 700, 0);
    }

    private void mapVelocityTokenMaker() {
        AbstractTokenMakerFactory tokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        tokenMakerFactory.putMapping(VELOCITY_KEY, VelocityTokenMaker.class.getName());
    }

    public String getSelectedEntityName() {
        Object selectedItem = entityComboBox.getSelectedItem();
        if (selectedItem != null) {
            return selectedItem.toString();
        }
        return null;
    }

    public JButton getPreviewButton() {
        return previewButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getFindButton() {
        return findButton;
    }

    public JButton getFindAndReplaceButton() {
        return findAndReplaceButton;
    }

    public String getTemplateText() {
        return editingTemplatePane.getText();
    }

    public RSyntaxTextArea getEditingTemplatePane() {
        return editingTemplatePane;
    }

    public RSyntaxTextArea getClassPreviewPane() {
        return classPreviewPane;
    }

}


