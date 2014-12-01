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
package org.apache.cayenne.modeler.dialog.autorelationship;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.commons.collections.Predicate;

public class InferRelationshipsControllerBase extends CayenneController {

    public static final String SELECTED_PROPERTY = "selected";

    protected DataMap dataMap;

    protected List<InferredRelationship> inferredRelationships;

    protected List<DbEntity> entities;
    protected Set<InferredRelationship> selectedEntities;
    protected int index = 0;
    protected ObjectNameGenerator strategy;

    protected transient InferredRelationship currentEntity;
    protected transient Integer entityNumber;

    public InferRelationshipsControllerBase(CayenneController parent, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.entities = new ArrayList<DbEntity>(dataMap.getDbEntities());
        this.selectedEntities = new HashSet<InferredRelationship>();
    }

    public void setRelationships() {
        inferredRelationships = new ArrayList<InferredRelationship>();

        for (DbEntity entity : entities) {
            createRelationships(entity);
        }

        createJoins();
        createNames();
    }

    protected void createRelationships(DbEntity entity) {

        for (DbAttribute attribute : entity.getAttributes()) {

            String name = attribute.getName();
            if (name.length() < 4) {
                continue;
            }

            if (!name.substring(name.length() - 3, name.length()).equalsIgnoreCase("_ID")) {
                continue;
            }

            String baseName = name.substring(0, name.length() - 3);
            for (DbEntity targetEntity : entities) {
                // TODO: should we handle relationships to self??
                if (targetEntity == entity) {
                    continue;
                }

                if (baseName.equalsIgnoreCase(targetEntity.getName())
                        && !attribute.isPrimaryKey()
                        && !targetEntity.getAttributes().isEmpty()) {

                    if (!attribute.isForeignKey()) {
                        InferredRelationship myir = new InferredRelationship();
                        myir.setSource(entity);
                        myir.setTarget(targetEntity);
                        inferredRelationships.add(myir);
                    }
                    createReversRelationship(targetEntity, entity);
                }
            }
        }
    }

    public void createReversRelationship(DbEntity eSourse, DbEntity eTarget) {
        InferredRelationship myir = new InferredRelationship();
        for (DbRelationship relationship : eSourse.getRelationships()) {
            for (DbJoin join : relationship.getJoins()) {
                if (((DbEntity) join.getSource().getEntity()).equals(eSourse)
                        && ((DbEntity) join.getTarget().getEntity()).equals(eTarget)) {
                    return;
                }
            }
        }
        myir.setSource(eSourse);
        myir.setTarget(eTarget);
        inferredRelationships.add(myir);
    }

    public String getJoin(InferredRelationship irItem) {
        return irItem.getJoinSource().getName()
                + " : "
                + irItem.getJoinTarget().getName();
    }

    public String getToMany(InferredRelationship irItem) {
        if (irItem.isToMany()) {
            return "to many";
        }
        else {
            return "to one";
        }
    }

    protected DbAttribute getJoinAttribute(DbEntity sEntity, DbEntity tEntity) {
        if (sEntity.getAttributes().size() == 1) {
            return sEntity.getAttributes().iterator().next();
        }
        else {
            for (DbAttribute attr : sEntity.getAttributes()) {
                if (attr.getName().equalsIgnoreCase(tEntity.getName() + "_ID")) {
                    return attr;
                }
            }

            for (DbAttribute attr : sEntity.getAttributes()) {
                if ((attr.getName().equalsIgnoreCase(sEntity.getName() + "_ID"))
                        && (!attr.isPrimaryKey())) {
                    return attr;
                }
            }

            for (DbAttribute attr : sEntity.getAttributes()) {
                if (attr.isPrimaryKey()) {
                    return attr;
                }
            }
        }
        return null;
    }

    protected void createJoins() {
        Iterator<InferredRelationship> it = inferredRelationships.iterator();
        while (it.hasNext()) {
            InferredRelationship inferred = it.next();

            DbAttribute src = getJoinAttribute(inferred.getSource(), inferred.getTarget());
            if (src == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            DbAttribute target = getJoinAttribute(inferred.getTarget(), inferred
                    .getSource());
            if (target == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            inferred.setJoinSource(src);
            if (src.isPrimaryKey()) {
                inferred.setToMany(true);
            }

            inferred.setJoinTarget(target);
        }
    }

    protected void createNames() {

        ExportedKey key = null;
        for (InferredRelationship myir : inferredRelationships) {
            if (myir.getJoinSource().isPrimaryKey()) {
                key = getExportedKey(myir.getSource().getName(),
                                     myir.getJoinSource().getName(),
                                     myir.getTarget().getName(),
                                     myir.getJoinTarget().getName());
            } else {
                key = getExportedKey(myir.getTarget().getName(),
                                     myir.getJoinTarget().getName(),
                                     myir.getSource().getName(),
                                     myir.getJoinSource().getName());
            }
            myir.setName(strategy.createDbRelationshipName(key, myir.isToMany()));
        }
    }

    protected ExportedKey getExportedKey(
            String pkTable,
            String pkColumn,
            String fkTable,
            String fkColumn) {
        return new ExportedKey(pkTable, pkColumn, null, fkTable, fkColumn, null, (short) 1);
    }

    public List<InferredRelationship> getSelectedEntities() {
        List<InferredRelationship> selected = new ArrayList<InferredRelationship>(
                selectedEntities.size());

        for (InferredRelationship entity : inferredRelationships) {
            if (selectedEntities.contains(entity)) {
                selected.add(entity);
            }
        }

        return selected;
    }

    public boolean updateSelection(Predicate predicate) {
        boolean modified = false;

        for (InferredRelationship entity : inferredRelationships) {
            boolean select = predicate.evaluate(entity);

            if (select) {
                if (selectedEntities.add(entity)) {
                    modified = true;
                }
            }
            else {
                if (selectedEntities.remove(entity)) {
                    modified = true;
                }
            }
        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }

    public boolean isSelected() {
        return currentEntity != null ? selectedEntities.contains(currentEntity) : false;
    }

    public void setSelected(boolean selectedFlag) {
        if (currentEntity == null) {
            return;
        }

        if (selectedFlag) {
            if (selectedEntities.add(currentEntity)) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
        else {
            if (selectedEntities.remove(currentEntity)) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public List getEntities() {
        return inferredRelationships;
    }

    public InferredRelationship getCurrentEntity() {
        return currentEntity;
    }

    public void setCurrentEntity(InferredRelationship currentEntity) {
        this.currentEntity = currentEntity;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    @Override
    public Component getView() {
        return null;
    }

    public void setNamingStrategy(ObjectNameGenerator namestr) {
        strategy = namestr;
    }

}
