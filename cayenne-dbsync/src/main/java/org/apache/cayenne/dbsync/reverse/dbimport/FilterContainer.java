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

package org.apache.cayenne.dbsync.reverse.dbimport;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0.
 */
public abstract class FilterContainer {

    private String name;

    private final Collection<IncludeTable> includeTableCollection = new LinkedList<>();

    private final Collection<ExcludeTable> excludeTableCollection = new LinkedList<>();

    private final Collection<IncludeColumn> includeColumnCollection = new LinkedList<>();

    private final Collection<ExcludeColumn> excludeColumnCollection = new LinkedList<>();

    private final Collection<IncludeProcedure> includeProcedureCollection = new LinkedList<>();

    private final Collection<ExcludeProcedure> excludeProcedureCollection = new LinkedList<>();

    public Collection<IncludeTable> getIncludeTables() {
        return includeTableCollection;
    }

    public Collection<ExcludeTable> getExcludeTables() {
        return excludeTableCollection;
    }

    public Collection<IncludeColumn> getIncludeColumns() {
        return includeColumnCollection;
    }

    public Collection<ExcludeColumn> getExcludeColumns() {
        return excludeColumnCollection;
    }

    public Collection<IncludeProcedure> getIncludeProcedures() {
        return includeProcedureCollection;
    }

    public Collection<ExcludeProcedure> getExcludeProcedures() {
        return excludeProcedureCollection;
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
        if (name.trim().isEmpty()) {
            return;
        }
        setName(name);
    }

    public boolean isEmptyContainer() {
        return includeColumnCollection.isEmpty()    && excludeColumnCollection.isEmpty()
            && includeTableCollection.isEmpty()     && excludeTableCollection.isEmpty()
            && includeProcedureCollection.isEmpty() && excludeProcedureCollection.isEmpty();
    }

    static boolean isBlank(Collection<?> collection) {
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

        return res;
    }

    protected void appendCollection(StringBuilder res, String prefix, Collection<? extends PatternParam> collection) {
        if (!isBlank(collection)) {
            for (PatternParam item : collection) {
                item.toString(res, prefix);
            }
        }
    }
}
