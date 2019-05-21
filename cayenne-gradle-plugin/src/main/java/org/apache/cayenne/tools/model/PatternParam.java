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

package org.apache.cayenne.tools.model;

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeRelationship;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;

/**
 * @since 4.0
 */
public class PatternParam {

    private String pattern;

    PatternParam() {
    }

    PatternParam(String pattern) {
        this.pattern = pattern;
    }

    public void pattern(String pattern) {
        this.pattern = pattern;
    }

    ExcludeTable toExcludeTable() {
        ExcludeTable table = new ExcludeTable();
        table.setPattern(pattern);
        return table;
    }

    IncludeColumn toIncludeColumn() {
        IncludeColumn column = new IncludeColumn();
        column.setPattern(pattern);
        return column;
    }

    ExcludeColumn toExcludeColumn() {
        ExcludeColumn column = new ExcludeColumn();
        column.setPattern(pattern);
        return column;
    }

    IncludeProcedure toIncludeProcedure() {
        IncludeProcedure procedure = new IncludeProcedure();
        procedure.setPattern(pattern);
        return procedure;
    }

    ExcludeProcedure toExcludeProcedure() {
        ExcludeProcedure procedure = new ExcludeProcedure();
        procedure.setPattern(pattern);
        return procedure;
    }

    ExcludeRelationship toExcludeRelationship(){
        ExcludeRelationship excludeRelationship = new ExcludeRelationship();
        excludeRelationship.setPattern(pattern);
        return excludeRelationship;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}