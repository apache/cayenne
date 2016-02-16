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

import org.apache.cayenne.modeler.ProjectController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EjbqlQueryPropertiesPanel extends SelectPropertiesPanel {

    public EjbqlQueryPropertiesPanel(ProjectController mediator) {
        super(mediator);
    }

    protected PanelBuilder createPanelBuilder() {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, left:max(10dlu;pref), "
                        + "3dlu, left:max(37dlu;pref), 3dlu, fill:max(147dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addSeparator("Select Properties", cc.xywh(1, 1, 7, 1));
        builder.addLabel("Result Caching:", cc.xy(1, 3));
        builder.add(cacheStrategy, cc.xywh(3, 3, 5, 1));
        cacheGroupsLabel = builder.addLabel("Cache Groups:", cc.xy(1, 7));
        builder.add(cacheGroups.getComponent(), cc.xywh(3, 7, 5, 1));
        builder.addLabel("Fetch Offset, Rows:", cc.xy(1, 9));
        builder.add(fetchOffset.getComponent(), cc.xywh(3, 9, 3, 1));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 11));
        builder.add(fetchLimit.getComponent(), cc.xywh(3, 11, 3, 1));
        builder.addLabel("Page Size:", cc.xy(1, 13));
        builder.add(pageSize.getComponent(), cc.xywh(3, 13, 3, 1));
        return builder;
    }

    protected void initView() {
        super.initView();
        this.setLayout(new BorderLayout());
        this.add(createPanelBuilder().getPanel(), BorderLayout.CENTER);
    }
}
