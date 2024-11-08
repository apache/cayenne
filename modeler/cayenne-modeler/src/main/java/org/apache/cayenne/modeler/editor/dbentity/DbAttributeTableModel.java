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

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.extension.info.ObjectInfo;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Model for DbEntity attributes. Allows adding/removing attributes, modifying types and names.
 */
public class DbAttributeTableModel extends CayenneTableModel<DbAttribute> {

    // Columns
    private static final int DB_ATTRIBUTE_NAME = 0;
    private static final int DB_ATTRIBUTE_TYPE = 1;
    private static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
    private static final int DB_ATTRIBUTE_MANDATORY = 3;
    private static final int DB_ATTRIBUTE_MAX = 4;
    private static final int DB_ATTRIBUTE_SCALE = 5;
    private static final int DB_ATTRIBUTE_COMMENT = 6;

    protected DbEntity entity;

    public DbAttributeTableModel(DbEntity entity, ProjectController mediator,
                                 Object eventSource) {
        this(entity, mediator, eventSource, new ArrayList<>(entity.getAttributes()));
    }

    public DbAttributeTableModel(DbEntity entity, ProjectController mediator,
                                 Object eventSource, List<DbAttribute> objectList) {
        super(mediator, eventSource, objectList);
        this.entity = entity;
    }

    public int nameColumnInd() {
        return DB_ATTRIBUTE_NAME;
    }

    public int typeColumnInd() {
        return DB_ATTRIBUTE_TYPE;
    }

    public int lengthColumnId() {
        return DB_ATTRIBUTE_MAX;
    }

    public int scaleColumnId() {
        return DB_ATTRIBUTE_SCALE;
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
        return 7;
    }

    public DbAttribute getAttribute(int row) {
        return (row >= 0 && row < objectList.size())
                ? objectList.get(row)
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
            case DB_ATTRIBUTE_COMMENT:
                return "Comment";
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
            case DB_ATTRIBUTE_COMMENT:
                return getComment(attr);
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
                attr.getEntity().dbAttributeChanged(e);
                
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
            case DB_ATTRIBUTE_COMMENT:
                setComment((String) newVal, attr);
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

    public String getComment(DbAttribute attr) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), attr, ObjectInfo.COMMENT);
    }

    public void setMaxLength(String newVal, DbAttribute attr) {
        if (newVal == null || newVal.trim().length() <= 0) {
            attr.setMaxLength(-1);
        } else {
            try {
                attr.setMaxLength(Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid Max Length (" + newVal + "), only numbers are allowed",
                        "Invalid Maximum Length",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setAttributeType(String newVal, DbAttribute attr) {
        attr.setType(TypesMapping.getSqlTypeByName(newVal));
    }

    public void setScale(String newVal, DbAttribute attr) {
        if (newVal == null || newVal.trim().length() <= 0) {
            attr.setScale(-1);
        } else {
            try {
                attr.setScale(Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid precision (" + newVal + "), only numbers are allowed.",
                        "Invalid Precision Value",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean setPrimaryKey(Boolean newVal, DbAttribute attr, int row) {

        boolean flag = newVal;

        // when PK is unset, we need to fix some derived flags
        if (!flag) {

            attr.setGenerated(false);

            Collection<DbRelationship> relationships = ProjectUtil
                    .getRelationshipsUsingAttributeAsTarget(attr);
            relationships
                    .addAll(ProjectUtil.getRelationshipsUsingAttributeAsSource(attr));

            if (relationships.size() > 0) {
                relationships.removeIf(relationship -> !relationship.isFK());

                // filtered only those that are isFk
                if (relationships.size() > 0) {
                    StringBuilder message = new StringBuilder("Removing an attribute can affect the following relationships:\n");
                    for (DbRelationship relationship : relationships) {
                        message.append(relationship.getName()).append("\n");
                    }
                    message.append("It would be a good idea to check them after making the change. Continue?");

                    int answer = JOptionPane.showConfirmDialog(
                            Application.getFrame(),
                            message.toString(),
                            "Warning",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (answer != JOptionPane.OK_OPTION) {
                        // no action needed
                        return false;
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
        attr.setMandatory(newVal);
    }

    public void setGenerated(Boolean newVal, DbAttribute attr) {
        attr.setGenerated(newVal);
    }

    public void setComment(String newVal, DbAttribute attr) {
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), attr, ObjectInfo.COMMENT, newVal);
    }

    public boolean isCellEditable(int row, int col) {
        DbAttribute attrib = getAttribute(row);
        if (null == attrib) {
            return false;
        } else if (col == mandatoryColumnInd()) {
            return !attrib.isPrimaryKey();
        }
        return true;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return sortCol != DB_ATTRIBUTE_COMMENT;
    }

    @Override
    public void sortByColumn(int sortCol, boolean isAscent) {
        switch (sortCol) {
            case DB_ATTRIBUTE_NAME:
                sortByElementProperty("name", isAscent);
                break;
            case DB_ATTRIBUTE_TYPE:
                objectList.sort((o1, o2) -> {
                    if (o1 == o2) {
                        return 0;
                    } else if (o1 == null) {
                        return -1;
                    } else if (o2 == null) {
                        return 1;
                    }

                    String attrType1 = getAttributeType(o1);
                    String attrType2 = getAttributeType(o2);

                    return (attrType1 == null) ? -1
                            : (attrType2 == null) ? 1 : attrType1.compareTo(attrType2);
                });
                if (!isAscent) {
                    Collections.reverse(objectList);
                }
                break;
            case DB_ATTRIBUTE_PRIMARY_KEY:
                sortByElementProperty("primaryKey", isAscent);
                break;
            case DB_ATTRIBUTE_SCALE:
                sortByElementProperty("scale", isAscent);
                break;
            case DB_ATTRIBUTE_MANDATORY:
                sortByElementProperty("mandatory", isAscent);
                break;
            case DB_ATTRIBUTE_MAX:
                sortByElementProperty("maxLength", isAscent);
                break;
        }
    }
}
