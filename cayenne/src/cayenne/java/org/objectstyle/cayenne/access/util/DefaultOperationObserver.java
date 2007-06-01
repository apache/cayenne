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

package org.objectstyle.cayenne.access.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * Simple implementation of OperationObserver interface.
 * Useful as a superclass of other implementations of OperationObserver. 
 * This implementation only tracks transaction events and exceptions. 
 * 
 * <p><i>This operation observer is unsafe to use in application, since it doesn't
 * rethrow the exceptions immediately, and may cause the database to hang. 
 * Use {@link org.objectstyle.cayenne.access.QueryResult} instead.</i></p>
 *
 * @author Andrei Adamchik
 */
public class DefaultOperationObserver implements OperationObserver {
    private static Logger logObj =
        Logger.getLogger(DefaultOperationObserver.class);

    public static final Level DEFAULT_LOG_LEVEL = Query.DEFAULT_LOG_LEVEL;

    protected List globalExceptions = new ArrayList();
    protected Map queryExceptions = new HashMap();
    protected Level loggingLevel = DEFAULT_LOG_LEVEL;

    /**
     * Prints the information about query and global exceptions. 
     */
    public void printExceptions(PrintWriter out) {
        if (globalExceptions.size() > 0) {
            if (globalExceptions.size() == 1) {
                out.println("Global Exception:");
            } else {
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
            } else {
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

    /** Returns <code>true</code> if at least one exception was registered
      * during query execution. */
    public boolean hasExceptions() {
        return globalExceptions.size() > 0 || queryExceptions.size() > 0;
    }

    /**
     * @return always returns false.
     * @deprecated since 1.1 this is deprecated
     */
    public boolean isTransactionCommitted() {
        return false;
    }

    /**
     * @return always returns false.
     * @deprecated since 1.1 this is deprecated
     */
    public boolean isTransactionRolledback() {
        return false;
    }

    /**
     * Returns a log level level that should be used when
     * logging query execution.
     */
    public Level getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * Sets log level that should be used for queries.
     * If <code>level</code> argument is null, level is set to
     * DEFAULT_LOG_LEVEL. If <code>level</code> is equal or higher
     * than log level configured for QueryLogger, query SQL statements
     * will be logged.
     */
    public void setLoggingLevel(Level level) {
        this.loggingLevel = (level == null) ? DEFAULT_LOG_LEVEL : level;
    }

    public void nextCount(Query query, int resultCount) {
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        if (logObj.isDebugEnabled()) {
            for (int i = 0; i < resultCount.length; i++) {
                logObj.debug("batch count: " + resultCount[i]);
            }
        }
    }

    public void nextDataRows(Query query, List dataRows) {
    }

    public void nextDataRows(Query q, ResultIterator it) {
        logObj.debug("result: (iterator)");
    }

    public void nextQueryException(Query query, Exception ex) {
        queryExceptions.put(query, Util.unwindException(ex));
    }

    public void nextGlobalException(Exception ex) {
        globalExceptions.add(Util.unwindException(ex));
    }

    /**
     * @deprecated since 1.1
     */
    public void transactionCommitted() {
        // noop
    }

    /**
      * @deprecated since 1.1
      */
    public void transactionRolledback() {
        // noop
    }

    /** 
     * Returns <code>true</code> so that individual queries are executed in separate transactions. 
     * 
     * @deprecated Since 1.1 this method is no longer used by Cayenne.
     */
    public boolean useAutoCommit() {
        return true;
    }

    /**
     * Returns <code>false</code>.
     */
    public boolean isIteratedResult() {
        return false;
    }
}
