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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Table model to display ObjRelationships.
 * 
 */
public class ObjRelationshipTableModel extends CayenneTableModel<ObjRelationship> {

    // Columns
    public static final int REL_NAME = 0;
    public static final int REL_TARGET = 1;
    public static final int REL_TARGET_PATH = 2;
    public static final int REL_SEMANTICS = 3;
    public static final int REL_DELETE_RULE = 4;
    public static final int REL_LOCKING = 5;
    public static final int REL_COMMENT = 6;
    public static final int COLUMN_COUNT = 7;

    private final ObjEntity entity;

    public ObjRelationshipTableModel(ObjEntity entity, ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, new ArrayList<>(entity.getRelationships()));
        this.entity = entity;

        // order using local comparator
        objectList.sort(new RelationshipComparator());
    }

    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns ObjRelationship class.
     */
    @Override
    public Class<ObjRelationship> getElementsClass() {
        return ObjRelationship.class;
    }

    public int getColumnCount() {
        return COLUMN_COUNT;
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
            case REL_DELETE_RULE:
                return "Delete Rule";
            case REL_TARGET_PATH:
                return "DbRelationship Path";
            case REL_COMMENT:
                return "Comment";
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
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
        return (row >= 0 && row < objectList.size()) ?  objectList.get(row) : null;
    }

    public Object getValueAt(int row, int column) {
        ObjRelationship relationship = getRelationship(row);

        switch (column) {
            case REL_NAME:
                return relationship.getName();
            case REL_TARGET:
                return relationship.getTargetEntity();
            case REL_LOCKING:
                return relationship.isUsedForLocking() ? Boolean.TRUE : Boolean.FALSE;
            case REL_SEMANTICS:
                return getSemantics(relationship);
            case REL_DELETE_RULE:
                return DeleteRule.deleteRuleName(relationship.getDeleteRule());
            case REL_TARGET_PATH:
                return relationship.getDbRelationshipPath();
            case REL_COMMENT:
                return getComment(relationship);
            default:
                return null;
        }
    }

    private static String getSemantics(ObjRelationship relationship) {
        StringBuilder semantics =  new StringBuilder(20);
        semantics.append(relationship.isToMany() ? "to many" : "to one");
        if (relationship.isReadOnly()) {
            semantics.append(", read-only");
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

            semantics.append(", ").append(collection);
        }
        return semantics.toString();
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int column) {
        ObjRelationship relationship = getRelationship(row);
        RelationshipEvent event = new RelationshipEvent(eventSource, relationship, entity);

        switch (column) {
            case REL_NAME:
                String text = (String) value;
                event.setOldName(relationship.getName());
                ProjectUtil.setRelationshipName(entity, relationship, text);
                fireTableCellUpdated(row, column);
                break;
            case REL_TARGET:
                ObjEntity target = (ObjEntity) value;
                relationship.setTargetEntityName(target);

                // Clear existing relationships, otherwise addDbRelationship() might fail
                relationship.clearDbRelationships();

                // now try to connect DbEntities if we can do it in one step
                if (target != null) {
                    DbEntity srcDB = relationship.getSourceEntity().getDbEntity();
                    DbEntity targetDB = target.getDbEntity();
                    if (srcDB != null && targetDB != null) {
                        DbRelationship anyConnector = srcDB.getAnyRelationship(targetDB);
                        if (anyConnector != null) {
                            relationship.addDbRelationship(anyConnector);
                        }
                    }
                }

                fireTableRowsUpdated(row, row);
                break;
            case REL_DELETE_RULE:
                relationship.setDeleteRule(DeleteRule.deleteRuleForName((String) value));
                fireTableCellUpdated(row, column);
                break;
            case REL_LOCKING:
                relationship.setUsedForLocking((value instanceof Boolean) && (Boolean) value);
                fireTableCellUpdated(row, column);
                break;
            case REL_TARGET_PATH:
                relationship.setDbRelationshipPath((String) value);
                fireTableCellUpdated(row, column);
                break;
            case REL_COMMENT:
                setComment((String)value, relationship);
                fireTableRowsUpdated(row, row);
                break;
        }

        mediator.fireObjRelationshipEvent(event);
    }

    public void removeRow(int row) {
        if (row < 0) {
            return;
        }
        ObjRelationship rel = getRelationship(row);
        RelationshipEvent e;
        e = new RelationshipEvent(eventSource, rel, entity, RelationshipEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
        objectList.remove(row);
        entity.removeRelationship(rel.getName());
        fireTableRowsDeleted(row, row);
    }

    private boolean isInherited(int row) {
        ObjRelationship relationship = getRelationship(row);
        return (relationship != null) && relationship.getSourceEntity() != entity;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return !isInherited(row) && col != REL_SEMANTICS && col != REL_TARGET;
    }

    final class RelationshipComparator implements Comparator<ObjRelationship> {
        public int compare(ObjRelationship o1, ObjRelationship o2) {
            int delta = getWeight(o1) - getWeight(o2);
            return (delta != 0) ? delta : Util.nullSafeCompare(true, o1.getName(), o2.getName());
        }

        private int getWeight(ObjRelationship r) {
            return r.getSourceEntity() == entity ? 1 : -1;
        }
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

    @Override
    public void sortByColumn(final int sortCol, boolean isAscent) {
        switch (sortCol) {
            case REL_NAME:
                sortByElementProperty("name", isAscent);
                break;
            case REL_TARGET:
                sortByElementProperty("targetEntityName", isAscent);
                break;
            case REL_LOCKING:
                sortByElementProperty("usedForLocking", isAscent);
                break;
            case REL_SEMANTICS:
            case REL_DELETE_RULE:
            case REL_TARGET_PATH:
                objectList.sort(new ObjRelationshipTableComparator(sortCol));
                if (!isAscent) {
                    Collections.reverse(objectList);
                }
                break;
            default:
                break;
        }
    }

    private static class ObjRelationshipTableComparator implements Comparator<ObjRelationship>{

        private final int sortCol;

        ObjRelationshipTableComparator(int sortCol) {
            this.sortCol = sortCol;
        }

        public int compare(ObjRelationship o1, ObjRelationship o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            switch(sortCol) {
                case REL_SEMANTICS:
                    return compareColumnsData(getSemantics(o1), getSemantics(o2));
                case REL_DELETE_RULE:
                    return compareColumnsData(DeleteRule.deleteRuleName(o1.getDeleteRule()),
                                    DeleteRule.deleteRuleName(o2.getDeleteRule()));
                case REL_TARGET_PATH:
                    return compareColumnsData(o1.getDbRelationshipPath().value(), o2.getDbRelationshipPath().value());
                default:
                    return compareColumnsData("", "");
            }
        }
    }

    private static int compareColumnsData(String value1, String value2) {
        if (value1 == null) {
            return -1;
        } else if (value2 == null) {
            return 1;
        } else {
            return value1.compareTo(value2);
        }
    }

    private String getComment(ObjRelationship rel) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), rel, ObjectInfo.COMMENT);
    }

    private void setComment(String newVal, ObjRelationship rel) {
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), rel, ObjectInfo.COMMENT, newVal);
    }
}
