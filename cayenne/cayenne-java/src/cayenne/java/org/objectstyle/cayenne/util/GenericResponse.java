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
package org.objectstyle.cayenne.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.QueryResponse;

/**
 * A simple serializable implementation of QueryResponse.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class GenericResponse implements QueryResponse, Serializable {

    protected List results;

    protected transient int currentIndex;

    /**
     * Creates an empty BaseResponse.
     */
    public GenericResponse() {
        results = new ArrayList();
    }

    /**
     * Creates a BaseResponse with a single result list.
     */
    public GenericResponse(List list) {
        results = new ArrayList(1);
        addResultList(list);
    }

    /**
     * Creates a response that it a shallow copy of another response.
     */
    public GenericResponse(QueryResponse response) {

        results = new ArrayList(response.size());

        response.reset();
        while (response.next()) {
            if (response.isList()) {
                addResultList(response.currentList());
            }
            else {
                addBatchUpdateCount(response.currentUpdateCount());
            }
        }
    }

    public List firstList() {
        for (reset(); next();) {
            if (isList()) {
                return currentList();
            }
        }

        return null;
    }

    public int[] firstUpdateCount() {
        for (reset(); next();) {
            if (!isList()) {
                return currentUpdateCount();
            }
        }

        return null;
    }

    public List currentList() {
        return (List) results.get(currentIndex - 1);
    }

    public int[] currentUpdateCount() {
        return (int[]) results.get(currentIndex - 1);
    }

    public boolean isList() {
        return results.get(currentIndex - 1) instanceof List;
    }

    public boolean next() {
        return ++currentIndex <= results.size();
    }

    public void reset() {
        // use a zero-based index, not -1, as this will simplify serialization handling
        currentIndex = 0;
    }

    public int size() {
        return results.size();
    }

    /**
     * Clears any previously collected information.
     */
    public void clear() {
        results.clear();
    }

    public void addBatchUpdateCount(int[] resultCount) {

        if (resultCount != null) {
            results.add(resultCount);
        }
    }

    public void addUpdateCount(int resultCount) {
        results.add(new int[] {
            resultCount
        });
    }

    public void addResultList(List list) {
        this.results.add(list);
    }

    /**
     * Replaces previously stored result with a new result.
     */
    public void replaceResult(Object oldResult, Object newResult) {
        int index = results.indexOf(oldResult);
        if (index >= 0) {
            results.set(index, newResult);
        }
    }
}
