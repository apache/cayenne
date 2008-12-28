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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceDetail;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

public class TemplatePreferences extends CayenneController {

    protected TemplatePreferencesView view;
    protected PreferenceEditor editor;
    protected List<FSPath> templateEntries;
    protected ObjectBinding tableBinding;

    public TemplatePreferences(PreferenceDialog parent) {
        super(parent);

        this.view = new TemplatePreferencesView();
        this.editor = parent.getEditor();
        this.templateEntries = new ArrayList(getTemplateDomain().getDetails(FSPath.class));

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected Domain getTemplateDomain() {
        Domain domain = CodeTemplateManager.getTemplateDomain(getApplication());
        return editor.editableInstance(domain);
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

        PreferenceDetail selection = (PreferenceDetail) templateEntries.remove(selected);
        editor.deleteDetail(getTemplateDomain(), selection.getKey());
        ((AbstractTableModel) view.getTable().getModel()).fireTableRowsDeleted(
                selected,
                selected);
    }
}
