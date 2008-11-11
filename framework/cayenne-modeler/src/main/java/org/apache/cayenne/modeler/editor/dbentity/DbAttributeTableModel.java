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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;

/**
 * Model for DbEntity attributes. Allows adding/removing attributes, modifying types and
 * names.
 * 
 */
public class DbAttributeTableModel extends CayenneTableModel {

    // Columns
    private static final int DB_ATTRIBUTE_NAME = 0;
    private static final int DB_ATTRIBUTE_TYPE = 1;
    private static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
    private static final int DB_ATTRIBUTE_MANDATORY = 3;
    private static final int DB_ATTRIBUTE_MAX = 4;
    private static final int DB_ATTRIBUTE_SCALE = 5;

    protected DbEntity entity;

    public DbAttributeTableModel(DbEntity entity, ProjectController mediator,
            Object eventSource) {
        this(entity, mediator, eventSource, new ArrayList<DbAttribute>(entity
                .getAttributes()));
        this.entity = entity;
    }

    public DbAttributeTableModel(DbEntity entity, ProjectController mediator,
            Object eventSource, List<DbAttribute> objectList) {
        super(mediator, eventSource, objectList);
    }

    public int nameColumnInd() {
        return DB_ATTRIBUTE_NAME;
    }

    public int typeColumnInd() {
        return DB_ATTRIBUTE_TYPE;
    }

    public int mandatoryColumnInd() {
        return DB_ATTRIBUTE_MANDATORY;
    }

    /**
     * Returns DbAttribute class.
     */
    @Override
    public Class<?> getElementsClass() {
        return DbAttribute.class;
    }

    /**
     * Returns the number of columns in the table.
     */
    public int getColumnCount() {
        return 6;
    }

    public DbAttribute getAttribute(int row) {
        return (row >= 0 && row < objectList.size())
                ? (DbAttribute) objectList.get(row)
                : null;
    }

    public String getColumnName(int col) {
        switch (col) {
            case DB_ATTRIBUTE_NAME:
                return "Name";
            case DB_ATTRIBUTE_TYPE:
                return "Type";
            case DB_ATTRIBUTE_PRIMARY_KEY:
                return "PK";
            case DB_ATTRIBUTE_SCALE:
                return "Scale";
            case DB_ATTRIBUTE_MANDATORY:
                return "Mandatory";
            case DB_ATTRIBUTE_MAX:
                return "Max Length";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
            case DB_ATTRIBUTE_PRIMARY_KEY:
            case DB_ATTRIBUTE_MANDATORY:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public Object getValueAt(int row, int column) {
        DbAttribute attr = getAttribute(row);

        if (attr == null) {
            return "";
        }

        switch (column) {
            case DB_ATTRIBUTE_NAME:
                return getAttributeName(attr);
            case DB_ATTRIBUTE_TYPE:
                return getAttributeType(attr);
            case DB_ATTRIBUTE_PRIMARY_KEY:
                return isPrimaryKey(attr);
            case DB_ATTRIBUTE_SCALE:
                return getScale(attr);
            case DB_ATTRIBUTE_MANDATORY:
                return isMandatory(attr);
            case DB_ATTRIBUTE_MAX:
                return getMaxLength(attr);
            default:
                return "";
        }
    }

    public void setUpdatedValueAt(Object newVal, int row, int col) {
        DbAttribute attr = getAttribute(row);
        if (attr == null) {
            return;
        }

        AttributeEvent e = new AttributeEvent(eventSource, attr, entity);

        switch (col) {
            case DB_ATTRIBUTE_NAME:
                e.setOldName(attr.getName());
                attr.setName((String) newVal);
                ((DbEntity) attr.getEntity()).dbAttributeChanged(e);
                
                fireTableCellUpdated(row, col);
                break;
            case DB_ATTRIBUTE_TYPE:
                setAttributeType((String) newVal, attr);
                break;
            case DB_ATTRIBUTE_PRIMARY_KEY:
                if (!setPrimaryKey(((Boolean) newVal), attr, row)) {
                    return;
                }
                break;
            case DB_ATTRIBUTE_SCALE:
                setScale((String) newVal, attr);
                break;
            case DB_ATTRIBUTE_MANDATORY:
                setMandatory((Boolean) newVal, attr);
                break;
            case DB_ATTRIBUTE_MAX:
                setMaxLength((String) newVal, attr);
                break;
        }

        mediator.fireDbAttributeEvent(e);
    }

    public String getMaxLength(DbAttribute attr) {
        return (attr.getMaxLength() >= 0) ? String.valueOf(attr.getMaxLength()) : "";
    }

    public String getAttributeName(DbAttribute attr) {
        return attr.getName();
    }

    public String getAttributeType(DbAttribute attr) {
        return TypesMapping.getSqlNameByType(attr.getType());
    }

    public String getScale(DbAttribute attr) {
        return (attr.getScale() >= 0) ? String.valueOf(attr.getScale()) : "";
    }

    public Boolean isPrimaryKey(DbAttribute attr) {
        return (attr.isPrimaryKey()) ? Boolean.TRUE : Boolean.FALSE;
    }

    public Boolean isMandatory(DbAttribute attr) {
        return (attr.isMandatory()) ? Boolean.TRUE : Boolean.FALSE;
    }

    public void setMaxLength(String newVal, DbAttribute attr) {
        if (newVal == null || newVal.trim().length() <= 0) {
            attr.setMaxLength(-1);
        }
        else {
            try {
                attr.setMaxLength(Integer.parseInt(newVal));
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid Max Length (" + newVal + "), only numbers are allowed",
                        "Invalid Maximum Length",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    public void setAttributeType(String newVal, DbAttribute attr) {
        attr.setType(TypesMapping.getSqlTypeByName(newVal));
    }

    public void setScale(String newVal, DbAttribute attr) {
        if (newVal == null || newVal.trim().length() <= 0) {
            attr.setScale(-1);
        }
        else {
            try {
                attr.setScale(Integer.parseInt(newVal));
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid precision (" + newVal + "), only numbers are allowed.",
                        "Invalid Precision Value",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean setPrimaryKey(Boolean newVal, DbAttribute attr, int row) {

        boolean flag = newVal.booleanValue();

        // when PK is unset, we need to fix some derived flags
        if (!flag) {

            attr.setGenerated(false);

            Collection<DbRelationship> relationships = ProjectUtil
                    .getRelationshipsUsingAttributeAsTarget(attr);
            relationships
                    .addAll(ProjectUtil.getRelationshipsUsingAttributeAsSource(attr));

            if (relationships.size() > 0) {
                Iterator<DbRelationship> it = relationships.iterator();
                while (it.hasNext()) {
                    DbRelationship relationship = it.next();
                    if (!relationship.isToDependentPK()) {
                        it.remove();
                    }
                }

                // filtered only those that are to dep PK
                if (relationships.size() > 0) {
                    String message = (relationships.size() == 1)
                            ? "Fix \"To Dep PK\" relationship using this attribute?"
                            : "Fix "
                                    + relationships.size()
                                    + " \"To Dep PK\" relationships using this attribute?";

                    int answer = JOptionPane.showConfirmDialog(
                            Application.getFrame(),
                            message);
                    if (answer != JOptionPane.YES_OPTION) {
                        // no action needed
                        return false;
                    }

                    // fix target relationships
                    for (DbRelationship relationship : relationships) {
                        relationship.setToDependentPK(false);
                    }
                }
            }
        }

        attr.setPrimaryKey(flag);
        if (flag) {
            attr.setMandatory(true);
            fireTableCellUpdated(row, DB_ATTRIBUTE_MANDATORY);
        }
        return true;
    }

    public void setMandatory(Boolean newVal, DbAttribute attr) {
        attr.setMandatory(newVal.booleanValue());
    }

    public void setGenerated(Boolean newVal, DbAttribute attr) {
        attr.setGenerated(newVal.booleanValue());
    }

    public boolean isCellEditable(int row, int col) {
        DbAttribute attrib = getAttribute(row);
        if (null == attrib) {
            return false;
        }
        else if (col == mandatoryColumnInd()) {
            if (attrib.isPrimaryKey()) {
                return false;
            }
        }
        return true;
    }
}
