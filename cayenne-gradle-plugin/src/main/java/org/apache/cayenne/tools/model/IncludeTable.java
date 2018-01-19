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

package org.apache.cayenne.tools.model;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0
 */
public class IncludeTable extends PatternParam {

    private Collection<PatternParam> includeColumns = new LinkedList<>();
    private Collection<PatternParam> excludeColumns = new LinkedList<>();
    private Collection<PatternParam> excludeRelationships = new LinkedList<>();

    IncludeTable() {
    }

    IncludeTable(String pattern) {
        super(pattern);
    }

    public void setName(String name) {
        setPattern(name);
    }

    public void name(String name) {
        setPattern(name);
    }

    public void includeColumn(String pattern) {
        includeColumns.add(new PatternParam(pattern));
    }

    public void includeColumns(String... patterns) {
        for(String pattern: patterns) {
            includeColumn(pattern);
        }
    }

    public void excludeColumn(String pattern) {
        excludeColumns.add(new PatternParam(pattern));
    }

    public void excludeColumns(String... patterns) {
        for(String pattern: patterns) {
            excludeColumn(pattern);
        }
    }

    /**
     * @since 4.1
     */
    public void excludeRelationship(String pattern){
        excludeRelationships.add(new PatternParam(pattern));
    }

    /**
     * @since 4.1
     */
    public void excludeRelationships(String... patterns){
        for(String pattern : patterns) {
            excludeRelationship(pattern);
        }
    }

    org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable toIncludeTable() {
        org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable table
                = new org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable();
        table.setPattern(getPattern());

        for(PatternParam includeColumn : includeColumns) {
            table.addIncludeColumn(includeColumn.toIncludeColumn());
        }
        for(PatternParam excludeColumn : excludeColumns) {
            table.addExcludeColumn(excludeColumn.toExcludeColumn());
        }

        for(PatternParam excludeRelationship : excludeRelationships){
            table.addExcludeRelationship(excludeRelationship.toExcludeRelationship());
        }

        return table;
    }
}