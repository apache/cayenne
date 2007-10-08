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

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * A convenience superclass of the queries that resolve into some other queries during the
 * routing phase. Provides caching of a replacement query.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class IndirectQuery implements Query {

    protected String name;

    protected transient Query replacementQuery;
    protected transient EntityResolver lastResolver;

    /**
     * Returns the metadata obtained from the replacement query.
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return getReplacementQuery(resolver).getMetaData(resolver);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Delegates routing to a replacement query.
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        getReplacementQuery(resolver).route(
                router,
                resolver,
                substitutedQuery != null ? substitutedQuery : this);
    }

    /**
     * Creates a substitute query. An implementor is free to provide an arbitrary
     * replacement query.
     */
    protected abstract Query createReplacementQuery(EntityResolver resolver);

    /**
     * Returns a replacement query, creating it on demand and caching it for reuse.
     */
    protected Query getReplacementQuery(EntityResolver resolver) {
        if (replacementQuery == null || lastResolver != resolver) {
            this.replacementQuery = createReplacementQuery(resolver);
            this.lastResolver = resolver;
        }

        return replacementQuery;
    }

    /**
     * Throws an exception as indirect query should not be executed directly.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this.getClass().getName()
                + " is an indirect query and doesn't support its own sql actions. "
                + "It should've been delegated to another "
                + "query during resolution or routing phase.");
    }

    /**
     * This implementation throws an exception.
     * 
     * @deprecated since 1.2
     */
    public Object getRoot() {
        throw new CayenneRuntimeException("This deprecated method is not implemented");
    }

    /**
     * Throws an exception.
     * 
     * @deprecated since 1.2
     */
    public void setRoot(Object root) {
        throw new CayenneRuntimeException("This deprecated method is not implemented");
    }
}
