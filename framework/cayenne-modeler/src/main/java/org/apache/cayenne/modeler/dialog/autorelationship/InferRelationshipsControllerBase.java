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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.commons.collections.Predicate;

public class InferRelationshipsControllerBase extends CayenneController {

    public static final String SELECTED_PROPERTY = "selected";

    protected DataMap dataMap;

    protected List<InferRelationships> ir;

    protected List<DbEntity> entities;
    protected Set<InferRelationships> selectedEntities;
    protected int index = 0;
    protected NamingStrategy strategy;

    protected transient InferRelationships currentEntity;
    protected transient Integer entityNumber;

    public InferRelationshipsControllerBase(CayenneController parent, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.entities = new ArrayList(dataMap.getDbEntities());
        this.selectedEntities = new HashSet();
    }

    public void setRelationships() {
        ir = new ArrayList<InferRelationships>();

        for (DbEntity entity : entities) {
            createRelationships(entity);
        }

        createJoin();
        createName();
    }

    public void createRelationships(DbEntity entity) {

        for (DbAttribute attribute : entity.getAttributes()) {

            for (DbEntity targetEntity : entities) {
                // TODO: should we handle relationships to self??
                if (targetEntity == entity) {
                    continue;
                }

                if (attribute.getName().equalsIgnoreCase(targetEntity.getName() + "_ID")
                        && !attribute.isPrimaryKey()
                        && !targetEntity.getAttributes().isEmpty()) {

                    if (!attribute.isForeignKey()) {
                        InferRelationships myir = new InferRelationships();
                        myir.setSource(entity);
                        myir.setTarget(targetEntity);
                        ir.add(myir);
                    }
                    createReversRelationship(targetEntity, entity);
                }
            }
        }
    }

    public void createReversRelationship(DbEntity eSourse, DbEntity eTarget) {
        InferRelationships myir = new InferRelationships();
        for (Relationship relationship : eSourse.getRelationships()) {
            for (DbJoin join : ((DbRelationship) relationship).getJoins()) {
                if (((DbEntity) join.getSource().getEntity()).equals(eSourse)
                        && ((DbEntity) join.getTarget().getEntity()).equals(eTarget)) {
                    return;
                }
            }
        }
        myir.setSource(eSourse);
        myir.setTarget(eTarget);
        ir.add(myir);
    }

    public String getJoin(InferRelationships irItem) {
        return irItem.getJoinSource().getName()
                + " : "
                + irItem.getJoinTarget().getName();
    }

    public String getToMany(InferRelationships irItem) {
        if (irItem.isToMany()) {
            return "to many";
        }
        else {
            return "to one";
        }
    }

    public DbAttribute getJoinAttribute(DbEntity sEntity, DbEntity tEntity) {
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
                    return (DbAttribute) sEntity.getAttribute(attr.getName());
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

    public void createJoin() {
        Iterator<InferRelationships> it = ir.iterator();
        while (it.hasNext()) {
            InferRelationships myir = it.next();

            DbAttribute src = getJoinAttribute(myir.getSource(), myir.getTarget());
            if (src == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            DbAttribute target = getJoinAttribute(myir.getTarget(), myir.getSource());
            if (target == null) {
                // TODO: andrus 03/28/2010 this is pretty inefficient I guess... We should
                // check for this condition earlier. See CAY-1405 for the map that caused
                // this issue
                it.remove();
                continue;
            }

            myir.setJoinSource(src);
            if (src.isPrimaryKey()) {
                myir.setToMany(true);
            }

            myir.setJoinTarget(target);
        }
    }

    public void createName() {

        ExportedKey key = null;
        for (InferRelationships myir : ir) {
            if (myir.getJoinSource().isPrimaryKey()) {
                key = getExportedKey(myir.getSource().getName(), myir
                        .getJoinSource()
                        .getName(), myir.getTarget().getName(), myir
                        .getJoinTarget()
                        .getName());
            }
            else {
                key = getExportedKey(myir.getTarget().getName(), myir
                        .getJoinTarget()
                        .getName(), myir.getSource().getName(), myir
                        .getJoinSource()
                        .getName());
            }
            myir.setName(strategy.createDbRelationshipName(key, myir.isToMany()));
        }
    }

    public ExportedKey getExportedKey(
            String pkTable,
            String pkColumn,
            String fkTable,
            String fkColumn) {
        return new ExportedKey(pkTable, pkColumn, null, fkTable, fkColumn, null);
    }

    public List<InferRelationships> getSelectedEntities() {
        List<InferRelationships> selected = new ArrayList<InferRelationships>(
                selectedEntities.size());

        for (InferRelationships entity : ir) {
            if (selectedEntities.contains(entity)) {
                selected.add(entity);
            }
        }

        return selected;
    }

    public boolean updateSelection(Predicate predicate) {
        boolean modified = false;

        for (InferRelationships entity : ir) {
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
        return ir;
    }

    public InferRelationships getCurrentEntity() {
        return currentEntity;
    }

    public void setCurrentEntity(InferRelationships currentEntity) {
        this.currentEntity = currentEntity;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    @Override
    public Component getView() {
        return null;
    }

    public void setNamingStrategy(NamingStrategy namestr) {
        strategy = namestr;
    }

}
