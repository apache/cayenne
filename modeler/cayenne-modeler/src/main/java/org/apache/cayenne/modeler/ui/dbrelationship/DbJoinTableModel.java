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

package org.apache.cayenne.modeler.ui.dbrelationship;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.table.CMTableModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DbJoinTableModel extends CMTableModel<DbJoin> {

    static final int SOURCE = 0;
    static final int TARGET = 1;

    private final DbRelationship relationship;
    private final DbEntity source;
    private final DbEntity target;

    public DbJoinTableModel(
            DbRelationship relationship,
            ProjectSession session,
            Object src,
            List<DbJoin> joins,
            DbEntity target) {

        super(session, src, copyJoins(relationship, joins));
        this.relationship = relationship;
        this.source = relationship.getSourceEntity();
        this.target = target;
    }

    private static List<DbJoin> copyJoins(DbRelationship relationship, List<DbJoin> joins) {
        return joins.stream()
                .map(j -> new DbJoin(relationship, j.getSourceName(), j.getTargetName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Class<?> getElementsClass() {
        return DbJoin.class;
    }

    public void commit() {
        // drop empty rows added by the user but never filled in
        relationship.setJoins(objectList.stream()
                .filter(j -> j.getSourceName() != null || j.getTargetName() != null)
                .toList());
    }

    /**
     * Same check as {@link DbRelationship#isValidForDepPk()}, but over the uncommitted joins and target
     * entity of this table.
     */
    public boolean isValidForDepPk() {
        if (objectList.isEmpty()) {
            return false;
        }

        for (DbJoin join : objectList) {
            DbAttribute sourceAttribute = source != null && join.getSourceName() != null
                    ? source.getAttribute(join.getSourceName())
                    : null;
            DbAttribute targetAttribute = target != null && join.getTargetName() != null
                    ? target.getAttribute(join.getTargetName())
                    : null;

            if (targetAttribute != null && !targetAttribute.isPrimaryKey()
                    || sourceAttribute != null && !sourceAttribute.isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        if (column == SOURCE)
            return source.getName();
        else if (column == TARGET)
            return target != null ? target.getName() : "";
        else
            return "";
    }

    public DbJoin getJoin(int row) {
        return (row >= 0 && row < objectList.size())
                ? objectList.get(row)
                : null;
    }

    public Object getValueAt(int row, int column) {
        DbJoin join = getJoin(row);
        if (join == null) {
            return null;
        }

        if (column == SOURCE) {
            return join.getSourceName();
        } else if (column == TARGET) {
            return join.getTargetName();
        } else {
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
        } else if (column == TARGET) {
            if (target == null || target.getAttribute(value) == null) {
                value = null;
            }

            join.setTargetName(value);
        }

        fireTableRowsUpdated(row, row);
    }

    public boolean isCellEditable(int row, int col) {
        if (col == SOURCE) {
            return source != null;
        } else if (col == TARGET) {
            return target != null;
        }

        return false;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

    @Override
    public void sortByColumn(int sortCol, boolean isAscent) {
        switch (sortCol) {
            case SOURCE:
                sortByElementProperty("sourceName", isAscent);
                break;
            case TARGET:
                sortByElementProperty("targetName", isAscent);
                break;
        }
    }
}
