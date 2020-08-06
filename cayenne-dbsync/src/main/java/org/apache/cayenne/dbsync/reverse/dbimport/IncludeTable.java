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

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 4.0.
 */
public class IncludeTable extends PatternParam implements XMLSerializable {

    private final List<IncludeColumn> includeColumns = new ArrayList<>();

    private final List<ExcludeColumn> excludeColumns = new ArrayList<>();

    private final List<ExcludeRelationship> excludeRelationship = new ArrayList<>();

    public IncludeTable() {
    }

    public IncludeTable(String pattern) {
        super(pattern);
    }

    public IncludeTable(IncludeTable original) {
        super(original);
        for (IncludeColumn includeColumn : original.getIncludeColumns()) {
            this.addIncludeColumn(new IncludeColumn(includeColumn));
        }
        for (ExcludeColumn excludeColumn : original.getExcludeColumns()) {
            this.addExcludeColumn(new ExcludeColumn(excludeColumn));
        }
    }

    public List<IncludeColumn> getIncludeColumns() {
        return includeColumns;
    }

    public void setIncludeColumns(Collection<IncludeColumn> includeColumns) {
        this.includeColumns.addAll(includeColumns);
    }

    public List<ExcludeColumn> getExcludeColumns() {
        return excludeColumns;
    }

    public void setExcludeColumns(Collection<ExcludeColumn> excludeColumns) {
        this.excludeColumns.addAll(excludeColumns);
    }

    /**
     * @since 4.1
     */
    public List<ExcludeRelationship> getExcludeRelationship() {
        return excludeRelationship;
    }

    /**
     * @since 4.1
     */
    public void setExcludeRelationship (Collection<ExcludeRelationship> excludeRelationship) {
        this.excludeRelationship.addAll(excludeRelationship);
    }

    public void addIncludeColumn(IncludeColumn includeColumn) {
        this.includeColumns.add(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        this.excludeColumns.add(excludeColumn);
    }

    /**
     * @since 4.1
     */
    public void addExcludeRelationship(ExcludeRelationship excludeRelationship){
        this.excludeRelationship.add(excludeRelationship);
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("includeTable")
            .simpleTag("name", this.getPattern())
            .nested(this.getIncludeColumns(), delegate)
            .nested(this.getExcludeColumns(), delegate)
        .end();
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

        if(excludeRelationship != null && !excludeRelationship.isEmpty()) {
            for(ExcludeRelationship excludeRelationship : excludeRelationship) {
                excludeRelationship.toString(res, prefix);
            }
        }

        return res;
    }
}
