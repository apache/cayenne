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

package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.query.QueryMetadata;

/**
 * A panel that supports editing the properties a query not based on ObjEntity, but still
 * supporting Persistent objects retrieval.
 * 
 */
public abstract class RawQueryPropertiesPanel extends SelectPropertiesPanel {

    protected JCheckBox persistentObjects;
    protected JComboBox<ObjEntity> entities;

    public RawQueryPropertiesPanel(ProjectController mediator) {
        super(mediator);
    }

    protected void initController() {
        super.initController();
        persistentObjects.addItemListener(e -> setFetchingPersistentObjects(persistentObjects.isSelected()));

        entities.addActionListener(event -> {
            ObjEntity entity = (ObjEntity) entities.getModel().getSelectedItem();
            setEntity(entity);
        });
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
        cacheGroupsLabel = builder.addLabel("Cache Group:", cc.xy(1, 7));
        builder.add(cacheGroups.getComponent(), cc.xywh(3, 7, 5, 1));
        builder.addLabel("Fetch Persistent Objects:", cc.xy(1, 9));
        builder.add(persistentObjects, cc.xy(3, 9));
        builder.add(entities, cc.xywh(5, 9, 3, 1));
        builder.addLabel("Fetch Offset, Rows:", cc.xy(1, 11));
        builder.add(fetchOffset.getComponent(), cc.xywh(3, 11, 3, 1));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 13));
        builder.add(fetchLimit.getComponent(), cc.xywh(3, 13, 3, 1));
        builder.addLabel("Page Size:", cc.xy(1, 15));
        builder.add(pageSize.getComponent(), cc.xywh(3, 15, 3, 1));
        return builder;
    }

    protected void initView() {
        super.initView();

        // create widgets

        persistentObjects = new JCayenneCheckBox();

        entities = Application.getWidgetFactory().createUndoableComboBox();
        entities.setRenderer(CellRenderers.listRendererWithIcons());

        this.setLayout(new BorderLayout());
        this.add(createPanelBuilder().getPanel(), BorderLayout.CENTER);
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(QueryDescriptor query) {
        super.initFromModel(query);

        boolean fetchingDO = !Boolean.valueOf(query.getProperties().get(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY));
        persistentObjects.setSelected(fetchingDO);

        // TODO: now we only allow ObjEntities from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        List<ObjEntity> objEntities = new ArrayList<>(map.getObjEntities());

        if (objEntities.size() > 1) {
            objEntities.sort(Comparators.getDataMapChildrenComparator());
        }

        entities.setEnabled(fetchingDO && isEnabled());
        DefaultComboBoxModel<ObjEntity> model = new DefaultComboBoxModel<>(objEntities.toArray(new ObjEntity[0]));
        model.setSelectedItem(getEntity(query));
        entities.setModel(model);
    }

    protected abstract void setEntity(ObjEntity selectedEntity);

    protected abstract ObjEntity getEntity(QueryDescriptor query);

    protected void setFetchingPersistentObjects(boolean fetchingPersistentObjects) {
        entities.setEnabled(fetchingPersistentObjects && isEnabled());

        if (!fetchingPersistentObjects) {
            entities.getModel().setSelectedItem(null);
        }

        setQueryProperty(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY,
                fetchingPersistentObjects ? Boolean.FALSE.toString() : Boolean.TRUE.toString());
    }
}
