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

package org.apache.cayenne.modeler.editor.cgen.templateeditor;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.editor.cgen.StandardModeController;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.velocity.exception.ParseErrorException;

import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Writer;

/**
 * @since 5.0
 */
public class TemplateEditorController extends CayenneController {

    private final DataMap currentDataMap;
    private final CgenConfiguration configuration;
    private final StandardModeController parentController;
    private final TemplateType templateType;
    private EditorTemplateLoader templateLoader;
    private EditorTemplateSaver templateSaver;
    private Boolean isTemplateModified;
    private Boolean isTemplateDefault;
    private ArtefactsConfigurator artefactsConfigurator;
    private TemplateEditorView editorView;
    private PreviewActionConfigurator actionConfigurator;


    public TemplateEditorController(StandardModeController parentController, TemplateType templateType) {
        super(parentController.getCodeGeneratorController());
        this.templateType = templateType;
        this.configuration = parentController.getCodeGeneratorController().getCgenConfiguration();
        this.currentDataMap = configuration.getDataMap();
        this.parentController = parentController;
    }

    public void startupAction() {
        this.artefactsConfigurator = setupArtefactConfigurator();
        this.editorView = new TemplateEditorView(artefactsConfigurator.getArtifactsNames(currentDataMap));
        this.actionConfigurator = new PreviewActionConfigurator(this);
        this.isTemplateDefault = TemplateType.isDefault(configuration.getTemplateByType(templateType).getData());
        this.isTemplateModified = false;
        this.templateLoader = new EditorTemplateLoader(configuration, this.editorView);
        this.templateSaver = new EditorTemplateSaver(configuration);

        configureEditorView(templateType);
        addListeners();
        centerView();
        initBindings();
        this.editorView.setVisible(true);
    }

    private ArtefactsConfigurator setupArtefactConfigurator() {
        switch (templateType) {
            case ENTITY_SUPERCLASS:
            case ENTITY_SUBCLASS: {
                return new EntityArtefactsConfigurator();
            }
            case EMBEDDABLE_SUPERCLASS:
            case EMBEDDABLE_SUBCLASS: {
                return new EmbeddableArtefactsConfigurator();
            }
            case DATAMAP_SUPERCLASS:
            case DATAMAP_SUBCLASS: {
                return new DataMapArtefactsConfigurator();
            }
            default:
                throw new IllegalStateException("Illegal template type " + templateType);
        }
    }

    private void configureEditorView(TemplateType templateType) {
        this.editorView.getEditingTemplatePane().setText(templateLoader.load(templateType, isTemplateDefault));
        this.editorView.editingTemplatePane.setCaretPosition(0);
        this.editorView.getSaveButton().setEnabled(isTemplateModified);
        this.editorView.setTitle(templateType.readableName() + " - cayenne template editor");
    }

    @Override
    public TemplateEditorView getView() {
        return editorView;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(editorView.getSaveButton(), "saveAction()");
        builder.bindToAction(editorView.getPreviewButton(), "generatePreviewAction()");
        builder.bindToAction(editorView.getFindButton(), "findAction()");
        builder.bindToAction(editorView.getFindAndReplaceButton(), "findAndReplaceAction()");
        builder.bindToAction(editorView.getResetToDefaultButton(), "resetToDefaultAction()");
    }

    protected void addListeners() {
        editorView.getEditingTemplatePane().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {//noop
            }

            @Override
            public void removeUpdate(DocumentEvent e) {//noop
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                isTemplateModified = true;
                isTemplateDefault = false;
                editorView.getSaveButton().setEnabled(true);
            }
        });

        editorView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                parentController.setEditorOpen(true);
                parentController.updateTemplateEditorButtons();
            }
        });

        editorView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isTemplateModified && showUnsavedChangesCloseDialog() == 0) {
                    saveAction();
                }
                parentController.setEditorOpen(false);
                parentController.updateTemplateEditorButtons();
            }
        });

    }

    @SuppressWarnings("unused")
    public void saveAction() {
        templateSaver.save(templateType, isTemplateDefault, editorView.getTemplateText());
        parentController.getCodeGeneratorController().checkCgenConfigDirty();
        isTemplateModified = false;
        parentController.updateTemplatesLabels(configuration);
        editorView.getSaveButton().setEnabled(false);
    }

    @SuppressWarnings("unused")
    public void generatePreviewAction() throws Exception {
        ClassGenerationAction action = actionConfigurator.preparePreviewAction(editorView.getTemplateText());
        Writer writer = actionConfigurator.getWriter();

        int caretPosition = editorView.getEditingTemplatePane().getCaretPosition();
        try {
            action.execute();
        } catch (ParseErrorException pe) {
            caretPosition = getErrorCaretPosition(pe);
            writer.write(pe.getMessage());
        } catch (Exception e) {
            writer.write(e.getMessage());
        }
        displayPreview(writer, caretPosition);
    }

    private void displayPreview(Writer writer, int caretPosition) {
        editorView.getEditingTemplatePane().setCaretPosition(caretPosition);
        editorView.getClassPreviewPane().setText(null);
        editorView.getClassPreviewPane().setText(writer.toString());
        editorView.getClassPreviewPane().setCaretPosition(0);
    }

    private int getErrorCaretPosition(ParseErrorException e) throws BadLocationException {
        int errorLineNumber = e.getLineNumber();
        return editorView.getEditingTemplatePane().getLineStartOffset(errorLineNumber - 1);
    }

    @SuppressWarnings("unused")
    public void findAction() {
        new FindController(this).startupAction();
    }

    @SuppressWarnings("unused")
    public void findAndReplaceAction() {
        new FindAndReplaceController(this).startupAction();
    }

    @SuppressWarnings("unused")
    public void resetToDefaultAction() {
        int result = showResetToDefaultDialog();
        if (result == JOptionPane.OK_OPTION) {
            editorView.getEditingTemplatePane().setText(templateLoader.load(templateType, true));
            editorView.getEditingTemplatePane().setCaretPosition(0);
            isTemplateModified = true;
            isTemplateDefault = true;
        }
    }

    /**
     * Brings up a YES_NO option dialog
     *
     * @return 0 if "Yes" was selected 1 for the "No""
     */
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

    /**
     * Brings up a YES_NO option dialog
     *
     * @return 0 if "Yes" was selected 1 for the "No""
     */
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

    public DataMap getCurrentDataMap() {
        return currentDataMap;
    }

    public CgenConfiguration getConfiguration() {
        return configuration;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public ArtefactsConfigurator getArtefactsConfigurator() {
        return artefactsConfigurator;
    }

    public String getSelectedArtifactName() {
        return getView().getSelectedArtifactName();
    }
}
