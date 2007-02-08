/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

import java.io.Serializable;

import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * Defines minimal API of a query descriptor that is executable via Cayenne.
 * 
 * @author Andrus Adamchik
 */
public interface Query extends Serializable {

    /**
     * Returns query runtime parameters. The method is called at various stages of the
     * execution by Cayenne access stack to retrieve query parameters. EntityResolver
     * instance is passed to this method, meaning that the query doesn't need to store
     * direct references to Cayenne mapping objects and can resolve them at runtime.
     * 
     * @since 1.2
     */
    QueryMetadata getMetaData(EntityResolver resolver);

    /**
     * A callback method invoked by Cayenne during the routing phase of the query
     * execution. Mapping of DataNodes is provided by QueryRouter. Query should use a
     * {@link QueryRouter#route(QueryEngine, Query, Query)} callback method to route
     * itself. Query can create one or more substitute queries or even provide its own
     * QueryEngine to execute itself.
     * 
     * @since 1.2
     */
    void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery);

    /**
     * A callback method invoked by Cayenne during the final execution phase of the query
     * run. A concrete query implementation is given a chance to decide how it should be
     * handled. Implementors can pick an appropriate method of the SQLActionVisitor to
     * handle itself, create a custom SQLAction of its own, or substitute itself with
     * another query that should be used for SQLAction construction.
     * 
     * @since 1.2
     */
    SQLAction createSQLAction(SQLActionVisitor visitor);

    /**
     * Returns a symbolic name of the query. The name may be used as a key to find queries
     * stored in the DataMap. Some query implementors reuse the name as a QueryMetadata
     * cache key. Generally the name can be null.
     * 
     * @since 1.1
     */
    String getName();

    /**
     * Sets a symbolic name of the query.
     * 
     * @since 1.1
     * @deprecated since 1.2
     */
    void setName(String name);

    /**
     * Returns the root object of the query.
     * 
     * @deprecated since 1.2. Query "root" is now accessed via
     *             {@link #getMetaData(EntityResolver)}.
     */
    Object getRoot();

    /**
     * Sets the root of the query.
     * 
     * @deprecated since 1.2. Query "root" is now accessed via
     *             {@link #getMetaData(EntityResolver)}.
     */
    void setRoot(Object root);
}