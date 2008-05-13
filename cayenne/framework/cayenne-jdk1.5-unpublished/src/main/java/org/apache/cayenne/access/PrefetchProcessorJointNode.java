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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchTreeNode;

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
    Map<Map, Persistent> resolved;
    List<DataRow> resolvedRows;

    PrefetchProcessorJointNode(PrefetchTreeNode parent, String segmentPath) {
        super(parent, segmentPath);
    }

    @Override
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
        resolved = new HashMap<Map, Persistent>(capacity);
        resolvedRows = new ArrayList<DataRow>(capacity);
        buildRowMapping();
        buildPKIndex();
    }

    List<DataRow> getResolvedRows() {
        return resolvedRows;
    }

    void addObject(Persistent object, DataRow row) {
        objects.add(object);
        resolvedRows.add(row);
    }

    /**
     * Returns an ObjectId map from the flat row.
     */
    Map<String, Object> idFromFlatRow(DataRow flatRow) {

        // TODO: should we also check for nulls in ID (and skip such rows) - this will
        // likely be an indicator of an outer join ... and considering SQLTemplate,
        // this is reasonable to expect...

        Map<String, Object> id = new TreeMap<String, Object>();
        for (int idIndex : idIndices) {
            Object value = flatRow.get(columns[idIndex].getLabel());
            id.put(columns[idIndex].getName(), value);
        }

        return id;
    }

    /**
     * Looks up a previously resolved object using an ObjectId map as a key. Returns null
     * if no matching object exists.
     */
    Persistent getResolved(Map id) {
        return resolved.get(id);
    }

    /**
     * Registers an object in a map of resolved objects, connects this object to parent if
     * parent exists.
     */
    void putResolved(Map id, Persistent object) {
        resolved.put(id, object);
    }

    /**
     * Returns a DataRow from the flat row.
     */
    DataRow rowFromFlatRow(DataRow flatRow) {
        DataRow row = new DataRow(rowCapacity);

        // extract subset of flat row columns, recasting to the target keys
        for (ColumnDescriptor column : columns) {
            row.put(column.getName(), flatRow.get(column.getLabel()));
        }

        return row;
    }

    // ***** private methods *****
    // ========================================================

    /**
     * Configures row columns mapping for this node entity.
     */
    private void buildRowMapping() {
        Map<String, ColumnDescriptor> targetSource = new TreeMap<String, ColumnDescriptor>();

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
                && !getIncoming().getRelationship().isFlattened()) {

            DbRelationship r = getIncoming()
                    .getRelationship()
                    .getDbRelationships()
                    .get(0);
            for (final DbJoin join : r.getJoins()) {

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
        for (ObjAttribute attribute : getResolver().getEntity().getAttributes()) {
            String target = attribute.getDbAttributePath();

            appendColumn(targetSource, target, prefix + target);
        }

        // add relationships
        for (ObjRelationship rel : getResolver().getEntity().getRelationships()) {
            DbRelationship dbRel = rel.getDbRelationships().get(0);
            for (DbAttribute attribute : dbRel.getSourceAttributes() ){
                String target = attribute.getName();

                appendColumn(targetSource, target, prefix + target);
            }
        }

        // add unmapped PK
        for (DbAttribute pk : getResolver().getEntity().getDbEntity().getPrimaryKeys()) {
            appendColumn(targetSource, pk.getName(), prefix + pk.getName());
        }

        int size = targetSource.size();
        this.rowCapacity = (int) Math.ceil(size / 0.75);
        this.columns = new ColumnDescriptor[size];
        targetSource.values().toArray(columns);
    }

    private ColumnDescriptor appendColumn(Map<String, ColumnDescriptor> map, String name, String label) {
        ColumnDescriptor column = map.get(name);

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
        Collection<DbAttribute> pks = getResolver()
                .getEntity()
                .getDbEntity()
                .getPrimaryKeys();
        this.idIndices = new int[pks.size()];

        // this is needed for checking that a valid index is made
        Arrays.fill(idIndices, -1);

        Iterator<DbAttribute> it = pks.iterator();
        for (int i = 0; i < idIndices.length; i++) {
            DbAttribute pk = it.next();

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
            for (ColumnDescriptor column : columns) {
                if (targetColumn.equals(column.getName())) {
                    return column.getLabel();
                }
            }
        }

        return null;
    }

}
