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

package org.apache.cayenne.modeler.dialog.codegen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ImageRendererColumn;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassesTabController extends CayenneController {

    public static final String GENERATE_PROPERTY = "generate";

    protected ClassesTabPanel view;

    private Map<DataMap, ObjectBinding> objectBindings;

    protected Collection<DataMap> dataMaps;

    protected Map<DataMap, List<Object>> objectList;

    private List<Object> currentCollection;

    public ClassesTabController(CodeGeneratorControllerBase parent, Collection<DataMap> dataMaps) {
        super(parent);

        currentCollection = new ArrayList<>();

        this.objectList = new HashMap<>();
        for(DataMap dataMap : dataMaps) {
            List<Object> list = new ArrayList<>(Arrays.asList(dataMap));
            list.addAll(Stream.concat(dataMap.getObjEntities().stream(), dataMap.getEmbeddables().stream())
                    .collect(Collectors.toList()));
            objectList.put(dataMap, list);
        }

        this.objectBindings = new HashMap<>();
        this.dataMaps = dataMaps;
        this.view = new ClassesTabPanel(dataMaps);

        initBindings();
    }

    protected CodeGeneratorControllerBase getParentController() {
        return (CodeGeneratorControllerBase) getParent();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);
        
        tableBuilder.addColumn(
                "",
                "parent.setCurrentClass(#item), selected",
                Boolean.class,
                true,
                Boolean.TRUE);

        tableBuilder.addColumn(
                "Class",
                "parent.getItemName(#item)",
                JLabel.class,
                false,
                "XXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "Comments, Warnings",
                "parent.getProblem(#item)",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXX");

        for(DataMap dataMap : dataMaps) {
            if(view.getDataMapTables().get(dataMap) != null) {
                currentCollection = objectList.get(dataMap);
                objectBindings.put(dataMap, tableBuilder.bindToTable(view.getDataMapTables().get(dataMap), "currentCollection"));
                view.getDataMapTables().get(dataMap).getColumnModel().getColumn(1).setCellRenderer(new ImageRendererColumn());
            }
            if(view.getDataMapJCheckBoxMap().get(dataMap) != null) {
                view.getDataMapJCheckBoxMap().get(dataMap).addActionListener(val -> checkDataMap(dataMap, ((JCheckBox)val.getSource()).isSelected()));
            }
        }
    }

    public List<Object> getCurrentCollection() {
        return currentCollection;
    }

    public boolean isSelected() {
        return getParentController().isSelected();
    }

    public void setSelected(boolean selected) {
        getParentController().setSelected(selected);
        classSelectedAction();

        for(DataMap dataMap : dataMaps) {
            if(view.isAllCheckBoxesFromDataMapSelected(dataMap)) {
                view.getDataMapJCheckBoxMap().get(dataMap).setSelected(true);
            } else {
                view.getDataMapJCheckBoxMap().get(dataMap).setSelected(false);
            }
        }
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void classSelectedAction() {
        int selectedCount = getParentController().getSelectedEntitiesSize() + getParentController().getSelectedEmbeddablesSize() + getParentController().getSelectedDataMapsSize();

        if (selectedCount == 0) {
            view.getCheckAll().setSelected(false);
        }
        else if (selectedCount == getParentController().getClasses().size()) {
            view.getCheckAll().setSelected(true);
        }
    }

    /**
     * An action that updates entity check boxes in response to the Select All state
     * change.
     */
    public void checkAllAction() {
        if (getParentController().updateSelection(view.getCheckAll().isSelected() ? o -> true : o -> false)) {
            dataMaps.forEach(dataMap -> {
                if(objectBindings.get(dataMap) != null) {
                    currentCollection = objectList.get(dataMap);
                    objectBindings.get(dataMap).updateView();
                }
            });
        }
    }

    private void checkDataMap(DataMap dataMap, boolean selected) {
        if (getParentController().updateDataMapSelection(selected ? o -> true : o -> false, dataMap)){
            if(objectBindings.get(dataMap) != null) {
                currentCollection = objectList.get(dataMap);
                objectBindings.get(dataMap).updateView();
            }
            if(isAllMapsSelected()) {
                view.getCheckAll().setSelected(true);
            }
        }
    }

    private boolean isAllMapsSelected() {
        for(DataMap dataMap : dataMaps) {
            if(view.getDataMapJCheckBoxMap().get(dataMap) != null) {
                if(!view.getDataMapJCheckBoxMap().get(dataMap).isSelected()) {
                    return false;
                }
            }
        }
        return true;
    }
}
