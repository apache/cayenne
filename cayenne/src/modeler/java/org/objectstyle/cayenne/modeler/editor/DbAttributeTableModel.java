/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;

/** 
 * Model for DbEntity attributes. Allows adding/removing 
 * attributes, modifying types and names.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbAttributeTableModel extends CayenneTableModel {

    // Columns
    private static final int DB_ATTRIBUTE_NAME = 0;
    private static final int DB_ATTRIBUTE_TYPE = 1;
    private static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
    private static final int DB_ATTRIBUTE_MANDATORY = 3;
    private static final int DB_ATTRIBUTE_MAX = 4;
    private static final int DB_ATTRIBUTE_PRECISION = 5;

    protected DbEntity entity;

    public DbAttributeTableModel(
        DbEntity entity,
        ProjectController mediator,
        Object eventSource) {
        this(entity, mediator, eventSource, new ArrayList(entity.getAttributes()));
        this.entity = entity;
    }

    public DbAttributeTableModel(
        DbEntity entity,
        ProjectController mediator,
        Object eventSource,
        java.util.List objectList) {
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
    public Class getElementsClass() {
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
            case DB_ATTRIBUTE_NAME :
                return "Name";
            case DB_ATTRIBUTE_TYPE :
                return "Type";
            case DB_ATTRIBUTE_PRIMARY_KEY :
                return "PK";
            case DB_ATTRIBUTE_PRECISION :
                return "Precision";
            case DB_ATTRIBUTE_MANDATORY :
                return "Mandatory";
            case DB_ATTRIBUTE_MAX :
                return "Max Length";
            default :
                return "";
        }
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case DB_ATTRIBUTE_PRIMARY_KEY :
            case DB_ATTRIBUTE_MANDATORY :
                return Boolean.class;
            default :
                return String.class;
        }
    }

    public Object getValueAt(int row, int column) {
        DbAttribute attr = getAttribute(row);

        if (attr == null) {
            return "";
        }

        switch (column) {
            case DB_ATTRIBUTE_NAME :
                return getAttributeName(attr);
            case DB_ATTRIBUTE_TYPE :
                return getAttributeType(attr);
            case DB_ATTRIBUTE_PRIMARY_KEY :
                return isPrimaryKey(attr);
            case DB_ATTRIBUTE_PRECISION :
                return getPrecision(attr);
            case DB_ATTRIBUTE_MANDATORY :
                return isMandatory(attr);
            case DB_ATTRIBUTE_MAX :
                return getMaxLength(attr);
            default :
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
            case DB_ATTRIBUTE_NAME :
                e.setOldName(attr.getName());
                setAttributeName((String) newVal, attr);
                fireTableCellUpdated(row, col);
                break;
            case DB_ATTRIBUTE_TYPE :
                setAttributeType((String) newVal, attr);
                break;
            case DB_ATTRIBUTE_PRIMARY_KEY :
                if (!setPrimaryKey(((Boolean) newVal), attr, row)) {
                    return;
                }
                break;
            case DB_ATTRIBUTE_PRECISION :
                setPrecision((String) newVal, attr);
                break;
            case DB_ATTRIBUTE_MANDATORY :
                setMandatory((Boolean) newVal, attr);
                break;
            case DB_ATTRIBUTE_MAX :
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

    public String getPrecision(DbAttribute attr) {
        return (attr.getPrecision() >= 0) ? String.valueOf(attr.getPrecision()) : "";
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

    public void setAttributeName(String newVal, DbAttribute attr) {
        String newName = newVal.trim();
        ProjectUtil.setAttributeName(attr, newName);
    }

    public void setAttributeType(String newVal, DbAttribute attr) {
        attr.setType(TypesMapping.getSqlTypeByName(newVal));
    }

    public void setPrecision(String newVal, DbAttribute attr) {
        if (newVal == null || newVal.trim().length() <= 0) {
            attr.setPrecision(-1);
        }
        else {
            try {
                attr.setPrecision(Integer.parseInt(newVal));
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

        // make sure "to-dep-pk" relationships are fixed when the primary key is unset.
        if (!flag) {
            Collection relationships =
                ProjectUtil.getRelationshipsUsingAttributeAsTarget(attr);
            relationships.addAll(ProjectUtil.getRelationshipsUsingAttributeAsSource(attr));

            if (relationships.size() > 0) {
                Iterator it = relationships.iterator();
                while (it.hasNext()) {
                    DbRelationship relationship = (DbRelationship) it.next();
                    if (!relationship.isToDependentPK()) {
                        it.remove();
                    }
                }

                // filtered only those that are to dep PK
                if (relationships.size() > 0) {
                    String message =
                        (relationships.size() == 1)
                            ? "Fix \"To Dep PK\" relationship using this attribute?"
                            : "Fix "
                                + relationships.size()
                                + " \"To Dep PK\" relationships using this attribute?";

                    int answer =
                        JOptionPane.showConfirmDialog(Application.getFrame(), message);
                    if (answer != JOptionPane.YES_OPTION) {
                        // no action needed
                        return false;
                    }

                    // fix target relationships
                    Iterator fixIt = relationships.iterator();
                    while (fixIt.hasNext()) {
                        DbRelationship relationship = (DbRelationship) fixIt.next();
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
