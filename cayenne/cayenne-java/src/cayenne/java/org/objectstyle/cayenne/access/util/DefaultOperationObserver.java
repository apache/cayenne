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

package org.objectstyle.cayenne.access.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * Simple implementation of OperationObserver interface. Useful as a superclass of other
 * implementations of OperationObserver. This implementation only tracks transaction
 * events and exceptions.
 * <p>
 * <i>This operation observer is unsafe to use in application, since it doesn't rethrow
 * the exceptions immediately, and may cause the database to hang.</i>
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class DefaultOperationObserver implements OperationObserver {


    /**
     * @deprecated Unused since 1.2
     */
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    protected List globalExceptions = new ArrayList();
    protected Map queryExceptions = new HashMap();

    /**
     * Prints the information about query and global exceptions.
     */
    public void printExceptions(PrintWriter out) {
        if (globalExceptions.size() > 0) {
            if (globalExceptions.size() == 1) {
                out.println("Global Exception:");
            }
            else {
                out.println("Global Exceptions:");
            }

            Iterator it = globalExceptions.iterator();
            while (it.hasNext()) {
                Throwable th = (Throwable) it.next();
                th.printStackTrace(out);
            }
        }

        if (queryExceptions.size() > 0) {
            if (queryExceptions.size() == 1) {
                out.println("Query Exception:");
            }
            else {
                out.println("Query Exceptions:");
            }

            Iterator it = queryExceptions.keySet().iterator();
            while (it.hasNext()) {
                Throwable th = (Throwable) queryExceptions.get(it.next());
                th.printStackTrace(out);
            }
        }
    }

    /** Returns a list of global exceptions that occured during data operation run. */
    public List getGlobalExceptions() {
        return globalExceptions;
    }

    /** Returns a list of exceptions that occured during data operation run by query. */
    public Map getQueryExceptions() {
        return queryExceptions;
    }

    /**
     * Returns <code>true</code> if at least one exception was registered during query
     * execution.
     */
    public boolean hasExceptions() {
        return globalExceptions.size() > 0 || queryExceptions.size() > 0;
    }

    /**
     * Returns a log level level that should be used when logging query execution.
     * 
     * @deprecated since 1.2
     */
    public Level getLoggingLevel() {
        return Level.INFO;
    }

    /**
     * Sets log level that should be used for queries. If <code>level</code> argument is
     * null, level is set to DEFAULT_LOG_LEVEL. If <code>level</code> is equal or higher
     * than log level configured for QueryLogger, query SQL statements will be logged.
     * 
     * @deprecated since 1.2
     */
    public void setLoggingLevel(Level level) {
        // noop
    }

    public void nextCount(Query query, int resultCount) {
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        
    }

    public void nextDataRows(Query query, List dataRows) {
        // noop
    }

    /**
     * Closes ResultIterator without reading its data. If you implement a custom subclass,
     * only call super if closing the iterator is what you need.
     */
    public void nextDataRows(Query query, ResultIterator it) {
        if (it != null) {
            try {
                it.close();
            }
            catch (CayenneException ex) {
                // don't throw here....
                nextQueryException(query, ex);
            }
        }
    }

    /**
     * Closes ResultIterator without reading its data. If you implement a custom subclass,
     * only call super if closing the iterator is what you need.
     * 
     * @since 1.2
     */
    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        if (keysIterator != null) {
            try {
                keysIterator.close();
            }
            catch (CayenneException ex) {
                // don't throw here....
                nextQueryException(query, ex);
            }
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        queryExceptions.put(query, Util.unwindException(ex));
    }

    public void nextGlobalException(Exception ex) {
        globalExceptions.add(Util.unwindException(ex));
    }

    /**
     * Returns <code>false</code>.
     */
    public boolean isIteratedResult() {
        return false;
    }
}