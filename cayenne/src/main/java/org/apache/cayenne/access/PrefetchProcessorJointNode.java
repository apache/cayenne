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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A specialized PrefetchTreeNode used for joint prefetch resolving.
 * 
 * @since 1.2
 */
class PrefetchProcessorJointNode extends PrefetchProcessorNode {

    RSColumn[] columns;
    int[] idIndices;
    Map<Object, Persistent> resolved;
    List<DataRow> resolvedRows;

    // the layout of this node's rows, by the columns' result set names
    private DataRowLayout rowLayout;

    // reads this node's columns from the flat rows
    private DataRowLayout.Reader flatReader;

    PrefetchProcessorJointNode(PrefetchProcessorNode parent, String segmentPath) {
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

        objects = new ArrayList<>(capacity);
        resolved = HashMap.newHashMap(capacity);
        resolvedRows = new ArrayList<>(capacity);
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
     * Returns a key of the flat row's object for {@link #getResolved(Object)} and {@link #putResolved(Object,
     * Persistent)}: the PK value of a single-column PK, or a list of the PK values in the PK column order. The key
     * is only meaningful within this node, as the column names are not a part of it.
     */
    Object idFromFlatRow(DataRow flatRow) {

        // TODO: should we also check for nulls in ID (and skip such rows) - this will
        // likely be an indicator of an outer join ... and considering SQLTemplate,
        // this is reasonable to expect...

        if (idIndices.length == 1) {
            return flatReader.get(flatRow, idIndices[0]);
        }

        Object[] id = new Object[idIndices.length];
        for (int i = 0; i < id.length; i++) {
            id[i] = flatReader.get(flatRow, idIndices[i]);
        }

        // Arrays.asList: a value-based key, unlike List.of it allows nulls, which an outer join produces
        return Arrays.asList(id);
    }

    /**
     * Looks up a previously resolved object by the key returned by {@link #idFromFlatRow(DataRow)}. Returns null if
     * no matching object exists.
     */
    Persistent getResolved(Object id) {
        return resolved.get(id);
    }

    /**
     * Registers an object in a map of resolved objects, connects this object to parent if
     * parent exists.
     */
    void putResolved(Object id, Persistent object) {
        resolved.put(id, object);
    }

    /**
     * Returns a DataRow from the flat row. It is a part of the same statement result, so it has the flat row's
     * version.
     */
    DataRow rowFromFlatRow(DataRow flatRow) {
        // extract subset of flat row columns, recasting to the target keys
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            values[i] = flatReader.get(flatRow, i);
        }

        DataRow row = DataRow.of(resolver.getEntity().getName(), rowLayout, values, flatRow.getVersion());

        // since JDBC row reader won't inject JOINED entity name, we have to detect it here...
        ClassDescriptor descriptor = resolver.getDescriptor();
        ObjEntity entity = descriptor.getEntityInheritanceTree().entityMatchingRow(row);
        String entityName = entity == null ? null : entity.getName();
        return Objects.equals(entityName, row.getEntityName()) ? row : row.withEntityName(entityName);
    }

    /**
     * Configures row columns mapping for this node entity.
     */
    private void buildRowMapping() {
        final Map<String, RSColumn> targetSource = new TreeMap<>();

        // build a DB path .. find parent node that terminates the joint group...
        PrefetchTreeNode jointRoot = this;
        while (jointRoot.getParent() != null && !jointRoot.isDisjointPrefetch()
                && !jointRoot.isDisjointByIdPrefetch()) {
            jointRoot = jointRoot.getParent();
        }

        final String prefix;
        if (jointRoot != this) {
            Expression objectPath = ExpressionFactory.pathExp(getPath(jointRoot));
            PathExp translated = (PathExp) ((PrefetchProcessorNode) jointRoot)
                    .getResolver()
                    .getEntity()
                    .translateToDbPath(objectPath);

            // make sure we do not include "db:" prefix
            prefix = translated.getOperand(0) + ".";
        } else {
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
                appendColumn(targetSource, join.getTargetName(), prefix
                        + join.getTargetName());
            }
        }

        ClassDescriptor descriptor = resolver.getDescriptor();

        for (PropertyDescriptor property : descriptor.getAllProperties()) {
            switch (property) {
                case AttributeProperty attribute -> {
                    CayennePath target = attribute.getAttribute().getDbAttributePath();
                    if (!attribute.getAttribute().isLazy()) {
                        appendColumn(targetSource, target.value(), prefix + target);
                    }
                }
                case ArcProperty arc -> {
                    DbRelationship dbRel = arc.getRelationship().getDbRelationships().get(0);
                    for (DbAttribute dbAttribute : dbRel.getSourceAttributes()) {
                        String target = dbAttribute.getName();
                        appendColumn(targetSource, target, prefix + target);
                    }
                }
                default -> {
                }
            }
        }

        // append id columns ... (some may have been appended already via relationships)
        for (String pkName : descriptor.getEntity().getPrimaryKeyNames()) {
            appendColumn(targetSource, pkName, prefix + pkName);
        }

        // append id columns of additional entities...
        descriptor.getAdditionalDbEntities().forEach((additionalPath, additionalDescriptor) -> {
            for (DbAttribute pk : additionalDescriptor.getDbEntity().getPrimaryKeys()) {
                String name = additionalPath.dot(pk.getName()).value();
                appendColumn(targetSource, name, prefix + name);
            }
        });

        // append inheritance discriminator columns...
        for (ObjAttribute column : descriptor.getDiscriminatorColumns()) {
            CayennePath target = column.getDbAttributePath();
            appendColumn(targetSource, target.value(), prefix + target);
        }

        int size = targetSource.size();
        this.columns = new RSColumn[size];
        targetSource.values().toArray(columns);

        String[] names = new String[size];
        String[] flatNames = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = columns[i].rsName();
            flatNames[i] = columns[i].dataRowName();
        }
        this.rowLayout = DataRowLayout.of(names);
        this.flatReader = DataRowLayout.of(flatNames).reader();
    }

    private RSColumn appendColumn(
            Map<String, RSColumn> map,
            String name,
            String label) {
        RSColumn column = map.get(name);

        if (column == null) {
            column = new RSColumn(name, 0, label, null, null);
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
                if (pk.getName().equals(columns[j].rsName())) {
                    idIndices[i] = j;
                    break;
                }
            }

            // sanity check
            if (idIndices[i] == -1) {
                throw new CayenneRuntimeException("PK column is not part of result row: %s", pk.getName());
            }
        }
    }
}
