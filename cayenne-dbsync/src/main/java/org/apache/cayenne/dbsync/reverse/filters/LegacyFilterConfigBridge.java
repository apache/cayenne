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
package org.apache.cayenne.dbsync.reverse.filters;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @since 4.0
 */
public class LegacyFilterConfigBridge {

    private String catalog;
    private String schema;

    private String includeTableFilters;
    private String includeColumnFilters;
    private String includeProceduresFilters;
    private String excludeTableFilters;
    private String excludeColumnFilters;
    private String excludeProceduresFilters;

    private boolean loadProcedures;

    public LegacyFilterConfigBridge() {
    }

    public LegacyFilterConfigBridge catalog(String catalog) {
        this.catalog = catalog;
        return this;
    }

    public String catalog() {
        return catalog;
    }

    public LegacyFilterConfigBridge schema(String schema) {
        this.schema = schema;
        return this;
    }

    public String schema() {
        return schema;
    }

    public LegacyFilterConfigBridge includeTables(String tableFilters) {
        if (isBlank(tableFilters)) {
            return this;
        }

        this.includeTableFilters = transform(tableFilters);
        return this;
    }

    public LegacyFilterConfigBridge includeColumns(String columnFilters) {
        if (isBlank(columnFilters)) {
            return this;
        }

        this.includeColumnFilters = transform(columnFilters);
        return this;
    }

    public LegacyFilterConfigBridge includeProcedures(String proceduresFilters) {
        if (isBlank(proceduresFilters)) {
            return this;
        }

        this.includeProceduresFilters = transform(proceduresFilters);
        return this;
    }

    public LegacyFilterConfigBridge excludeTables(String tableFilters) {
        if (isBlank(tableFilters)) {
            return this;
        }

        this.excludeTableFilters = transform(tableFilters);
        return this;
    }

    public LegacyFilterConfigBridge excludeColumns(String columnFilters) {
        if (isBlank(columnFilters)) {
            return this;
        }

        this.excludeColumnFilters = transform(columnFilters);
        return this;
    }

    public LegacyFilterConfigBridge excludeProcedures(String proceduresFilters) {
        if (isBlank(proceduresFilters)) {
            return this;
        }

        this.excludeProceduresFilters = transform(proceduresFilters);
        return this;
    }

    private static String transform(String pattern) {
        return "^" + pattern.replaceAll("[*?]", ".$0") + "$";
    }

    public void setProceduresFilters(boolean loadProcedures) {
        this.loadProcedures = loadProcedures;
    }

    public String getIncludeTableFilters() {
        return includeTableFilters;
    }

    public String getIncludeColumnFilters() {
        return includeColumnFilters;
    }

    public String getIncludeProceduresFilters() {
        return includeProceduresFilters;
    }

    public String getExcludeTableFilters() {
        return excludeTableFilters;
    }

    public String getExcludeColumnFilters() {
        return excludeColumnFilters;
    }

    public String getExcludeProceduresFilters() {
        return excludeProceduresFilters;
    }

    public boolean isLoadProcedures() {
        return loadProcedures;
    }
}
