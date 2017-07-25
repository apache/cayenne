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
package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class EjbqlQueryMainTab extends JPanel{

    protected ProjectController mediator;
    protected TextAdapter name;
    protected TextAdapter comment;
    protected EjbqlQueryPropertiesPanel properties;
    protected TextAdapter qualifier;

    public EjbqlQueryMainTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();  
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setQueryName(text);
            }
        };

        comment = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setQueryComment(text);
            }
        };

        properties = new EjbqlQueryPropertiesPanel(mediator);
        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addSeparator("EJBQL Query Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));
        builder.addLabel("Comment:", cc.xy(1, 5));
        builder.add(comment.getComponent(), cc.xy(3, 5));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.EJBQL_QUERY.equals(query.getType())) {
            setVisible(false);
            return;
        }

        name.setText(query.getName());
        comment.setText(getQueryComment(query));
        properties.initFromModel(query);
        setVisible(true);
    }

    protected QueryDescriptor getQuery() {
        QueryDescriptor query = mediator.getCurrentQuery();
        return (query != null && QueryDescriptor.EJBQL_QUERY.equals(query.getType())) ? query : null;
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        QueryDescriptor query = getQuery();

        if (query == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, query.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Query name is required.");
        }

        DataMap map = mediator.getCurrentDataMap();

        if (map.getQueryDescriptor(newName) == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        }
        else {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    private void setQueryComment(String text) {
        QueryDescriptor query = getQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), query, ObjectInfo.COMMENT, text);
        mediator.fireQueryEvent(new QueryEvent(this, query));
    }

    private String getQueryComment(QueryDescriptor queryDescriptor) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), queryDescriptor, ObjectInfo.COMMENT);
    }
}
