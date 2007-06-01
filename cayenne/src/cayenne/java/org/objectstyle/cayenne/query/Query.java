/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import org.apache.log4j.Level;

/**
 * Defining a generic selecting or updating query that can be executed in Cayenne.
 * 
 * @see org.objectstyle.cayenne.access.QueryEngine
 * @author Andrei Adamchik
 */
public interface Query {

    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    /**
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public static final int SELECT_QUERY = 1;

    /**
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public static final int INSERT_QUERY = 2;

    /**
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public static final int UPDATE_QUERY = 3;

    /**
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public static final int DELETE_QUERY = 4;

    /**
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public static final int UNKNOWN_QUERY = 5;

    /**
     * Returns a symbolic name of the query.
     * 
     * @since 1.1
     */
    public String getName();

    /**
     * Sets a symbolic name of the query.
     * 
     * @since 1.1
     */
    public void setName(String name);

    /**
     * Returns the <code>logLevel</code> property of this query. Log level is a hint to
     * QueryEngine that performs this query to log execution with a certain priority.
     */
    public Level getLoggingLevel();

    public void setLoggingLevel(Level level);

    /**
     * Returns one of the values: SELECT_QUERY, INSERT_QUERY, UPDATE_QUERY, DELETE_QUERY
     * 
     * @deprecated Since 1.1 Query type is no longer relevant.
     */
    public int getQueryType();

    /**
     * Returns the root object of this query. Might be a String, ObjEntity, DbEntity or
     * Class, depending on the query in question
     * 
     * @return Object
     */
    public Object getRoot();

    /**
     * Sets the root of the query.
     * 
     * @param value The new root
     */
    public void setRoot(Object value);
}