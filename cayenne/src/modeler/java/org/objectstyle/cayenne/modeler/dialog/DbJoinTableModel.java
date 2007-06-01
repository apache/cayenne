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
package org.objectstyle.cayenne.modeler.dialog;

import java.util.ArrayList;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;

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
