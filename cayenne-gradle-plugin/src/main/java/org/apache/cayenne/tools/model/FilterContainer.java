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

import java.util.Collection;
import java.util.LinkedList;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

/**
 * @since 4.0
 */
public class FilterContainer {

    private String name;
    private Collection<IncludeTable> includeTables = new LinkedList<>();
    private Collection<PatternParam> excludeTables = new LinkedList<>();
    private Collection<PatternParam> includeColumns = new LinkedList<>();
    private Collection<PatternParam> excludeColumns = new LinkedList<>();
    private Collection<PatternParam> includeProcedures = new LinkedList<>();
    private Collection<PatternParam> excludeProcedures = new LinkedList<>();
    private Collection<PatternParam> excludeRelationships = new LinkedList<>();

    FilterContainer() {
    }

    FilterContainer(String name) {
        this.name = name;
    }

    public void name(String name) {
        this.name = name;
    }

    public void includeTable(String pattern) {
        includeTables.add(new IncludeTable(pattern));
    }

    public void includeTable(Closure<?> closure) {
        includeTables.add(ConfigureUtil.configure(closure, new IncludeTable()));
    }

    public void includeTable(String pattern, Closure<?> closure) {
        includeTables.add(ConfigureUtil.configure(closure, new IncludeTable(pattern)));
    }

    public void includeTables(String... patterns) {
        for(String pattern: patterns) {
            includeTable(pattern);
        }
    }

    public void excludeTable(String pattern) {
        addToCollection(excludeTables, pattern);
    }

    public void excludeTables(String... patterns) {
        for(String pattern: patterns) {
            excludeTable(pattern);
        }
    }

    public void includeColumn(String pattern) {
        addToCollection(includeColumns, pattern);
    }

    public void includeColumns(String... patterns) {
        for(String pattern: patterns) {
            includeColumn(pattern);
        }
    }

    public void excludeColumn(String pattern) {
        addToCollection(excludeColumns, pattern);
    }

    public void excludeColumns(String... patterns) {
        for(String pattern: patterns) {
            excludeColumn(pattern);
        }
    }

    /**
     * @param pattern RegExp pattern to use for relationship exclusion
     * @since 4.1
     */
    public void excludeRelationship(String pattern) {
        addToCollection(excludeRelationships, pattern);
    }

    /**
     * @param patterns collection of RegExp patterns to use for relationship exclusion
     * @since 4.1
     */
    public void excludeRelationships(String... patterns) {
        for(String pattern : patterns) {
            excludeRelationship(pattern);
        }
    }

    public void includeProcedure(String pattern) {
        addToCollection(includeProcedures, pattern);
    }

    public void includeProcedures(String... patterns) {
        for(String pattern: patterns) {
            includeProcedure(pattern);
        }
    }

    public void excludeProcedure(String pattern) {
        addToCollection(excludeProcedures, pattern);
    }

    public void excludeProcedures(String... patterns) {
        for(String pattern: patterns) {
            excludeProcedure(pattern);
        }
    }

    private static void addToCollection(Collection<PatternParam> collection, String name) {
        collection.add(new PatternParam(name));
    }

    <C extends org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer> C fillContainer(final C container) {
        container.setName(name);
        for(IncludeTable table : includeTables) {
            container.addIncludeTable(table.toIncludeTable());
        }
        for(PatternParam table : excludeTables) {
            container.addExcludeTable(table.toExcludeTable());
        }

        for(PatternParam column : includeColumns) {
            container.addIncludeColumn(column.toIncludeColumn());
        }
        for(PatternParam column : excludeColumns) {
            container.addExcludeColumn(column.toExcludeColumn());
        }

        for(PatternParam procedure : includeProcedures) {
            container.addIncludeProcedure(procedure.toIncludeProcedure());
        }
        for(PatternParam procedure : excludeProcedures) {
            container.addExcludeProcedure(procedure.toExcludeProcedure());
        }
        for(PatternParam rel : excludeRelationships) {
            container.addExcludeRelationship(rel.toExcludeRelationship());
        }
        return container;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}