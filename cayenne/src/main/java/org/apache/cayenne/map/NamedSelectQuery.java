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
package org.apache.cayenne.map;

import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.util.XMLEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mapped select query, defined by a String in the syntax of {@link ObjectSelect#parse(String, Object...)}.
 * The settings that the String has no syntax for (caching, page size, etc.) are the properties of the named query.
 * <p>
 * The String is stored as is and is never parsed here: the runtime parses it when the query is run, and the tools
 * that inspect the mapping parse it on their own. Unlike the other named queries, a select query has no root of its
 * own: its root entity is the "from" clause of the String.
 *
 * @since 4.0
 */
public final class NamedSelectQuery extends NamedQuery {

    // a single or double quoted string literal of the query grammar, with its backslash escapes
    private static final String QUOTED_STRING = "'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"";

    private String ql;

    /**
     * Returns the query String, exactly as it was set.
     *
     * @since 5.0
     */
    public String getQl() {
        return ql;
    }

    /**
     * Sets the query String. The String is stored as is, and is not validated.
     *
     * @since 5.0
     */
    public void setQl(String ql) {
        this.ql = ql;
    }

    /**
     * Returns a Pattern matching the references to an entity in the "from" clauses of a query String, for
     * {@link #renameEntity(Pattern, String)}.
     *
     * @param db whether the entity is a DbEntity, i.e. is referenced as "from db:NAME"
     */
    static Pattern renamePattern(String name, boolean db) {
        return Pattern.compile(QUOTED_STRING + "|(\\bfrom\\s+" + (db ? "db:\\s*" : "") + ")"
                + Pattern.quote(name) + "(?![\\w$.])", Pattern.DOTALL);
    }

    /**
     * Renames an entity in the "from" clauses of the query String
     */
    void renameEntity(Pattern reference, String newName) {
        if (ql == null) {
            return;
        }

        String renamed = reference.matcher(ql)
                .replaceAll(m -> Matcher.quoteReplacement(m.group(1) == null ? m.group() : m.group(1) + newName));
        if (!renamed.equals(ql)) {
            setQl(renamed);
        }
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor<?> delegate) {
        encoder.start("objectQuery")
                .attribute("name", getName())
                .attribute("cacheStrategy", cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheStrategy.name() : null)
                .attribute("dataRows", fetchingDataRows)
                .attribute("pageSize", pageSize)
                .attribute("statementFetchSize", statementFetchSize);

        if (ql != null && !ql.isBlank()) {
            encoder.start("ql").cdata(ql, true).end();
        }

        encodeCacheGroup(encoder);

        delegate.visitQuery(this);
        encoder.end();
    }
}
