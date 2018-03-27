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
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A base superclass of a top controller for the code generator. Defines all common model
 * parts used in class generation.
 *
 */
public abstract class CodeGeneratorControllerBase extends CayenneController {

    public static final String SELECTED_PROPERTY = "selected";

    protected Collection<DataMap> dataMaps;

    protected ValidationResult validation;

    protected List<Object> classes;

    protected Set<String> selectedEntities;
    protected Set<String> selectedEmbeddables;

    protected transient Object currentClass;

    public CodeGeneratorControllerBase(CayenneController parent, Collection<DataMap> dataMaps) {
        super(parent);

        this.dataMaps = dataMaps;
        this.classes = new ArrayList<>();

        for(DataMap dataMap:dataMaps){
            this.classes.addAll(dataMap.getObjEntities());
            this.classes.addAll(dataMap.getEmbeddables());
        }
        this.selectedEntities = new HashSet<>();
        this.selectedEmbeddables = new HashSet<>();
    }

    public List<Object> getClasses() {
        return classes;
    }

    public abstract Component getView();

    public void validate(GeneratorController validator) {

        ValidationResult validationBuffer = new ValidationResult();

        if (validator != null) {
            for (Object classObj : classes) {
                if (classObj instanceof ObjEntity) {
                    validator.validateEntity(
                            validationBuffer,
                            (ObjEntity) classObj,
                            false);
                }
                else if (classObj instanceof Embeddable) {
                    validator.validateEmbeddable(validationBuffer, (Embeddable) classObj);
                }
            }

        }

        this.validation = validationBuffer;
    }

    public boolean updateSelection(Predicate<Object> predicate) {

        boolean modified = false;

        for (Object classObj : classes) {
            boolean select = predicate.test(classObj);
            if (classObj instanceof ObjEntity) {

                if (select) {
                    if (selectedEntities.add(((ObjEntity) classObj).getName())) {
                        modified = true;
                    }
                }
                else {
                    if (selectedEntities.remove(((ObjEntity) classObj).getName())) {
                        modified = true;
                    }
                }
            }
            else if (classObj instanceof Embeddable) {
                if (select) {
                    if (selectedEmbeddables.add(((Embeddable) classObj).getClassName())) {
                        modified = true;
                    }
                }
                else {
                    if (selectedEmbeddables
                            .remove(((Embeddable) classObj).getClassName())) {
                        modified = true;
                    }
                }
            }

        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }

    public List<Embeddable> getSelectedEmbeddables() {

        List<Embeddable> selected = new ArrayList<>(selectedEmbeddables.size());

        for (Object classObj : classes) {
            if (classObj instanceof Embeddable
                    && selectedEmbeddables.contains(((Embeddable) classObj)
                            .getClassName())) {
                selected.add((Embeddable) classObj);
            }
        }

        return selected;
    }

    public List<ObjEntity> getSelectedEntities() {
        List<ObjEntity> selected = new ArrayList<>(selectedEntities.size());
        for (Object classObj : classes) {
            if (classObj instanceof ObjEntity
                    && selectedEntities.contains(((ObjEntity) classObj).getName())) {
                selected.add(((ObjEntity) classObj));
            }
        }

        return selected;
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public int getSelectedEmbeddablesSize() {
        return selectedEmbeddables.size();
    }

    /**
     * Returns the first encountered validation problem for an antity matching the name or
     * null if the entity is valid or the entity is not present.
     */
    public String getProblem(Object obj) {

        String name = null;
        
        if (obj instanceof ObjEntity) {
            name = ((ObjEntity) obj).getName();
        }
        else if (obj instanceof Embeddable) {
            name = ((Embeddable) obj).getClassName();
        }
        
        if (validation == null) {
            return null;
        }

        List failures = validation.getFailures(name);
        if (failures.isEmpty()) {
            return null;
        }

        return ((ValidationFailure) failures.get(0)).getDescription();
    }

    public boolean isSelected() {
        if (currentClass instanceof ObjEntity) {
            return selectedEntities
                    .contains(((ObjEntity) currentClass).getName());
        }
        if (currentClass instanceof Embeddable) {
            return selectedEmbeddables
                    .contains(((Embeddable) currentClass).getClassName());
        }
        return false;

    }

    public void setSelected(boolean selectedFlag) {
        if (currentClass == null) {
            return;
        }
        if (currentClass instanceof ObjEntity) {
            if (selectedFlag) {
                if (selectedEntities.add(((ObjEntity) currentClass).getName())) {
                    firePropertyChange(SELECTED_PROPERTY, null, null);
                }
            }
            else {
                if (selectedEntities.remove(((ObjEntity) currentClass).getName())) {
                    firePropertyChange(SELECTED_PROPERTY, null, null);
                }
            }
        }
        if (currentClass instanceof Embeddable) {
            if (selectedFlag) {
                if (selectedEmbeddables.add(((Embeddable) currentClass).getClassName())) {
                    firePropertyChange(SELECTED_PROPERTY, null, null);
                }
            }
            else {
                if (selectedEmbeddables
                        .remove(((Embeddable) currentClass).getClassName())) {
                    firePropertyChange(SELECTED_PROPERTY, null, null);
                }
            }
        }
    }

    public Object getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(Object currentClass) {
        this.currentClass = currentClass;
    }

    public Collection<DataMap> getDataMaps() {
        return dataMaps;
    }

    public JLabel getItemName(Object obj) {
        String className;
        Icon icon;
        if (obj instanceof Embeddable) {
            className = ((Embeddable) obj).getClassName();
            icon = CellRenderers.iconForObject(new Embeddable());
        } else {
            className = ((ObjEntity) obj).getName();
            icon = CellRenderers.iconForObject(new ObjEntity());
        }
        JLabel labelIcon = new JLabel();
        labelIcon.setIcon(icon);
        labelIcon.setVisible(true);
        labelIcon.setText(className);
        return labelIcon;
    }
}
