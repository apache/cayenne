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

import static org.apache.commons.lang.StringUtils.join;

/**
 * @since 4.0.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class IncludeTable extends PatternParam {

    @XmlElement(name = "includeColumn")
    private Collection<IncludeColumn> includeColumns = new LinkedList<IncludeColumn>();

    @XmlElement(name = "excludeColumn")
    private Collection<ExcludeColumn> excludeColumns = new LinkedList<ExcludeColumn>();

    public IncludeTable() {
    }

    public IncludeTable(String pattern) {
        super(pattern);
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

    public void addIncludeColumn(IncludeColumn includeColumn) {
        this.includeColumns.add(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        this.excludeColumns.add(excludeColumn);
    }

    @Override
    public StringBuilder toString(StringBuilder res, String s) {
        super.toString(res, s);

        String prefix = s + "  ";
        if (includeColumns != null && !includeColumns.isEmpty()) {
            for (IncludeColumn includeColumn : includeColumns) {
                includeColumn.toString(res, prefix);
            }
        }

        if (excludeColumns != null && !excludeColumns.isEmpty()) {
            for (ExcludeColumn excludeColumn : excludeColumns) {
                excludeColumn.toString(res, prefix);
            }
        }

        return res;
    }
}
