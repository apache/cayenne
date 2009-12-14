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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel that supports editing the properties of a query based on ObjEntity.
 * 
 */
public class ObjectQueryPropertiesPanel extends SelectPropertiesPanel {

    protected JCheckBox dataRows;

    public ObjectQueryPropertiesPanel(ProjectController mediator) {
        super(mediator);
    }

    protected void initView() {
        super.initView();
        // create widgets

        dataRows = new JCheckBox();

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, left:max(50dlu;pref), fill:max(150dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("", cc.xywh(1, 1, 4, 1));

        builder.addLabel("Result Caching:", cc.xy(1, 3));
        builder.add(cacheStrategy, cc.xywh(3, 3, 2, 1));
        cacheGroupsLabel = builder.addLabel("Cache Groups:", cc.xy(1, 7));
        builder.add(cacheGroups.getComponent(), cc.xywh(3, 7, 2, 1));
        builder.addLabel("Fetch Data Rows:", cc.xy(1, 9));
        builder.add(dataRows, cc.xy(3, 9));
        builder.addLabel("Fetch Offset, Rows:", cc.xy(1, 11));
        builder.add(fetchOffset.getComponent(), cc.xy(3, 11));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 13));
        builder.add(fetchLimit.getComponent(), cc.xy(3, 13));
        builder.addLabel("Page Size:", cc.xy(1, 15));
        builder.add(pageSize.getComponent(), cc.xy(3, 15));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        super.initController();

        dataRows.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Boolean b = dataRows.isSelected() ? Boolean.TRUE : Boolean.FALSE;
                setQueryProperty("fetchingDataRows", b);
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(Query query) {
        super.initFromModel(query);

        dataRows.setSelected(((SelectQuery) query).isFetchingDataRows());
    }
}
