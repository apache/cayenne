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

package org.apache.cayenne.modeler.dialog;

import java.util.ArrayList;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;

/** Model for editing DbAttributePair-s. Changes in the join attributes
 *  don't take place until commit() is called. Creation of the new
 *  DbAttributes is not allowed - user should choose from the existing ones.
*/
public class DbJoinTableModel extends CayenneTableModel {

    // Columns
    static final int SOURCE = 0;
    static final int TARGET = 1;

    protected DbRelationship relationship;
    protected DbEntity source;
    protected DbEntity target;

    /** Is the table editable. */
    private boolean editable;

    public DbJoinTableModel(
        DbRelationship relationship,
        ProjectController mediator,
        Object src) {

        super(mediator, src, new ArrayList(relationship.getJoins()));
        this.relationship = relationship;
        this.source = (DbEntity) relationship.getSourceEntity();
        this.target = (DbEntity) relationship.getTargetEntity();
    }

    public DbJoinTableModel(
        DbRelationship relationship,
        ProjectController mediator,
        Object src,
        boolean editable) {

        this(relationship, mediator, src);
        this.editable = editable;
    }

    public Class getElementsClass() {
        return DbJoin.class;
    }

    /** Mode new attribute pairs from list to the DbRelationship. */
    public void commit() {
        relationship.setJoins(getObjectList());
    }

    /**
     * Returns null to disable ordering.
     */
    public String getOrderingKey() {
        return null;
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int column) {
        if (column == SOURCE)
            return "Source";
        else if (column == TARGET)
            return "Target";
        else
            return "";
    }

    public DbJoin getJoin(int row) {
        return (row >= 0 && row < objectList.size())
            ? (DbJoin) objectList.get(row)
            : null;
    }

    public Object getValueAt(int row, int column) {
        DbJoin join = getJoin(row);
        if (join == null) {
            return null;
        }

        if (column == SOURCE) {
            return join.getSourceName();
        }
        else if (column == TARGET) {
            return join.getTargetName();
        }
        else {
            return null;
        }

    }

    public void setUpdatedValueAt(Object aValue, int row, int column) {
        DbJoin join = getJoin(row);
        if (join == null) {
            return;
        }

        String value = (String) aValue;
        if (column == SOURCE) {
            if (source == null || source.getAttribute(value) == null) {
                value = null;
            }

            join.setSourceName(value);
        }
        else if (column == TARGET) {
            if (target == null || target.getAttribute(value) == null) {
                value = null;
            }

            join.setTargetName(value);
        }
        
        fireTableRowsUpdated(row, row);
    }

    public boolean isCellEditable(int row, int col) {
        if (col == SOURCE) {
            return relationship.getSourceEntity() != null && editable;
        }
        else if (col == TARGET) {
            return relationship.getTargetEntity() != null && editable;
        }

        return false;
    }
}
