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
package org.objectstyle.cayenne.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.Util;

/**
 * Represents a result of a validation execution. Contains a set of 
 * {@link ValidationFailure ValidationFailures} that occured in a given 
 * context. All failures are kept in the same order they were added.
 * 
 * @author Fabricio Voznika
 * @since 1.1
 */
public class ValidationResult implements Serializable {
    private static final Logger logObj = Logger.getLogger(ValidationResult.class);

    private List failures;

    public ValidationResult() {
        failures = new ArrayList();
    }

    /**
     * Add a failure to the validation result.
     *
     * @param failure failure to be added. It may not be null.
     * @see ValidationFailure
     */
    public void addFailure(ValidationFailure failure) {
        if (failure == null) {
            throw new IllegalArgumentException("failure cannot be null.");
        }

        if (logObj.isDebugEnabled()) {
            logObj.debug(failure);
        }

        failures.add(failure);
    }

    /**
     * Returns all failures added to this result, or empty list is result
     * has no failures.
     */
    public List getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /**
     * Returns all failures related to the <code>source</code> object, or an empty
     * list if there are no such failures.
     * 
     * @param source it may be null.
     * @see ValidationFailure#getSource()
     */
    public List getFailures(Object source) {

        ArrayList matchingFailures = new ArrayList(5);
        Iterator it = failures.iterator();
        while (it.hasNext()) {
            ValidationFailure failure = (ValidationFailure) it.next();
            if (Util.nullSafeEquals(source, failure.getSource())) {
                matchingFailures.add(failure);
            }
        }

        return matchingFailures;
    }

    /**
     * Returns true if at least one failure has been added to this result. False otherwise.
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    /**
     * @param source it may be null.
     * @return true if there is at least one failure for <code>source</code>. False otherwise.
     */
    public boolean hasFailures(Object source) {
        Iterator it = failures.iterator();
        while (it.hasNext()) {
            ValidationFailure failure = (ValidationFailure) it.next();
            if (Util.nullSafeEquals(source, failure.getSource())) {
                return true;
            }
        }
        
        return false;
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        String separator = System.getProperty("line.separator");

        Iterator it = getFailures().iterator();
        while (it.hasNext()) {
            if (ret.length() > 0) {
                ret.append(separator);
            }

            ret.append(it.next());
        }

        return ret.toString();
    }
}