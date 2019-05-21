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

import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class TemplatePreferences extends CayenneController {

    protected TemplatePreferencesView view;
    protected CayennePreferenceEditor editor;
    protected List<FSPath> templateEntries;
    protected ObjectBinding tableBinding;

    private static Logger logger = LoggerFactory.getLogger(TemplatePreferences.class);

    public TemplatePreferences(PreferenceDialog parent) {
        super(parent);

        this.view = new TemplatePreferencesView();
        PreferenceEditor editor = parent.getEditor();
        if (editor instanceof CayennePreferenceEditor) {
            this.editor = (CayennePreferenceEditor) editor;
        }
        this.templateEntries = new ArrayList<>();

        try {
            String[] keys = getTemplatePreferences().childrenNames();
            for (String key : keys) {
                templateEntries.add((FSPath) application
                        .getCayenneProjectPreferences()
                        .getProjectDetailObject(
                                FSPath.class,
                                getTemplatePreferences().node(key)));
            }
        }
        catch (BackingStoreException e) {
            logger.warn("Error reading preferences");
        }

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected Preferences getTemplatePreferences() {
        return application.getPreferencesNode(
                CodeTemplateManager.class,
                CodeTemplateManager.NODE_NAME);
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getAddButton(), "addTemplateAction()");
        builder.bindToAction(view.getRemoveButton(), "removeTemplateAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "Name",
                "#item.key",
                String.class,
                false,
                "XXXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "Path",
                "#item.path",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        tableBuilder.bindToTable(view.getTable(), "templateEntries").updateView();
    }

    public List<FSPath> getTemplateEntries() {
        return templateEntries;
    }

    public PreferenceEditor getEditor() {
        return editor;
    }

    public void addTemplateAction() {
        FSPath path = new TemplateCreator(this).startupAction();
        addToTemplateEntries(path);
    }

    void addTemplateAction(String templatePath, String superTemplatePath) {
        if(templatePath != null) {
            createTemplate(templatePath);
        }
        if(superTemplatePath != null) {
            createTemplate(superTemplatePath);
        }
    }

    private void createTemplate(String templatePath) {
        TemplateCreator templateCreator = new TemplateCreator(this);
        TemplateCreatorView creatorView = (TemplateCreatorView)templateCreator.getView();
        creatorView.getTemplateChooser().setFile(Paths.get(templatePath).toFile());
        FSPath path = templateCreator.startupAction();
        addToTemplateEntries(path);
    }

    private void addToTemplateEntries(FSPath path) {
        if (path != null) {
            int len = templateEntries.size();
            templateEntries.add(path);
            ((AbstractTableModel) view.getTable().getModel()).fireTableRowsInserted(
                    len,
                    len);
        }
    }

    public void removeTemplateAction() {
        int selected = view.getTable().getSelectedRow();
        if (selected < 0) {
            return;
        }

        Object key = ((AbstractTableModel) view.getTable().getModel()).getValueAt(
                selected,
                0);

        editor.getRemovedNode().add(getTemplatePreferences().node((String) key));
        templateEntries.remove(selected);

        ((AbstractTableModel) view.getTable().getModel()).fireTableRowsDeleted(
                selected,
                selected);

    }
}
