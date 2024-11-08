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

package org.apache.cayenne.modeler.editor.dbentity;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.extension.info.ObjectInfo;

/**
 * Table model for DbRelationship table.
 * 
 */
public class DbRelationshipTableModel extends CayenneTableModel<DbRelationship> {

    // Columns
    static final int NAME = 0;
    static final int TARGET = 1;
    static final int FK = 2;
    static final int CARDINALITY = 3;
    static final int COMMENTS = 4;

    protected DbEntity entity;

    public DbRelationshipTableModel(DbEntity entity, ProjectController mediator,
                                    Object eventSource) {

        super(mediator, eventSource, new ArrayList<>(entity.getRelationships()));
        this.entity = entity;
    }

    /**
     * Returns DbRelationship class.
     */
    public Class getElementsClass() {
        return DbRelationship.class;
    }

    public int getColumnCount() {
        return 5;
    }

    public String getColumnName(int col) {
        switch (col) {
            case NAME:
                return "Name";
            case TARGET:
                return "Target";
            case FK:
                return "FK";
            case CARDINALITY:
                return "To Many";
            case COMMENTS:
                return "Comment";
            default:
                return null;
        }
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case TARGET:
                return DbEntity.class;
            case FK:
            case CARDINALITY:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public DbRelationship getRelationship(int row) {
        return (row >= 0 && row < objectList.size()) ? objectList.get(row) : null;
    }

    public Object getValueAt(int row, int col) {
        DbRelationship rel = getRelationship(row);
        if (rel == null) {
            return null;
        }

        switch (col) {
            case NAME:
                return rel.getName();
            case TARGET:
                return rel.getTargetEntity();
            case FK:
                return rel.isFK() ? Boolean.TRUE : Boolean.FALSE;
            case CARDINALITY:
                return rel.isToMany() ? Boolean.TRUE : Boolean.FALSE;
            case COMMENTS:
                return getComment(rel);
            default:
                return null;
        }
    }

    private String getComment(DbRelationship rel) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), rel, ObjectInfo.COMMENT);
    }

    private void setComment(String newVal, DbRelationship rel) {
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), rel, ObjectInfo.COMMENT, newVal);
    }

    public void setUpdatedValueAt(Object aValue, int row, int column) {

        DbRelationship rel = getRelationship(row);
        // If name column
        if (column == NAME) {
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity, rel.getName());
            rel.setName((String) aValue);
            mediator.fireDbRelationshipEvent(e);
            fireTableCellUpdated(row, column);
        } else if (column == FK) {
            boolean flag = (Boolean) aValue;

            // set/unset FK at both ends of relationship.
            DbRelationship reverse = rel.getReverseRelationship();
            if (reverse != null) {
                boolean isOKAnswer = JOptionPane.showConfirmDialog(Application.getFrame()
                        , flag ? "Foreign key will be unset in reverse relationship" : "Foreign key will be set in reverse relationship"
                        , "Warning"
                        , JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
                if (isOKAnswer) {
                    rel.setFK(flag);
                    reverse.setFK(!flag);
                } else {
                    rel.setFK(!flag);
                    reverse.setFK(flag);
                }
            } else {
                rel.setFK(flag);
            }
            mediator.fireDbRelationshipEvent(new RelationshipEvent(eventSource, rel, entity));
        } else if (column == CARDINALITY) {
            rel.setToMany((Boolean) aValue);
            mediator.fireDbRelationshipEvent(new RelationshipEvent(eventSource, rel, entity));

            updateDependentObjRelationships(rel);
        } else if (column == COMMENTS) {
            setComment((String) aValue, rel);
            mediator.fireDbRelationshipEvent(new RelationshipEvent(eventSource, rel, entity));
        }
        fireTableRowsUpdated(row, row);
    }

    /**
     * Relationship just needs to be removed from the model. It is already removed from
     * the DataMap.
     */
    void removeRelationship(DbRelationship rel) {
        objectList.remove(rel);
        fireTableDataChanged();
    }

    void updateDependentObjRelationships(DbRelationship relationship) {

        Collection<ObjRelationship> objRelationshipsForDbRelationship = ProjectUtil
                .findObjRelationshipsForDbRelationship(mediator, relationship);
        for (ObjRelationship objRelationship : objRelationshipsForDbRelationship) {
            objRelationship.recalculateToManyValue();
        }
    }

    public boolean isCellEditable(int row, int col) {
        DbRelationship rel = getRelationship(row);
        if (rel == null) {
            return false;
        } else if (col == TARGET) {
            return false;
        } else if (col == FK) {
            return rel.isValidForFk();
        }
        return true;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

    @Override
    public void sortByColumn(int sortCol, boolean isAscent) {
        switch (sortCol) {
            case NAME:
                sortByElementProperty("name", isAscent);
                break;
            case TARGET:
                sortByElementProperty("targetEntityName", isAscent);
                break;
            case FK:
                sortByElementProperty("FK", isAscent);
                break;
            case CARDINALITY:
                sortByElementProperty("toMany", isAscent);
                break;
        }
    }
}
