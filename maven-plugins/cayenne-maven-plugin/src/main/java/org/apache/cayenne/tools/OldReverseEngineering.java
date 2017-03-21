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

package org.apache.cayenne.tools;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;

/**
 * @since 4.0
 * @deprecated this class exists only to catch old configuration and warn about it.
 *             Can be deleted with deprecated members of {@link DbImporterMojo}.
 */
@Deprecated
public class OldReverseEngineering {
    private String name;
    private Boolean skipRelationshipsLoading;
    private Boolean skipPrimaryKeyLoading;
    private String meaningfulPkTables;
    private String stripFromTableNames;
    private boolean usePrimitives;

    private final Collection<Schema> schemaCollection = new LinkedList<>();
    private final Collection<String> tableTypes = new LinkedList<>();
    private final Collection<Catalog> catalogCollection = new LinkedList<>();
    private final Collection<IncludeTable> includeTableCollection = new LinkedList<>();
    private final Collection<ExcludeTable> excludeTableCollection = new LinkedList<>();
    private final Collection<IncludeColumn> includeColumnCollection = new LinkedList<>();
    private final Collection<ExcludeColumn> excludeColumnCollection = new LinkedList<>();
    private final Collection<IncludeProcedure> includeProcedureCollection = new LinkedList<>();
    private final Collection<ExcludeProcedure> excludeProcedureCollection = new LinkedList<>();

    private void throwException() {
        throw new UnsupportedOperationException("\n<reverseEngineering> tag has been renamed to <dbimport> since 4.0.M5.\n" +
                "\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    public void setName(String name) {
        throwException();
    }
    public void addTableType(String type) {
        throwException();
    }
    public void addCatalog(Catalog catalog) {
        throwException();
    }
    public void addSchema(Schema schema) {
        throwException();
    }
    public void addIncludeColumn(IncludeColumn includeColumn) {
        throwException();
    }
    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        throwException();
    }
    public void addIncludeTable(IncludeTable includeTable) {
        throwException();
    }
    public void addExcludeTable(ExcludeTable excludeTable) {
        throwException();
    }
    public void addIncludeProcedure(IncludeProcedure includeProcedure) {
        throwException();
    }
    public void addExcludeProcedure(ExcludeProcedure excludeProcedure) {
        throwException();
    }
}
