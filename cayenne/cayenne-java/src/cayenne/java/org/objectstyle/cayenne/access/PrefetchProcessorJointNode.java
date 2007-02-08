/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.parser.ASTPath;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * A specialized PrefetchTreeNode used for joint prefetch resolving.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class PrefetchProcessorJointNode extends PrefetchProcessorNode {

    ColumnDescriptor[] columns;
    int[] idIndices;
    int rowCapacity;
    Map resolved;
    List resolvedRows;

    PrefetchProcessorJointNode(PrefetchTreeNode parent, String segmentPath) {
        super(parent, segmentPath);
    }

    void afterInit() {
        super.afterInit();

        // as node will be resolved one row at a time, init objects array here

        // list may shrink as a result of duplicates in flattened rows.. so don't
        // allocate too much space
        int capacity = dataRows != null ? dataRows.size() : 10;
        if (capacity > 100) {
            capacity = capacity / 2;
        }

        objects = new ArrayList(capacity);
        resolved = new HashMap(capacity);
        resolvedRows = new ArrayList(capacity);
        buildRowMapping();
        buildPKIndex();
    }
    
    List getResolvedRows() {
        return resolvedRows;
    }

    void addObject(DataObject object, DataRow row) {
        objects.add(object);
        resolvedRows.add(row);
    }

    /**
     * Returns an ObjectId map from the flat row.
     */
    Map idFromFlatRow(DataRow flatRow) {

        // TODO: should we also check for nulls in ID (and skip such rows) - this will
        // likely be an indicator of an outer join ... and considering SQLTemplate,
        // this is reasonable to expect...

        Map id = new TreeMap();
        for (int i = 0; i < idIndices.length; i++) {
            Object value = flatRow.get(columns[idIndices[i]].getLabel());
            id.put(columns[idIndices[i]].getName(), value);
        }

        return id;
    }

    /**
     * Looks up a previously resolved object using an ObjectId map as a key. Returns null
     * if no matching object exists.
     */
    DataObject getResolved(Map id) {
        return (DataObject) resolved.get(id);
    }

    /**
     * Registers an object in a map of resolved objects, connects this object to parent if
     * parent exists.
     */
    void putResolved(Map id, DataObject object) {
        resolved.put(id, object);
    }

    /**
     * Returns a DataRow from the flat row.
     */
    DataRow rowFromFlatRow(DataRow flatRow) {
        DataRow row = new DataRow(rowCapacity);

        // extract subset of flat row columns, recasting to the target keys
        for (int i = 0; i < columns.length; i++) {
            row.put(columns[i].getName(), flatRow.get(columns[i].getLabel()));
        }

        return row;
    }

    // ***** private methods *****
    // ========================================================

    /**
     * Configures row columns mapping for this node entity.
     */
    private void buildRowMapping() {
        Map targetSource = new TreeMap();

        // build a DB path .. find parent node that terminates the joint group...
        PrefetchTreeNode jointRoot = this;
        while (jointRoot.getParent() != null && !jointRoot.isDisjointPrefetch()) {
            jointRoot = jointRoot.getParent();
        }

        String prefix;
        if (jointRoot != this) {
            Expression objectPath = Expression.fromString(getPath(jointRoot));
            ASTPath translated = (ASTPath) ((PrefetchProcessorNode) jointRoot)
                    .getResolver()
                    .getEntity()
                    .translateToDbPath(objectPath);

            // make sure we do not include "db:" prefix
            prefix = translated.getOperand(0) + ".";
        }
        else {
            prefix = "";
        }

        // find propagated keys, assuming that only one-step joins
        // share their column(s) with parent

        if (getParent() != null
                && !getParent().isPhantom()
                && getIncoming() != null
                && !getIncoming().isFlattened()) {

            DbRelationship r = (DbRelationship) getIncoming().getDbRelationships().get(0);
            Iterator it = r.getJoins().iterator();
            while (it.hasNext()) {
                DbJoin join = (DbJoin) it.next();

                PrefetchProcessorNode parent = (PrefetchProcessorNode) getParent();
                String source;
                if (parent instanceof PrefetchProcessorJointNode) {
                    source = ((PrefetchProcessorJointNode) parent).sourceForTarget(join
                            .getSourceName());
                }
                else {
                    source = join.getSourceName();
                }

                if (source == null) {
                    throw new CayenneRuntimeException(
                            "Propagated column value is not configured for parent node. Join: "
                                    + join);
                }

                appendColumn(targetSource, join.getTargetName(), source);
            }
        }

        // add class attributes
        Iterator attributes = getResolver().getEntity().getAttributes().iterator();
        while (attributes.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) attributes.next();
            String target = attribute.getDbAttributePath();

            appendColumn(targetSource, target, prefix + target);
        }

        // add relationships
        Iterator relationships = getResolver().getEntity().getRelationships().iterator();
        while (relationships.hasNext()) {
            ObjRelationship rel = (ObjRelationship) relationships.next();
            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Iterator dbAttributes = dbRel.getSourceAttributes().iterator();

            while (dbAttributes.hasNext()) {
                DbAttribute attribute = (DbAttribute) dbAttributes.next();
                String target = attribute.getName();

                appendColumn(targetSource, target, prefix + target);
            }
        }

        // add unmapped PK
        Iterator pks = getResolver().getEntity().getDbEntity().getPrimaryKey().iterator();
        while (pks.hasNext()) {
            DbAttribute pk = (DbAttribute) pks.next();
            appendColumn(targetSource, pk.getName(), prefix + pk.getName());
        }

        int size = targetSource.size();
        this.rowCapacity = (int) Math.ceil(size / 0.75);
        this.columns = new ColumnDescriptor[size];
        targetSource.values().toArray(columns);
    }

    private ColumnDescriptor appendColumn(Map map, String name, String label) {
        ColumnDescriptor column = (ColumnDescriptor) map.get(name);

        if (column == null) {
            column = new ColumnDescriptor();
            column.setName(name);
            column.setLabel(label);
            map.put(name, column);
        }

        return column;
    }

    /**
     * Creates an internal index of PK columns in the result.
     */
    private void buildPKIndex() {
        // index PK
        List pks = getResolver().getEntity().getDbEntity().getPrimaryKey();
        this.idIndices = new int[pks.size()];

        // this is needed for checking that a valid index is made
        Arrays.fill(idIndices, -1);

        for (int i = 0; i < idIndices.length; i++) {
            DbAttribute pk = (DbAttribute) pks.get(i);

            for (int j = 0; j < columns.length; j++) {
                if (pk.getName().equals(columns[j].getName())) {
                    idIndices[i] = j;
                    break;
                }
            }

            // sanity check
            if (idIndices[i] == -1) {
                throw new CayenneRuntimeException("PK column is not part of result row: "
                        + pk.getName());
            }
        }
    }

    /**
     * Returns a source label for a given target label.
     */
    private String sourceForTarget(String targetColumn) {
        if (targetColumn != null && columns != null) {
            for (int i = 0; i < columns.length; i++) {
                if (targetColumn.equals(columns[i].getName())) {
                    return columns[i].getLabel();
                }
            }
        }

        return null;
    }

}
