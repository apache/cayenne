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

import org.apache.cayenne.map.*;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Table model to display ObjRelationships.
 * 
 */
public class ObjRelationshipTableModel extends CayenneTableModel {

    // Columns
    static final int REL_NAME = 0;
    static final int REL_TARGET = 1;
    static final int REL_SEMANTICS = 2;
    static final int REL_DELETERULE = 3;
    static final int REL_LOCKING = 4;

    protected ObjEntity entity;

    public ObjRelationshipTableModel(ObjEntity entity, ProjectController mediator,
            Object eventSource) {
        super(mediator, eventSource, new ArrayList(entity.getRelationships()));
        this.entity = entity;

        // order using local comparator
        Collections.sort(objectList, new RelationshipComparator());
    }

    @Override
    protected void orderList() {
        // NOOP
    }

    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns ObjRelationship class.
     */
    @Override
    public Class getElementsClass() {
        return ObjRelationship.class;
    }

    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case REL_NAME:
                return "Name";
            case REL_TARGET:
                return "Target";
            case REL_LOCKING:
                return "Used for Locking";
            case REL_SEMANTICS:
                return "Semantics";
            case REL_DELETERULE:
                return "Delete Rule";

            default:
                return null;
        }
    }

    @Override
    public Class getColumnClass(int col) {
        switch (col) {
            case REL_TARGET:
                return ObjEntity.class;
            case REL_LOCKING:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public ObjRelationship getRelationship(int row) {
        return (row >= 0 && row < objectList.size()) ? (ObjRelationship) objectList
                .get(row) : null;
    }

    public Object getValueAt(int row, int column) {
        ObjRelationship relationship = getRelationship(row);

        if (column == REL_NAME) {
            return relationship.getName();
        }
        else if (column == REL_TARGET) {
            return relationship.getTargetEntity();
        }
        else if (column == REL_LOCKING) {
            return relationship.isUsedForLocking() ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (column == REL_SEMANTICS) {
            String semantics = relationship.isToMany() ? "to many" : "to one";
            if (relationship.isReadOnly()) {
                semantics += ", read-only";
            }

            if (relationship.isToMany()) {
                String collection = "list";
                if (relationship.getCollectionType() != null) {
                    int dot = relationship.getCollectionType().lastIndexOf('.');
                    collection = relationship
                            .getCollectionType()
                            .substring(dot + 1)
                            .toLowerCase();
                }

                semantics += ", " + collection;
            }

            return semantics;
        }
        else if (column == REL_DELETERULE) {
            return DeleteRule.deleteRuleName(relationship.getDeleteRule());
        }
        else {
            return null;
        }
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int column) {
        ObjRelationship relationship = getRelationship(row);
        RelationshipEvent event = new RelationshipEvent(eventSource, relationship, entity);

        if (column == REL_NAME) {
            String text = (String) value;
            event.setOldName(relationship.getName());
            ProjectUtil.setRelationshipName(entity, relationship, text);
            fireTableCellUpdated(row, column);
        }
        else if (column == REL_TARGET) {
            ObjEntity target = (ObjEntity) value;
            relationship.setTargetEntity(target);
            
            /**
             * Clear existing relationships, otherwise addDbRelationship() might fail
             */
            relationship.clearDbRelationships();
            
            // now try to connect DbEntities if we can do it in one step
            if (target != null) {
                DbEntity srcDB = ((ObjEntity) relationship.getSourceEntity())
                        .getDbEntity();
                DbEntity targetDB = target.getDbEntity();
                if (srcDB != null && targetDB != null) {
                    Relationship anyConnector = srcDB.getAnyRelationship(targetDB);
                    if (anyConnector != null) {
                        relationship.addDbRelationship((DbRelationship) anyConnector);
                    }
                }
            }

            fireTableRowsUpdated(row, row);
        }
        else if (column == REL_DELETERULE) {
            relationship.setDeleteRule(DeleteRule.deleteRuleForName((String) value));
            fireTableCellUpdated(row, column);
        }
        else if (column == REL_LOCKING) {
            relationship.setUsedForLocking((value instanceof Boolean)
                    && ((Boolean) value).booleanValue());
            fireTableCellUpdated(row, column);
        }

        mediator.fireObjRelationshipEvent(event);
    }

    public void removeRow(int row) {
        if (row < 0)
            return;
        Relationship rel = getRelationship(row);
        RelationshipEvent e;
        e = new RelationshipEvent(eventSource, rel, entity, RelationshipEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
        objectList.remove(row);
        entity.removeRelationship(rel.getName());
        fireTableRowsDeleted(row, row);
    }

    private boolean isInherited(int row) {
        ObjRelationship relationship = getRelationship(row);
        return (relationship != null) ? relationship.getSourceEntity() != entity : false;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return !isInherited(row) && col != REL_SEMANTICS;
    }

    final class RelationshipComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            ObjRelationship r1 = (ObjRelationship) o1;
            ObjRelationship r2 = (ObjRelationship) o2;

            int delta = getWeight(r1) - getWeight(r2);

            return (delta != 0) ? delta : Util.nullSafeCompare(true, r1.getName(), r2
                    .getName());
        }

        private int getWeight(ObjRelationship r) {
            return r.getSourceEntity() == entity ? 1 : -1;
        }
    }
}
