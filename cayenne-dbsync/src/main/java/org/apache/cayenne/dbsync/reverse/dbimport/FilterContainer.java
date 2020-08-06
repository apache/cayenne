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

package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.0.
 */
public abstract class FilterContainer {

    private String name;

    private final List<IncludeTable> includeTableCollection = new ArrayList<>();

    private final List<ExcludeTable> excludeTableCollection = new ArrayList<>();

    private final List<IncludeColumn> includeColumnCollection = new ArrayList<>();

    private final List<ExcludeColumn> excludeColumnCollection = new ArrayList<>();

    private final List<IncludeProcedure> includeProcedureCollection = new ArrayList<>();

    private final List<ExcludeProcedure> excludeProcedureCollection = new ArrayList<>();

    private final List<ExcludeRelationship> excludeRelationshipCollection = new ArrayList<>();

    public FilterContainer() {
    }

    public FilterContainer(FilterContainer original) {
        this.setName(original.getName());
        for (IncludeTable includeTable : original.getIncludeTables()) {
            this.addIncludeTable(new IncludeTable(includeTable));
        }
        for (ExcludeTable excludeTable : original.getExcludeTables()) {
            this.addExcludeTable(new ExcludeTable(excludeTable));
        }
        for (IncludeColumn includeColumn : original.getIncludeColumns()) {
            this.addIncludeColumn(new IncludeColumn(includeColumn));
        }
        for (ExcludeColumn excludeColumn : original.getExcludeColumns()) {
            this.addExcludeColumn(new ExcludeColumn(excludeColumn));
        }
        for (IncludeProcedure includeProcedure : original.getIncludeProcedures()) {
            this.addIncludeProcedure(new IncludeProcedure(includeProcedure));
        }
        for (ExcludeProcedure excludeProcedure : original.getExcludeProcedures()) {
            this.addExcludeProcedure(new ExcludeProcedure(excludeProcedure));
        }
    }

    public List<IncludeTable> getIncludeTables() {
        return includeTableCollection;
    }

    public List<ExcludeTable> getExcludeTables() {
        return excludeTableCollection;
    }

    public List<IncludeColumn> getIncludeColumns() {
        return includeColumnCollection;
    }

    public List<ExcludeColumn> getExcludeColumns() {
        return excludeColumnCollection;
    }

    public List<IncludeProcedure> getIncludeProcedures() {
        return includeProcedureCollection;
    }

    public List<ExcludeProcedure> getExcludeProcedures() {
        return excludeProcedureCollection;
    }

    /**
     * @since 4.1
     */
    public List<ExcludeRelationship> getExcludeRelationship() {
        return excludeRelationshipCollection;
    }

    public void addIncludeColumn(IncludeColumn includeColumn) {
        this.includeColumnCollection.add(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        this.excludeColumnCollection.add(excludeColumn);
    }

    public void addIncludeTable(IncludeTable includeTable) {
        this.includeTableCollection.add(includeTable);
    }

    public void addExcludeTable(ExcludeTable excludeTable) {
        this.excludeTableCollection.add(excludeTable);
    }

    public void addIncludeProcedure(IncludeProcedure includeProcedure) {
        this.includeProcedureCollection.add(includeProcedure);
    }

    public void addExcludeProcedure(ExcludeProcedure excludeProcedure) {
        this.excludeProcedureCollection.add(excludeProcedure);
    }

    /**
     * @since 4.1
     */
    public void addExcludeRelationship(ExcludeRelationship excludeRelationship) {
        this.excludeRelationshipCollection.add(excludeRelationship);
    }

    public void clearIncludeTables() {
        includeTableCollection.clear();
    }

    public void clearExcludeTables() {
        excludeTableCollection.clear();
    }

    public void clearIncludeProcedures() {
        includeProcedureCollection.clear();
    }

    public void clearExcludeProcedures() {
        excludeProcedureCollection.clear();
    }

    public void clearIncludeColumns() {
        includeColumnCollection.clear();
    }

    public void clearExcludeColumns() {
        excludeColumnCollection.clear();
    }

    /**
     * @since 4.1
     */
    public void clearExcludeRelationships() {
        excludeRelationshipCollection.clear();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void set(String name) {
        setName(name);
    }

    public void addConfiguredName(AntNestedElement name) {
        setName(name.getName());
    }

    public void addText(String name) {
        if (Util.isBlank(name)) {
            return;
        }
        setName(name);
    }

    public boolean isEmptyContainer() {
        return includeColumnCollection.isEmpty()    && excludeColumnCollection.isEmpty()
            && includeTableCollection.isEmpty()     && excludeTableCollection.isEmpty()
            && includeProcedureCollection.isEmpty() && excludeProcedureCollection.isEmpty() && excludeRelationshipCollection.isEmpty();
    }

    static boolean isBlank(List<?> collection) {
        return collection == null || collection.isEmpty();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), "").toString();
    }

    public StringBuilder toString(StringBuilder res, String prefix) {
        appendCollection(res, prefix, includeTableCollection);
        appendCollection(res, prefix, excludeTableCollection);
        appendCollection(res, prefix, includeColumnCollection);
        appendCollection(res, prefix, excludeColumnCollection);
        appendCollection(res, prefix, includeProcedureCollection);
        appendCollection(res, prefix, excludeProcedureCollection);
        appendCollection(res, prefix, excludeRelationshipCollection);

        return res;
    }

    protected void appendCollection(StringBuilder res, String prefix, List<? extends PatternParam> collection) {
        if (!isBlank(collection)) {
            for (PatternParam item : collection) {
                item.toString(res, prefix);
            }
        }
    }
}
