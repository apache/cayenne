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
package org.apache.cayenne.access.loader.filters;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.apache.cayenne.access.loader.filters.FilterFactory.*;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @since 4.0.
 */
public class EntityFilters {

    private static final Log LOG = LogFactory.getLog(Filter.class);

    private final DbPath dbPath;

    private final Filter<String> tableFilters;
    private final Filter<String> columnFilters;
    private final Filter<String> proceduresFilters;


    public EntityFilters(DbPath dbPath) {
        this(dbPath, NULL, NULL, NULL);
    }
    public EntityFilters(DbPath dbPath, Filter<String> tableFilters, Filter<String> columnFilters, Filter<String> proceduresFilters) {
        this.dbPath = dbPath;
        this.tableFilters = set(tableFilters);
        this.columnFilters = set(columnFilters);
        this.proceduresFilters = set(proceduresFilters);
    }

    public boolean isEmpty() {
        return (tableFilters == null || NULL.equals(tableFilters))
                && (columnFilters == null || NULL.equals(columnFilters))
                && (proceduresFilters == null || NULL.equals(proceduresFilters));
    }

    public DbPath getDbPath() {
        return dbPath;
    }

    private Filter<String> set(Filter<String> tableFilters) {
        return tableFilters == null ? NULL : tableFilters;
    }

    public Filter<DbEntity> tableFilter() {
        return new DbEntityFilter(dbPath, tableFilters);
    }

    public Filter<DbAttribute> columnFilter() {
        return new DbAttributeFilter(dbPath, columnFilters);
    }

    public Filter<Procedure> procedureFilter() {
        return new ProcedureFilter(dbPath, proceduresFilters);
    }

    public EntityFilters join(EntityFilters filter) {
        if (filter == null) {
            return this;
        }

        DbPath path;
        if (this.dbPath == null) {
            path = filter.dbPath;
        } else if (filter.dbPath == null) {
            path = this.dbPath;
        } else {
            path = this.dbPath.merge(filter.dbPath);
        }

        return new EntityFilters(path,
                this.tableFilters.join(filter.tableFilters),
                this.columnFilters.join(filter.columnFilters),
                this.proceduresFilters.join(filter.proceduresFilters));
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append(dbPath).append(":\n");
        if (tableFilters != null) {
            res.append("    Table: ").append(tableFilters).append("\n");
        }

        if (columnFilters != null) {
            res.append("    Column: ").append(columnFilters).append("\n");
        }

        if (proceduresFilters != null) {
            res.append("    Procedures: ").append(proceduresFilters).append("\n");
        }

        return res.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        EntityFilters rhs = (EntityFilters) obj;
        return new EqualsBuilder()
                .append(this.dbPath, rhs.dbPath)
                .append(this.tableFilters, rhs.tableFilters)
                .append(this.columnFilters, rhs.columnFilters)
                .append(this.proceduresFilters, rhs.proceduresFilters)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(dbPath)
                .append(tableFilters)
                .append(columnFilters)
                .append(proceduresFilters)
                .toHashCode();
    }

    /**
     * @param <T>
     */
    private abstract static class EntityFilter<T> implements Filter<T> {

        private final DbPath dbPath;
        private final Filter<String> filter;

        protected EntityFilter(DbPath dbPath, Filter<String> filter) {
            this.dbPath = dbPath;
            this.filter = filter;
        }

        DbPath getDbPath() {
            return dbPath;
        }

        Filter<String> getFilter() {
            return filter;
        }

        @Override
        public EntityFilter<T> join(Filter<T> filter) {
            if (!(filter instanceof EntityFilter)) {
                throw new IllegalArgumentException("Unexpected filter join '" + this + "' and '" + filter + "'");
            }

            EntityFilter<T> entityFilter = (EntityFilter<T>) filter;
            DbPath dbPath;
            if (entityFilter.dbPath.isCover(this.dbPath)) {
                dbPath = entityFilter.dbPath;
            } else if (this.dbPath.isCover(entityFilter.dbPath)) {
                dbPath = this.dbPath;
            } else {
                throw new IllegalArgumentException("Attempt to merge filter with incompatible tuples: '" + entityFilter.dbPath + "'");
            }

            return create(dbPath, this.filter.join(entityFilter.filter));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " (" + dbPath + " -> " + filter + ")";
        }

        public abstract EntityFilter<T> create(DbPath dbPath, Filter<String> filter);

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null) {
                return false;
            }

            if (o instanceof Filter) { // TODO
                return filter.equals(o);
            }

            if (getClass() != o.getClass()) {
                return false;
            }

            return filter.equals(((EntityFilter) o).filter);

        }

        @Override
        public int hashCode() {
            return filter.hashCode();
        }
    }

    private static class DbEntityFilter extends EntityFilter<DbEntity> {

        public DbEntityFilter(DbPath dbPath, Filter<String> filter) {
            super(dbPath, filter);
        }

        @Override
        public boolean isInclude(DbEntity obj) {
            if (LOG.isTraceEnabled()
                    && this.getDbPath().isCover(obj.getCatalog(), obj.getSchema())) {

                LOG.warn("Attempt to apply inconvenient filter '" + this + "' for dbEntity '" + obj + "'");
            }

            return this.getFilter().isInclude(obj.getName());
        }

        @Override
        public EntityFilter<DbEntity> create(DbPath dbPath, Filter<String> filter) {
            return new DbEntityFilter(dbPath, filter);
        }
    }

    private static class DbAttributeFilter extends EntityFilter<DbAttribute> {

        public DbAttributeFilter(DbPath dbPath, Filter<String> filter) {
            super(dbPath, filter);
        }

        @Override
        public boolean isInclude(DbAttribute obj) {
            DbEntity entity = obj.getEntity();
            if (LOG.isTraceEnabled()
                    && this.getDbPath().isCover(entity.getCatalog(), entity.getSchema(), entity.getName())) {

                LOG.warn("Attempt to apply inconvenient filter '" + this + "' for attribute '" + obj + "'");
            }

            return this.getFilter().isInclude(obj.getName());
        }

        @Override
        public EntityFilter<DbAttribute> create(DbPath dbPath, Filter<String> filter) {
            return new DbAttributeFilter(dbPath, filter);
        }
    }

    private static class ProcedureFilter extends EntityFilter<Procedure> {

        public ProcedureFilter(DbPath dbPath, Filter<String> filter) {
            super(dbPath, filter);
        }

        @Override
        public boolean isInclude(Procedure obj) {
            if (LOG.isTraceEnabled()
                    && this.getDbPath().isCover(obj.getCatalog(), obj.getSchema())) {

                LOG.warn("Attempt to apply inconvenient filter '" + this + "' for procedure '" + obj + "'");
            }
            return this.getFilter().isInclude(obj.getName());
        }

        @Override
        public EntityFilter<Procedure> create(DbPath dbPath, Filter<String> filter) {
            return new ProcedureFilter(dbPath, filter);
        }
    }


    public static final class Builder {
        private String catalog;
        private String schema;

        private Filter<String> tableFilters = TRUE;
        private Filter<String> columnFilters = TRUE;
        private Filter<String> proceduresFilters = NULL;

        public Builder() {
        }

        public Builder catalog(String catalog) {
            this.catalog = catalog;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public String schema() {
            return schema;
        }

        public Builder includeTables(String tableFilters) {
            if (isBlank(tableFilters)) {
                return this;
            }

            this.tableFilters = includeFilter(tableFilters, this.tableFilters);
            return this;
        }

        public Builder includeColumns(String columnFilters) {
            if (isBlank(columnFilters)) {
                return this;
            }

            this.columnFilters = includeFilter(columnFilters, this.columnFilters);
            return this;
        }

        public Builder includeProcedures(String proceduresFilters) {
            if (isBlank(proceduresFilters)) {
                return this;
            }

            this.proceduresFilters = includeFilter(proceduresFilters, this.proceduresFilters);
            return this;
        }

        private Filter<String> includeFilter(String tableFilters, Filter<String> filter) {
            for (String pattern : tableFilters.split(",")) {
                filter = filter.join(include(transform(pattern)));
            }

            return filter;
        }

        public Builder excludeTables(String tableFilters) {
            if (isBlank(tableFilters)) {
                return this;
            }

            this.tableFilters = excludeFilter(tableFilters, this.tableFilters);
            return this;
        }

        public Builder excludeColumns(String columnFilters) {
            if (isBlank(columnFilters)) {
                return this;
            }

            this.columnFilters = excludeFilter(columnFilters, this.columnFilters);
            return this;
        }

        public Builder excludeProcedures(String proceduresFilters) {
            if (isBlank(proceduresFilters)) {
                return this;
            }

            this.proceduresFilters = excludeFilter(proceduresFilters, this.proceduresFilters);
            return this;
        }

        private Filter<String> excludeFilter(String tableFilters, Filter<String> filter) {
            for (String pattern : tableFilters.split(",")) {
                filter = filter.join(exclude(transform(pattern)));
            }

            return filter;
        }

        private static String transform(String pattern) {
            return "^" + pattern.replaceAll("[*?]", ".$0") + "$";
        }

        public Filter<String> tableFilters() {
            return tableFilters;
        }

        public Filter<String> columnFilters() {
            return columnFilters;
        }

        public Filter<String> proceduresFilters() {
            return proceduresFilters;
        }

        public void setTableFilters(Filter<String> tableFilters) {
            this.tableFilters = tableFilters;
        }

        public void setColumnFilters(Filter<String> columnFilters) {
            this.columnFilters = columnFilters;
        }

        public void setProceduresFilters(Filter<String> proceduresFilters) {
            this.proceduresFilters = proceduresFilters;
        }

        public EntityFilters build() {
            return new EntityFilters(new DbPath(catalog, schema), tableFilters, columnFilters, proceduresFilters);
        }
    }
}
