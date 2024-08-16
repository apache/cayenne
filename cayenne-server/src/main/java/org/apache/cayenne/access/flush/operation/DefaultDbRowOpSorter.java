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

package org.apache.cayenne.access.flush.operation;

import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
public class DefaultDbRowOpSorter implements DbRowOpSorter {

    protected final Provider<DataDomain> dataDomainProvider;
    protected volatile Comparator<DbRowOp> comparator;

    public DefaultDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    @Override
    public List<DbRowOp> sort(List<DbRowOp> dbRows) {
        // sort by id, operation type and entity relations
        dbRows.sort(getComparator());
        // sort reflexively dependent objects
        sortReflexive(dbRows);

        return dbRows;
    }

    protected void sortReflexive(List<DbRowOp> sortedDbRows) {
        DataDomain dataDomain = dataDomainProvider.get();
        EntitySorter sorter = dataDomain.getEntitySorter();
        EntityResolver resolver = dataDomain.getEntityResolver();

        DbEntity lastEntity = null;
        int start = 0;
        int idx = 0;
        DbRowOp lastRow = null;
        for (int i = 0; i < sortedDbRows.size(); i++) {
            DbRowOp row = sortedDbRows.get(i);
            if (row.getEntity() != lastEntity) {
                // we do not sort update operations
                if(lastEntity != null && !(lastRow instanceof UpdateDbRowOp) && sorter.isReflexive(lastEntity)) {
                    ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
                    List<DbRowOp> reflexiveSublist = sortedDbRows.subList(start, idx);
                    sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRowOp);
                }
                start = idx;
                lastEntity = row.getEntity();
            }
            lastRow = row;
            idx++;
        }
        // sort last chunk
        if(lastEntity != null && !(lastRow instanceof UpdateDbRowOp) && sorter.isReflexive(lastEntity)) {
            ObjEntity objEntity = resolver.getObjEntity(lastRow.getObject().getObjectId().getEntityName());
            List<DbRowOp> reflexiveSublist = sortedDbRows.subList(start, idx);
            sorter.sortObjectsForEntity(objEntity, reflexiveSublist, lastRow instanceof DeleteDbRowOp);
        }
    }

    protected Comparator<DbRowOp> getComparator() {
        Comparator<DbRowOp> local = comparator;
        if(local == null) {
            synchronized (this) {
                local = comparator;
                if(local == null) {
                    local = new DbRowComparator(dataDomainProvider.get().getEntitySorter());
                    comparator = local;
                }
            }
        }
        return local;
    }

    protected static class DbRowComparator implements Comparator<DbRowOp> {

        private final EntitySorter entitySorter;

        protected DbRowComparator(EntitySorter entitySorter) {
            this.entitySorter = entitySorter;
        }

        @Override
        public int compare(DbRowOp left, DbRowOp right) {
            DbRowOpType leftType = left.accept(DbRowTypeVisitor.INSTANCE);
            DbRowOpType rightType = right.accept(DbRowTypeVisitor.INSTANCE);
            int result = leftType.compareTo(rightType);

            // 1. sort by op type
            if(result != 0) {
                return result;
            }

            // 2. sort by entity relations, we don't really need this for updates, but do it for the stable result
            result = entitySorter.getDbEntityComparator().compare(left.getEntity(), right.getEntity());
            if(result != 0) {
                // invert result for delete
                return leftType == DbRowOpType.DELETE ? -result : result;
            }

            // TODO: 3. sort updates by changed and null attributes to batch it better,
            //  need to check cost vs benefit though
            return result;
        }
    }

    protected static class DbRowTypeVisitor implements DbRowOpVisitor<DbRowOpType> {

        private static final DbRowTypeVisitor INSTANCE = new DbRowTypeVisitor();

        @Override
        public DbRowOpType visitInsert(InsertDbRowOp diffSnapshot) {
            return DbRowOpType.INSERT;
        }

        @Override
        public DbRowOpType visitUpdate(UpdateDbRowOp diffSnapshot) {
            return DbRowOpType.UPDATE;
        }

        @Override
        public DbRowOpType visitDelete(DeleteDbRowOp diffSnapshot) {
            return DbRowOpType.DELETE;
        }
    }
}
