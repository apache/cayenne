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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.collections.Predicate;

/**
 * A base superclass of a top controller for the code generator. Defines all common model
 * parts used in class generation.
 * 
 * @author Andrus Adamchik
 */
public abstract class CodeGeneratorControllerBase extends CayenneController {

    public static final String SELECTED_PROPERTY = "selected";

    protected DataMap dataMap;

    protected ValidationResult validation;

    protected List entities;
    protected Set selectedEntities;

    protected transient ObjEntity currentEntity;

    public CodeGeneratorControllerBase(CayenneController parent, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.entities = new ArrayList(dataMap.getObjEntities());
        this.selectedEntities = new HashSet();
    }

    public abstract Component getView();

    public void validate(GeneratorController validator) {

        ValidationResult validationBuffer = new ValidationResult();

        if (validator != null) {
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                validator.validateEntity(validationBuffer, (ObjEntity) it.next(), false);
            }
        }

        this.validation = validationBuffer;
    }

    public boolean updateSelection(Predicate predicate) {

        boolean modified = false;

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();

            boolean select = predicate.evaluate(entity);

            if (select) {
                if (selectedEntities.add(entity.getName())) {
                    modified = true;
                }
            }
            else {
                if (selectedEntities.remove(entity.getName())) {
                    modified = true;
                }
            }
        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }

    public Collection<Embeddable> getSelectedEmbeddables() {
        // TODO: andrus, 12/9/2007 - until Modeler filtering of embeddables is
        // implemented, show all embeddables we have
        return dataMap.getEmbeddables();
    }

    public List<ObjEntity> getSelectedEntities() {
        List<ObjEntity> selected = new ArrayList<ObjEntity>(selectedEntities.size());

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity e = (ObjEntity) it.next();
            if (selectedEntities.contains(e.getName())) {
                selected.add(e);
            }
        }

        return selected;
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public List getEntities() {
        return entities;
    }

    /**
     * Returns the first encountered validation problem for an antity matching the name or
     * null if the entity is valid or the entity is not present.
     */
    public String getProblem(String entityName) {

        if (validation == null) {
            return null;
        }

        List failures = validation.getFailures(entityName);
        if (failures.isEmpty()) {
            return null;
        }

        return ((ValidationFailure) failures.get(0)).getDescription();
    }

    public boolean isSelected() {
        return currentEntity != null
                ? selectedEntities.contains(currentEntity.getName())
                : false;
    }

    public void setSelected(boolean selectedFlag) {
        if (currentEntity == null) {
            return;
        }

        if (selectedFlag) {
            if (selectedEntities.add(currentEntity.getName())) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
        else {
            if (selectedEntities.remove(currentEntity.getName())) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
    }

    public ObjEntity getCurrentEntity() {
        return currentEntity;
    }

    public void setCurrentEntity(ObjEntity currentEntity) {
        this.currentEntity = currentEntity;
    }

    public DataMap getDataMap() {
        return dataMap;
    }
}
