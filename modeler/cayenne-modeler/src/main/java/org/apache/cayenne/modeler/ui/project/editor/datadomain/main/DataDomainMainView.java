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

package org.apache.cayenne.modeler.ui.project.editor.datadomain.main;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayListener;
import org.apache.cayenne.modeler.event.model.DomainEvent;
import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.project.ProjectSession;
import java.util.Objects;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Panel for editing DataDomain.
 */
public class DataDomainMainView extends ProjectPanel implements DomainDisplayListener {

    protected CMUndoableTextField name;
    protected JCheckBox objectValidation;
    protected JCheckBox sharedCache;

    public DataDomainMainView(ProjectSession session) {
        super(session);

        // Create and layout components
        initView();

        // hook up listeners to widgets
        initController();
    }

    protected void initView() {

        // create widgets
        this.name = new CMUndoableTextField(app.getUndoManager());
        this.name.addCommitListener(this::setDomainName);

        this.objectValidation = new CMCheckBox(app.getUndoManager());
        this.sharedCache = new CMCheckBox(app.getUndoManager());

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:50dlu, 3dlu, fill:47dlu, 3dlu, fill:100",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("DataDomain Configuration", cc.xywh(1, 1, 7, 1));
        builder.addLabel("Name:", cc.xy(1, 3));
        builder.add(name, cc.xywh(3, 3, 5, 1));

        builder.addLabel("Object Validation:", cc.xy(1, 5));
        builder.add(objectValidation, cc.xy(3, 5));

        builder.addLabel("Use Shared Cache:", cc.xy(1, 7));
        builder.add(sharedCache, cc.xy(3, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        session.addDomainDisplayListener(this);

        // add item listener to checkboxes
        objectValidation.addItemListener(e -> {
            String value = objectValidation.isSelected() ? "true" : "false";
            setDomainProperty(
                    DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                    value,
                    Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT));
        });

        sharedCache.addItemListener(e -> {
            String value = sharedCache.isSelected() ? "true" : "false";
            setDomainProperty(
                    DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                    value,
                    Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT));
        });

    }

    /**
     * Helper method that updates domain properties. If a value equals to default, null
     * value is used instead.
     */
    protected void setDomainProperty(String property, String value, String defaultValue) {

        DataChannelDescriptor domain = (DataChannelDescriptor) session
                .project()
                .getRootNode();

        if (domain == null) {
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

        Map<String, String> properties = domain.getProperties();
        String oldValue = properties.get(property);
        if (!Objects.equals(value, oldValue)) {
            properties.put(property, value);

            DomainEvent e = DomainEvent.ofChange(this, domain);
            session.fireDomainEvent(e);
        }
    }

    public String getDomainProperty(String property, String defaultValue) {

        DataChannelDescriptor domain = (DataChannelDescriptor) session
                .project()
                .getRootNode();

        if (domain == null) {
            return null;
        }

        String value = domain.getProperties().get(property);
        return value != null ? value : defaultValue;
    }

    public boolean getDomainBooleanProperty(String property, String defaultValue) {
        return "true".equalsIgnoreCase(getDomainProperty(property, defaultValue));
    }

    /**
     * Invoked on domain selection event. Updates view with the values from the currently
     * selected domain.
     */
    public void domainSelected(DomainDisplayEvent e) {
        DataChannelDescriptor domain = e.getDomain();
        if (null == domain) {
            return;
        }

        // extract values from the new domain object
        name.setText(domain.getName());

        objectValidation.setSelected(getDomainBooleanProperty(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT)));

        sharedCache.setSelected(getDomainBooleanProperty(
                DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT)));
    }

    void setDomainName(String newName) {

        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) app
                .getFrame().getProjectSession().project()
                .getRootNode();

        if (Objects.equals(dataChannelDescriptor.getName(), newName)) {
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Enter name for DataDomain");
        }

        DomainEvent e = DomainEvent.ofChange(
                this,
                dataChannelDescriptor,
                dataChannelDescriptor.getName());
        app.getPrefsManager().stageProjectRename(session.project(), newName);
        dataChannelDescriptor.setName(newName);

        session.fireDomainEvent(e);
    }
}
