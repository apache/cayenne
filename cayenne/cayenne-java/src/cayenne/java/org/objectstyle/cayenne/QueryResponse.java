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
package org.objectstyle.cayenne;

import java.util.List;

/**
 * Represents a result of query execution. It potentially contain a mix of update counts
 * and lists of selected values. Provides API somewhat similar to java.util.Iterator or
 * java.sql.ResultSet for scanning through the individual results.
 * <p>
 * An example of iterating through a response:
 * </p>
 * 
 * <pre>
 * QueryResponse response = context.performGenericQuery(query);
 * for (response.reset(); response.next();) {
 *     if (response.isList()) {
 *         List list = response.currentList();
 *         // ...
 *     }
 *     else {
 *         int[] updateCounts = reponse.currentUpdateCount();
 *         // ...
 *     }
 * }
 * </pre>
 * 
 * <p>
 * In case the structure of the result is known, and only a single list or an update count
 * is expected, there is a simpler API to access them:
 * </p>
 * 
 * <pre>
 * QueryResponse response = context.performGenericQuery(query);
 * List list = response.firstList();
 * int[] count = response.firstUpdateCount();
 * </pre>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface QueryResponse {

    /**
     * Returns a number of results in the response.
     */
    int size();

    /**
     * Returns whether current iteration result is a list or an update count.
     */
    boolean isList();

    /**
     * Returns a List under the current iterator position. Use {@link #isList()} to check
     * the result type before calling this method.
     */
    List currentList();

    /**
     * Returns an update count under the current iterator position. Returned value is an
     * int[] to accomodate batch queries. For a regular update result, the value will be
     * an int[1]. Use {@link #isList()} to check the result type before calling this
     * method.
     */
    int[] currentUpdateCount();

    /**
     * Rewinds response iterator to the next result, returning true if it is available.
     */
    boolean next();

    /**
     * Restarts response iterator.
     */
    void reset();

    /**
     * A utility method for quickly retrieving the first list in the response. Note that
     * this method resets current iterator to an undefined state.
     */
    List firstList();

    /**
     * A utility method for quickly retrieving the first update count from the response.
     * Note that this method resets current iterator to an undefined state.
     */
    int[] firstUpdateCount();
}
