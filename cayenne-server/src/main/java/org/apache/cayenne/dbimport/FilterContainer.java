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
package org.apache.cayenne.dbimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class FilterContainer {

    @XmlElement(name = "includeTable")
    private Collection<IncludeTable> includeTables = new LinkedList<IncludeTable>();

    @XmlElement(name = "excludeTable")
    private Collection<ExcludeTable> excludeTables = new LinkedList<ExcludeTable>();

    @XmlElement(name = "includeColumn")
    private Collection<IncludeColumn> includeColumns = new LinkedList<IncludeColumn>();

    @XmlElement(name = "excludeColumn")
    private Collection<ExcludeColumn> excludeColumns = new LinkedList<ExcludeColumn>();

    @XmlElement(name = "includeProcedure")
    private Collection<IncludeProcedure> includeProcedures = new LinkedList<IncludeProcedure>();

    @XmlElement(name = "excludeProcedure")
    private Collection<ExcludeProcedure> excludeProcedures = new LinkedList<ExcludeProcedure>();

    public Collection<IncludeTable> getIncludeTables() {
        return includeTables;
    }

    public void setIncludeTables(Collection<IncludeTable> includeTables) {
        this.includeTables = includeTables;
    }

    public Collection<ExcludeTable> getExcludeTables() {
        return excludeTables;
    }

    public void setExcludeTables(Collection<ExcludeTable> excludeTables) {
        this.excludeTables = excludeTables;
    }

    public Collection<IncludeColumn> getIncludeColumns() {
        return includeColumns;
    }

    public void setIncludeColumns(Collection<IncludeColumn> includeColumns) {
        this.includeColumns = includeColumns;
    }

    public Collection<ExcludeColumn> getExcludeColumns() {
        return excludeColumns;
    }

    public void setExcludeColumns(Collection<ExcludeColumn> excludeColumns) {
        this.excludeColumns = excludeColumns;
    }

    public Collection<IncludeProcedure> getIncludeProcedures() {
        return includeProcedures;
    }

    public void setIncludeProcedures(Collection<IncludeProcedure> includeProcedures) {
        this.includeProcedures = includeProcedures;
    }

    public Collection<ExcludeProcedure> getExcludeProcedures() {
        return excludeProcedures;
    }

    public void setExcludeProcedures(Collection<ExcludeProcedure> excludeProcedures) {
        this.excludeProcedures = excludeProcedures;
    }


    public void addIncludeColumn(IncludeColumn includeColumn) {
        this.includeColumns.add(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        this.excludeColumns.add(excludeColumn);
    }

    public void addIncludeTable(IncludeTable includeTable) {
        this.includeTables.add(includeTable);
    }

    public void addExcludeTable(ExcludeTable excludeTable) {
        this.excludeTables.add(excludeTable);
    }

    public void addIncludeProcedure(IncludeProcedure includeProcedure) {
        this.includeProcedures.add(includeProcedure);
    }

    public void addExcludeProcedure(ExcludeProcedure excludeProcedure) {
        this.excludeProcedures.add(excludeProcedure);
    }

    public boolean isEmptyContainer() {
        return includeColumns.isEmpty()    && excludeColumns.isEmpty()
            && includeTables.isEmpty()     && excludeTables.isEmpty()
            && includeProcedures.isEmpty() && excludeProcedures.isEmpty();
    }

    public static boolean isBlank(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), "").toString();
    }

    public StringBuilder toString(StringBuilder res, String prefix) {
        appendCollection(res, prefix, includeTables);
        appendCollection(res, prefix, excludeTables);
        appendCollection(res, prefix, includeColumns);
        appendCollection(res, prefix, excludeColumns);
        appendCollection(res, prefix, includeProcedures);
        appendCollection(res, prefix, excludeProcedures);

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
