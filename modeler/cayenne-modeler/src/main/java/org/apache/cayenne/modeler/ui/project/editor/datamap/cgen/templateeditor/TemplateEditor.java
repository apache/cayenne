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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.templateeditor;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.component.CMComponentGeometryPrefs;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.splitpane.CMSplitPanePrefs;
import org.apache.cayenne.modeler.toolkit.AppFrame;
import org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.CgenConfigPanel;
import org.apache.velocity.exception.ParseErrorException;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Writer;

public class TemplateEditor extends AppFrame {

    private static final String VELOCITY_KEY = "text/velocity";
    private static final ImageIcon modelerIcon = IconFactory.buildIcon("CayenneModeler.png");
    private static final Icon saveIcon = IconFactory.buildIcon("icon-save.png");
    private static final Icon resetToDefaultIcon = IconFactory.buildIcon("icon-undo.png");
    private static final Icon findIcon = IconFactory.buildIcon("icon-query.png");
    private static final Icon findAndReplaceIcon = IconFactory.buildIcon("icon-find_and_replace.png");
    private static final Icon previewIcon = IconFactory.buildIcon("icon-edit.png");

    private final CgenConfigPanel parent;
    private final TemplateType templateType;
    private final DataMap currentDataMap;
    private final CgenConfiguration configuration;
    private final ArtefactsConfigurator artefactsConfigurator;

    private final RSyntaxTextArea editingTemplatePane;
    private final RSyntaxTextArea classPreviewPane;
    private final JButton previewButton;
    private final JButton saveButton;
    private final JButton findButton;
    private final JButton findAndReplaceButton;
    private final JButton resetToDefaultButton;
    private final JComboBox<String> entityComboBox;
    private final JSplitPane split;

    private final EditorTemplateLoader templateLoader;
    private final EditorTemplateSaver templateSaver;
    private final PreviewActionConfigurator actionConfigurator;

    private boolean isTemplateModified;
    private boolean isTemplateDefault;

    public TemplateEditor(Application app, CgenConfigPanel parent, TemplateType templateType) {
        super(app);
        this.parent = parent;
        this.templateType = templateType;
        this.configuration = parent.getCodeGeneratorController().getCgenConfiguration();
        this.currentDataMap = configuration.getDataMap();
        this.artefactsConfigurator = setupArtefactConfigurator(templateType);

        this.editingTemplatePane = new TextEditorPane();
        this.classPreviewPane = new RSyntaxTextArea();
        this.saveButton = new JButton(saveIcon);
        this.resetToDefaultButton = new JButton(resetToDefaultIcon);
        this.findButton = new JButton(findIcon);
        this.findAndReplaceButton = new JButton(findAndReplaceIcon);
        this.previewButton = new JButton(previewIcon);
        this.entityComboBox = new JComboBox<>(
                artefactsConfigurator.getArtifactsNames(currentDataMap).toArray(new String[0]));
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        this.isTemplateDefault = TemplateType.isDefault(configuration.getTemplateByType(templateType).getData());
        this.isTemplateModified = false;
        this.templateLoader = new EditorTemplateLoader(configuration, this);
        this.templateSaver = new EditorTemplateSaver(configuration);
        this.actionConfigurator = new PreviewActionConfigurator(this);

        setIconImage(modelerIcon.getImage());
        mapVelocityTokenMaker();
        initLayout();
        initBindings();
        configureEditorView();
        bindGeometry();
    }

    public void open() {
        centerOnParent();
        setVisible(true);
    }

    public Application app() {
        return app;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public DataMap getCurrentDataMap() {
        return currentDataMap;
    }

    public CgenConfiguration getConfiguration() {
        return configuration;
    }

    public ArtefactsConfigurator getArtefactsConfigurator() {
        return artefactsConfigurator;
    }

    public String getSelectedArtifactName() {
        Object selectedItem = entityComboBox.getSelectedItem();
        return selectedItem != null ? selectedItem.toString() : null;
    }

    public String getTemplateText() {
        return editingTemplatePane.getText();
    }

    public RSyntaxTextArea getEditingTemplatePane() {
        return editingTemplatePane;
    }

    private ArtefactsConfigurator setupArtefactConfigurator(TemplateType type) {
        switch (type) {
            case ENTITY_SUPERCLASS:
            case ENTITY_SUBCLASS:
                return new EntityArtefactsConfigurator();
            case EMBEDDABLE_SUPERCLASS:
            case EMBEDDABLE_SUBCLASS:
                return new EmbeddableArtefactsConfigurator();
            case DATAMAP_SUPERCLASS:
            case DATAMAP_SUBCLASS:
                return new DataMapArtefactsConfigurator();
            default:
                throw new IllegalStateException("Illegal template type " + type);
        }
    }

    private void initLayout() {
        saveButton.setToolTipText("Save");
        resetToDefaultButton.setToolTipText("Reset to default template");
        findButton.setToolTipText("Find");
        findAndReplaceButton.setToolTipText("Find and replace");
        previewButton.setToolTipText("Generate preview");
        entityComboBox.setToolTipText("Select an entity for the test");

        editingTemplatePane.setSyntaxEditingStyle(VELOCITY_KEY);
        editingTemplatePane.setMarkOccurrences(true);
        RTextScrollPane leftPanel = new RTextScrollPane(editingTemplatePane);

        classPreviewPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        classPreviewPane.setEnabled(false);
        RTextScrollPane rightPanel = new RTextScrollPane(classPreviewPane);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        split.setPreferredSize(new Dimension(1200, 700));
        split.setDividerSize(6);
        split.setDividerLocation(1.0);

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.add(findButton);
        toolBar.add(findAndReplaceButton);
        toolBar.addSeparator();
        toolBar.add(previewButton);
        toolBar.add(entityComboBox);
        toolBar.addSeparator();
        toolBar.add(resetToDefaultButton);

        CellConstraints constraintsTop = new CellConstraints();
        PanelBuilder topPanelBuilder = new PanelBuilder(new FormLayout(
                "left:pref:grow, right:pref", "p, 3dlu, p, 3dlu, p"));
        topPanelBuilder.setDefaultDialogBorder();
        topPanelBuilder.add(toolBar, constraintsTop.xy(1, 1));
        topPanelBuilder.addSeparator("", constraintsTop.xyw(1, 3, 2));
        topPanelBuilder.addLabel("Editing  template", constraintsTop.xy(1, 5));
        topPanelBuilder.addLabel("Class preview", constraintsTop.xy(2, 5));
        JPanel topPanel = topPanelBuilder.getPanel();

        getRootPane().setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
    }

    private void initBindings() {
        saveButton.addActionListener(e -> saveAction());
        previewButton.addActionListener(e -> generatePreviewAction());
        findButton.addActionListener(e -> new TemplateEditorFindDialog(app, this).open());
        findAndReplaceButton.addActionListener(e -> new TemplateEditorFindAndReplaceDialog(app, this).open());
        resetToDefaultButton.addActionListener(e -> resetToDefaultAction());

        editingTemplatePane.getDocument().addDocumentListener(new TemplateChangeListener());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                parent.setEditorOpen(true);
                parent.updateTemplateEditorButtons();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (isTemplateModified && showUnsavedChangesCloseDialog() == 0) {
                    saveAction();
                }
                parent.setEditorOpen(false);
                parent.updateTemplateEditorButtons();
            }
        });
    }

    private void configureEditorView() {
        editingTemplatePane.setText(templateLoader.load(templateType, isTemplateDefault));
        editingTemplatePane.discardAllEdits();
        editingTemplatePane.setCaretPosition(0);
        saveButton.setEnabled(isTemplateModified);
        setTitle(templateType.readableName() + " - cayenne template editor");
    }

    private void bindGeometry() {
        new CMSplitPanePrefs(app.getPrefsManager(), "templateEditor/splitPane").bind(split, 600);
        new CMComponentGeometryPrefs(app.getPrefsManager(), "templateEditor/geometry").bind(this, 1200, 700);
    }

    private void mapVelocityTokenMaker() {
        AbstractTokenMakerFactory tokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        tokenMakerFactory.putMapping(VELOCITY_KEY, VelocityTokenMaker.class.getName());
    }

    private void centerOnParent() {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner == null) {
            return;
        }
        Dimension ownerSize = owner.getSize();
        Dimension size = getSize();
        Point ownerLocation = owner.isShowing() ? owner.getLocationOnScreen() : new java.awt.Point(0, 0);
        int x = ownerLocation.x + ownerSize.width / 2 - size.width / 2;
        int y = ownerLocation.y + ownerSize.height / 2 - size.height / 2;
        setLocation(x, y);
    }

    private void saveAction() {
        templateSaver.save(templateType, isTemplateDefault, getTemplateText());
        parent.getCodeGeneratorController().checkCgenConfigDirty();
        isTemplateModified = false;
        parent.updateTemplatesLabels(configuration);
        saveButton.setEnabled(false);
    }

    private void generatePreviewAction() {
        try {
            ClassGenerationAction action = actionConfigurator.preparePreviewAction(getTemplateText());
            Writer writer = actionConfigurator.getWriter();

            int caretPosition = editingTemplatePane.getCaretPosition();
            try {
                action.execute();
            } catch (ParseErrorException pe) {
                caretPosition = getErrorCaretPosition(pe);
                writer.write(pe.getMessage());
            } catch (Exception e) {
                writer.write(e.getMessage());
            }
            displayPreview(writer, caretPosition);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Preview Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayPreview(Writer writer, int caretPosition) {
        editingTemplatePane.setCaretPosition(caretPosition);
        classPreviewPane.setText(null);
        classPreviewPane.setText(writer.toString());
        classPreviewPane.setCaretPosition(0);
    }

    private int getErrorCaretPosition(ParseErrorException e) throws BadLocationException {
        int errorLineNumber = e.getLineNumber();
        return editingTemplatePane.getLineStartOffset(errorLineNumber - 1);
    }

    private void resetToDefaultAction() {
        if (showResetToDefaultDialog() == JOptionPane.OK_OPTION) {
            editingTemplatePane.setText(templateLoader.load(templateType, true));
            editingTemplatePane.setCaretPosition(0);
            isTemplateModified = true;
            isTemplateDefault = true;
        }
    }

    private int showResetToDefaultDialog() {
        Object[] options = {"Yes", "No"};
        return JOptionPane.showOptionDialog(null,
                "This action will be rollback template to default." +
                        "\n            This action can't be revert. " +
                        "\n                   Are you sure?",
                "Reset template to default",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
    }

    private int showUnsavedChangesCloseDialog() {
        Object[] options = {"Yes", "No"};
        return JOptionPane.showOptionDialog(null,
                "The are unsaved changes in template" +
                        "\n    Do you want to save it? ",
                "Unsaved changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    private class TemplateChangeListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            isTemplateModified = true;
            isTemplateDefault = false;
            saveButton.setEnabled(true);
        }
    }
}
