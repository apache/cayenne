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

package org.apache.cayenne.modeler.ui.project.editor.project.main;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.Project;
import org.apache.cayenne.configuration.runtime.DataDomainProvider;
import org.apache.cayenne.modeler.event.display.ProjectDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProjectDisplayListener;
import org.apache.cayenne.modeler.event.model.ProjectEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.project.editor.EditorForm;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;

/**
 * Panel for editing DataDomain.
 */
public class ProjectMainView extends ProjectPanel implements ProjectDisplayListener {

    protected CMUndoableTextField name;
    protected JCheckBox objectValidation;
    protected JCheckBox sharedCache;

    public ProjectMainView(ProjectSession session) {
        super(session);

        // Create and layout components
        initView();

        // hook up listeners to widgets
        initController();
    }

    protected void initView() {

        // create widgets
        this.name = new CMUndoableTextField(app.getUndoManager());
        this.name.addCommitListener(this::setProjectName);

        this.objectValidation = new CMCheckBox(app.getUndoManager());
        this.sharedCache = new CMCheckBox(app.getUndoManager());

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                EditorForm.LABEL_COLUMN + ", $lcgap, fill:50dlu, $lcgap, fill:47dlu, $lcgap, fill:100",
                "p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $rgap, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addLabel("Name:", cc.xy(1, 1));
        builder.add(name, cc.xywh(3, 1, 5, 1));

        builder.addLabel("Object Validation:", cc.xy(1, 3));
        builder.add(objectValidation, cc.xy(3, 3));

        builder.addLabel("Shared Cache:", cc.xy(1, 5));
        builder.add(sharedCache, cc.xy(3, 5));

        this.setLayout(new BorderLayout());
        this.add(EditorForm.toolBarSpacer(), BorderLayout.NORTH);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        session.addProjectDisplayListener(this);

        // add item listener to checkboxes
        objectValidation.addItemListener(e -> {
            String value = objectValidation.isSelected() ? "true" : "false";
            setProjectProperty(
                    DataDomainProvider.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                    value,
                    DataDomainProvider.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT);
        });

        sharedCache.addItemListener(e -> {
            String value = sharedCache.isSelected() ? "true" : "false";
            setProjectProperty(
                    DataDomainProvider.SHARED_CACHE_ENABLED_PROPERTY,
                    value,
                    DataDomainProvider.SHARED_CACHE_ENABLED_DEFAULT);
        });

    }

    /**
     * Helper method that updates project properties. If a value equals to default, null
     * value is used instead.
     */
    protected void setProjectProperty(String property, String value, String defaultValue) {

        Project project = session
                .project();

        if (project == null) {
            return;
        }

        // no empty strings
        if ("".equals(value)) {
            value = null;
        }

        // use NULL for defaults
        if (value != null && value.equals(defaultValue)) {
            value = null;
        }

        Map<String, String> properties = project.getProperties();
        String oldValue = properties.get(property);
        if (!Objects.equals(value, oldValue)) {
            properties.put(property, value);

            ProjectEvent e = ProjectEvent.ofChange(this, project);
            session.fireProjectEvent(e);
        }
    }

    public String getProjectProperty(String property, String defaultValue) {

        Project project = session
                .project();

        if (project == null) {
            return null;
        }

        String value = project.getProperties().get(property);
        return value != null ? value : defaultValue;
    }

    public boolean getProjectBooleanProperty(String property, String defaultValue) {
        return "true".equalsIgnoreCase(getProjectProperty(property, defaultValue));
    }

    /**
     * Invoked on project selection event. Updates view with the values from the currently
     * selected project.
     */
    public void projectSelected(ProjectDisplayEvent e) {
        Project project = e.getProject();
        if (null == project) {
            return;
        }

        // extract values from the new project object
        name.setText(project.getName());

        objectValidation.setSelected(getProjectBooleanProperty(
                DataDomainProvider.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                DataDomainProvider.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT));

        sharedCache.setSelected(getProjectBooleanProperty(
                DataDomainProvider.SHARED_CACHE_ENABLED_PROPERTY,
                DataDomainProvider.SHARED_CACHE_ENABLED_DEFAULT));
    }

    void setProjectName(String newName) {

        Project project = app
                .getFrame().getProjectSession().project();

        if (Objects.equals(project.getName(), newName)) {
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Enter project name");
        }

        ProjectEvent e = ProjectEvent.ofChange(
                this,
                project,
                project.getName());
        app.getPrefsManager().stageProjectRename(session.project(), newName);
        project.setName(newName);

        session.fireProjectEvent(e);
    }
}
