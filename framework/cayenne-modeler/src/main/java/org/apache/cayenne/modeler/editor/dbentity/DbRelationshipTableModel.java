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

package org.apache.cayenne.modeler.editor.dbentity;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;

/**
 * Table model for DbRelationship table.
 * 
 */
public class DbRelationshipTableModel extends CayenneTableModel {

    // Columns
    static final int NAME = 0;
    static final int TARGET = 1;
    static final int TO_DEPENDENT_KEY = 2;
    static final int CARDINALITY = 3;

    protected DbEntity entity;

    public DbRelationshipTableModel(DbEntity entity, ProjectController mediator,
            Object eventSource) {

        super(mediator, eventSource, new ArrayList(entity.getRelationships()));
        this.entity = entity;
    }

    /**
     * Returns DbRelationship class.
     */
    public Class getElementsClass() {
        return DbRelationship.class;
    }

    public int getColumnCount() {
        return 4;
    }

    public String getColumnName(int col) {
        switch (col) {
            case NAME:
                return "Name";
            case TARGET:
                return "Target";
            case TO_DEPENDENT_KEY:
                return "To Dep PK";
            case CARDINALITY:
                return "To Many";
            default:
                return null;
        }
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case TARGET:
                return DbEntity.class;
            case TO_DEPENDENT_KEY:
            case CARDINALITY:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public DbRelationship getRelationship(int row) {
        return (row >= 0 && row < objectList.size()) ? (DbRelationship) objectList
                .get(row) : null;
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
            case TO_DEPENDENT_KEY:
                return rel.isToDependentPK() ? Boolean.TRUE : Boolean.FALSE;
            case CARDINALITY:
                return rel.isToMany() ? Boolean.TRUE : Boolean.FALSE;
            default:
                return null;
        }
    }

    public void setUpdatedValueAt(Object aValue, int row, int column) {

        DbRelationship rel = getRelationship(row);
        // If name column
        if (column == NAME) {
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity, rel
                    .getName());
            rel.setName((String) aValue);
            mediator.fireDbRelationshipEvent(e);
            fireTableCellUpdated(row, column);
        }
        // If target column
        else if (column == TARGET) {
            DbEntity target = (DbEntity) aValue;

            // clear joins...
            rel.removeAllJoins();
            rel.setTargetEntity(target);

            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
            mediator.fireDbRelationshipEvent(e);
        }
        else if (column == TO_DEPENDENT_KEY) {
            boolean flag = ((Boolean) aValue).booleanValue();

            // make sure reverse relationship "to-dep-pk" is unset.
            if (flag) {
                DbRelationship reverse = rel.getReverseRelationship();
                if (reverse != null && reverse.isToDependentPK()) {
                    String message = "Unset reverse relationship's \"To Dep PK\" setting?";
                    int answer = JOptionPane.showConfirmDialog(
                            Application.getFrame(),
                            message);
                    if (answer != JOptionPane.YES_OPTION) {
                        // no action needed
                        return;
                    }

                    // unset reverse
                    reverse.setToDependentPK(false);
                }
            }

            rel.setToDependentPK(flag);
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
            mediator.fireDbRelationshipEvent(e);
        }
        else if (column == CARDINALITY) {
            Boolean temp = (Boolean) aValue;
            rel.setToMany(temp.booleanValue());
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
            mediator.fireDbRelationshipEvent(e);

            updateDependentObjRelationships(rel);
        }
        fireTableRowsUpdated(row, row);
    }

    /**
     * Relationship just needs to be removed from the model. It is already removed from
     * the DataMap.
     */
    void removeRelationship(Relationship rel) {
        objectList.remove(rel);
        fireTableDataChanged();
    }

    void updateDependentObjRelationships(DbRelationship relationship) {

        DataDomain domain = mediator.getCurrentDataDomain();
        if (domain != null) {

            for (ObjEntity entity : domain.getEntityResolver().getObjEntities()) {
                for (ObjRelationship objRelationship : entity.getRelationships()) {

                    for (DbRelationship dbRelationship : objRelationship
                            .getDbRelationships()) {
                        if (dbRelationship == relationship) {
                            objRelationship.recalculateToManyValue();
                            objRelationship.recalculateReadOnlyValue();
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean isCellEditable(int row, int col) {
        DbRelationship rel = getRelationship(row);
        if (rel == null) {
            return false;
        }
        else if (col == TO_DEPENDENT_KEY) {
            return rel.isValidForDepPk();
        }
        return true;
    }
}
