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

import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.control.FileChooser;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @since 4.3
 */
public class CreateTemplateFromDefaultController extends CayenneController {
    protected CreateTemplateFromDefaultView view;
    protected boolean canceled;
    protected CayennePreferenceEditor editor;
    protected Preferences preferences;
    private final CodeTemplateManager codeTemplateManager;

    private static final Logger logger = LoggerFactory.getLogger(CreateTemplateFromDefaultController.class);

    public CreateTemplateFromDefaultController(TemplatePreferences parent) {
        super(parent);

        JDialog parentDialog = (JDialog) SwingUtilities.getAncestorOfClass(
                JDialog.class,
                parent.getView());
        this.view = new CreateTemplateFromDefaultView(parentDialog);

        PreferenceEditor parentEditor = parent.getEditor();
        if (parentEditor instanceof CayennePreferenceEditor) {
            this.editor = (CayennePreferenceEditor) parentEditor;
        }
        this.preferences = parent.getTemplatePreferences();
        this.codeTemplateManager = application.getCodeTemplateManager();
        initBindings();
        initListeners();
    }

    public Component getView() {
        return view;
    }

    private void initListeners() {
        view.getSelectAll().addActionListener(val -> {
            boolean isSelected = view.getSelectAll().isSelected();
            view.getTemplatesCheckboxes().forEach(template -> template.setSelected(isSelected));
        });
    }


    /**
     * find start directory in preferences
     */
    private FSPath getLastTemplateDirectory() {
        FSPath path = new FSPath(application.getPreferencesNode(
                CodeTemplateManager.class,
                "lastTemplate"));
        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }
        return path;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(application.getBindingFactory(), this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getCreateButton(), "createAction()");
        view.getFolderChooser().setCurrentDirectory(getLastTemplateDirectory().getExistingDirectory(true));
        view.getFolderChooser().addPropertyChangeListener(
                FileChooser.CURRENT_DIRECTORY_PROPERTY,
                evt -> updateLastTemplateDirectory());
    }

    @SuppressWarnings("unused")
    public void createAction() {
        if (!Util.isEmptyString(view.getPrefix().getText()) && (view.getFolderChooser().getFile() != null)) {
            updateLastTemplateDirectory();
            canceled = false;
            view.dispose();
        } else {
            JOptionPane.showMessageDialog(view, "Enter prefix and select folder, please", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateLastTemplateDirectory() {
        final FSPath path = getLastTemplateDirectory();
        File directory = view.getFolderChooser().getFile();
        path.setDirectory(directory);
    }

    @SuppressWarnings("unused")
    public void cancelAction() {
        canceled = true;
        view.dispose();
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public List<FSPath> startupAction() {
        canceled = true;
        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
        return createTemplates();
    }

    protected ArrayList<FSPath> createTemplates() {
        ArrayList<FSPath> paths = new ArrayList<>();
        if (!canceled) {
            List<JCheckBox> selectedTemplates = view.getSelectedTemplates();
            for (JCheckBox template : selectedTemplates) {
                if (!canceled) {
                    paths.add(createTemplate(template.getText()));
                } else {
                    break;
                }
            }
        }
        return paths;
    }

    private FSPath createTemplate(String defaultTemplateName) {
        String prefix = view.getPrefix().getText();
        File destDir = view.getFolderChooser().getFile();
        String newTemplateName = prefix + defaultTemplateName;
        int result = isNameExist(newTemplateName) ? showOverwriteDialog(newTemplateName) : JOptionPane.OK_OPTION;
        TemplateType typeByName = TemplateType.byName(defaultTemplateName);
        if (result == JOptionPane.OK_OPTION && typeByName != null) {
            String newTemplatePath = destDir.getAbsolutePath() + File.separator + prefix + typeByName.fullFileName();
            createTemplateFile(newTemplatePath, defaultTemplateName);
            FSPath fsPath = codeTemplateManager.addTemplate(newTemplatePath, newTemplateName);
            editor.getAddedNode().add(fsPath.getCurrentPreference());
            return fsPath;
        } else if (result == JOptionPane.CANCEL_OPTION) {
            canceled = true;
        }
        return null;
    }

    private boolean isNameExist(String templateName) {
        try {
            return preferences.nodeExists(templateName);
        } catch (BackingStoreException e) {
            logger.warn("Error reading preferences");
        }
        return false;
    }

    private void createTemplateFile(String newTemplatePath, String templateName) {
        Path dest = Paths.get(newTemplatePath);
        int result = dest.toFile().exists() ? showOverwriteDialog(dest.toString()) : JOptionPane.OK_OPTION;
        TemplateType typeByName = TemplateType.byName(templateName);
        if (result == JOptionPane.OK_OPTION && typeByName != null) {
            writeFile(Paths.get(typeByName.pathFromSourceRoot()), dest);
        } else if (result == JOptionPane.CANCEL_OPTION) {
            canceled = true;
        }
    }

    private void writeFile(Path source, Path target) {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(source.toString());
            if (stream == null) {
                throw new IOException();
            }
            Files.copy(stream, target);
        } catch (IOException e) {
            canceled = true;
            JOptionPane.showMessageDialog(
                    view,
                    "File writing error \n" + target,
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            logger.warn("File writing error {}", target, e);
        }
    }

    /**
     * Brings up a OVERWRITE_SKIP_CANCEL option dialog
     *
     * @return 0 if "OVERWRITE" was selected, 1 and 2 for the "SKIP" and "CANCEL respectively "
     */
    private int showOverwriteDialog(String dest) {
        Object[] options = {"Overwrite", "Skip", "Cancel"};
        return JOptionPane.showOptionDialog(null,
                dest + " \nis already exist, overwrite?",
                "Replace or skip the file",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }
}

