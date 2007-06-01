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
import java.util.Collections;
import java.util.Comparator;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.util.Util;

/** 
 * Table model to display ObjRelationships. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjRelationshipTableModel extends CayenneTableModel {
    // Columns
    static final int REL_NAME = 0;
    static final int REL_TARGET = 1;
    static final int REL_CARDINALITY = 2;
    static final int REL_DELETERULE = 3;
    static final int REL_LOCKING = 4;

    protected ObjEntity entity;

    public ObjRelationshipTableModel(
        ObjEntity entity,
        ProjectController mediator,
        Object eventSource) {
        super(mediator, eventSource, new ArrayList(entity.getRelationships()));
        this.entity = entity;

        // order using local comparator
        Collections.sort(objectList, new RelationshipComparator());
    }

    protected void orderList() {
        // NOOP
    }

    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns ObjRelationship class.
     */
    public Class getElementsClass() {
        return ObjRelationship.class;
    }

    public int getColumnCount() {
        return 5;
    }

    public String getColumnName(int column) {
        switch (column) {
            case REL_NAME :
                return "Name";
            case REL_TARGET :
                return "Target";
            case REL_LOCKING :
                return "Used for Locking";
            case REL_CARDINALITY :
                return "To Many";
            case REL_DELETERULE :
                return "Delete Rule";

            default :
                return null;
        }
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case REL_TARGET :
                return ObjEntity.class;
            case REL_CARDINALITY :
            case REL_LOCKING :
                return Boolean.class;
            default :
                return String.class;
        }
    }

    public ObjRelationship getRelationship(int row) {
        return (row >= 0 && row < objectList.size())
            ? (ObjRelationship) objectList.get(row)
            : null;
    }

    public Object getValueAt(int row, int column) {
        ObjRelationship rel = getRelationship(row);

        if (column == REL_NAME) {
            return rel.getName();
        }
        else if (column == REL_TARGET) {
            return rel.getTargetEntity();
        }
        else if (column == REL_LOCKING) {
            return rel.isUsedForLocking() ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (column == REL_CARDINALITY) {
            return rel.isToMany() ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (column == REL_DELETERULE) {
            return DeleteRule.deleteRuleName(rel.getDeleteRule());
        }
        else {
            return null;
        }
    }

    public void setUpdatedValueAt(Object value, int row, int column) {
        ObjRelationship relationship = getRelationship(row);
        RelationshipEvent event =
            new RelationshipEvent(eventSource, relationship, entity);

        if (column == REL_NAME) {
            String text = (String) value;
            event.setOldName(relationship.getName());
            ProjectUtil.setRelationshipName(entity, relationship, text);
            fireTableCellUpdated(row, column);
        }
        else if (column == REL_TARGET) {
            ObjEntity target = (ObjEntity) value;
            relationship.setTargetEntity(target);

            // now try to connect DbEntities if we can do it in one step
            if (target != null) {
                DbEntity srcDB =
                    ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
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
            relationship.setUsedForLocking(
                (value instanceof Boolean) && ((Boolean) value).booleanValue());
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

    public boolean isCellEditable(int row, int col) {
        return !isInherited(row) && col != REL_CARDINALITY;
    }

    final class RelationshipComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            ObjRelationship r1 = (ObjRelationship) o1;
            ObjRelationship r2 = (ObjRelationship) o2;

            int delta = getWeight(r1) - getWeight(r2);

            return (delta != 0)
                ? delta
                : Util.nullSafeCompare(true, r1.getName(), r2.getName());
        }

        private int getWeight(ObjRelationship r) {
            return r.getSourceEntity() == entity ? 1 : -1;
        }
    }
}
