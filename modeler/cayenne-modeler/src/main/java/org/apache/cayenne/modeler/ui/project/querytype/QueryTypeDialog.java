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
package org.apache.cayenne.modeler.ui.project.querytype;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.undo.CreateQueryUndoableEdit;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Modal dialog for picking the type of new query (Select, SQLTemplate, Procedure, EJBQL)
 * before adding it to the current DataMap.
 */
public class QueryTypeDialog extends ProjectDialog {

    private final DataMap dataMap;
    private final DataChannelDescriptor domain;

    private final JRadioButton objectSelect;
    private final JRadioButton sqlSelect;
    private final JRadioButton procedureSelect;
    private final JRadioButton ejbqlSelect;
    private final JButton createButton;
    private final JButton cancelButton;

    public QueryTypeDialog(ProjectSession session, Window owner) {
        super(session, owner, "Select New Query Type", ModalityType.APPLICATION_MODAL);
        this.dataMap = session.getSelectedDataMap();
        this.domain = (DataChannelDescriptor) session.project().getRootNode();

        this.objectSelect = new JRadioButton("Object Select Query");
        this.sqlSelect = new JRadioButton("SQLTemplate Query");
        this.procedureSelect = new JRadioButton("Stored Procedure Query");
        this.ejbqlSelect = new JRadioButton("EJBQL Query");
        objectSelect.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(objectSelect);
        buttonGroup.add(sqlSelect);
        buttonGroup.add(procedureSelect);
        buttonGroup.add(ejbqlSelect);

        this.createButton = new JButton("Create");
        this.cancelButton = new JButton("Cancel");

        initLayout();
        initBindings();
    }

    /**
     * Fires the standard "query added" notification sequence on the given session.
     * Used both internally on Create, and externally by paste handling.
     */
    public static void fireQueryEvent(Object src, ProjectSession session, DataMap dataMap, QueryDescriptor query) {
        session.fireQueryEvent(QueryEvent.ofAdd(src, query, dataMap));
        session.displayQuery(new QueryDisplayEvent(src,
                (DataChannelDescriptor) session.project().getRootNode(),
                dataMap, query));
    }

    private void initLayout() {
        getRootPane().setDefaultButton(createButton);

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:max(180dlu;pref)",
                "p, 4dlu, p, 4dlu, p, 4dlu, p, 4dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.add(objectSelect, cc.xy(1, 1));
        builder.add(sqlSelect, cc.xy(1, 3));
        builder.add(procedureSelect, cc.xy(1, 5));
        builder.add(ejbqlSelect, cc.xy(1, 7));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        add(new CMButtonPanel(cancelButton, createButton), BorderLayout.SOUTH);
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> dispose());
        createButton.addActionListener(e -> createQuery());
    }

    private void createQuery() {
        QueryDescriptor query = QueryDescriptor.descriptor(selectedQueryType());
        query.setName(NameBuilder.of(query, dataMap).build());
        query.setDataMap(dataMap);

        dataMap.addQueryDescriptor(query);

        app.getUndoManager().addEdit(new CreateQueryUndoableEdit(session, domain, dataMap, query));

        fireQueryEvent(this, session, dataMap, query);
        dispose();
    }

    private String selectedQueryType() {
        if (sqlSelect.isSelected()) return QueryDescriptor.SQL_TEMPLATE;
        if (procedureSelect.isSelected()) return QueryDescriptor.PROCEDURE_QUERY;
        if (ejbqlSelect.isSelected()) return QueryDescriptor.EJBQL_QUERY;
        return QueryDescriptor.SELECT_QUERY;
    }
}
