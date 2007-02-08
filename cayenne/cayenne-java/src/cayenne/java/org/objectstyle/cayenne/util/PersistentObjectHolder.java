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

import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.FaultFailureException;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.ValueHolder;

/**
 * A ValueHolder implementation that holds a single Persistent object related to an object
 * used to initialize PersistentObjectHolder. Value is resolved on first access.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PersistentObjectHolder extends RelationshipFault implements ValueHolder {

    protected boolean fault;
    protected Object value;

    // exists for the benefit of manual serialization schemes such as the one in Hessian.
    private PersistentObjectHolder() {
        fault = true;
    }

    public PersistentObjectHolder(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
        fault = !isTransientParent();
    }

    /**
     * Returns true if this holder is not resolved, meaning its object is not yet known.
     */
    public boolean isFault() {
        return fault;
    }

    public void invalidate() {
        fault = true;
        value = null;
    }

    /**
     * Returns a value resolving it via a query on the first call to this method.
     */
    public Object getValue() throws CayenneRuntimeException {

        if (fault) {
            resolve();
        }

        return value;
    }
    
    public Object getValueDirectly() throws CayenneRuntimeException {
        return value;
    }

    /**
     * Sets an object value, marking this ValueHolder as resolved.
     */
    public synchronized Object setValue(Object value) throws CayenneRuntimeException {

        if (fault) {
            resolve();
        }

        Object oldValue = setValueDirectly(value);

        if (oldValue != value) {
            // notify ObjectContext
            if (relationshipOwner.getObjectContext() != null) {
                relationshipOwner.getObjectContext().propertyChanged(
                        relationshipOwner,
                        relationshipName,
                        oldValue,
                        value);
            }
        }

        return oldValue;
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {

        // must obtain the value from the local context
        if (value instanceof Persistent) {
            value = connect((Persistent) value);
        }

        Object oldValue = this.value;

        this.value = value;
        this.fault = false;

        return oldValue;
    }

    /**
     * Returns an object that should be stored as a value in this ValueHolder, ensuring
     * that it is registered with the same context.
     */
    protected Object connect(Persistent persistent) {

        if (persistent == null) {
            return null;
        }

        if (relationshipOwner.getObjectContext() != persistent.getObjectContext()) {
            throw new CayenneRuntimeException(
                    "Cannot set object as destination of relationship "
                            + relationshipName
                            + " because it is in a different ObjectContext");
        }

        return persistent;
    }

    /**
     * Reads an object from the database.
     */
    protected synchronized void resolve() {
        if (!fault) {
            return;
        }

        // TODO: should build a HOLLOW object instead of running a query if relationship
        // is required and thus expected to be not null.

        List objects = resolveFromDB();

        if (objects.size() == 0) {
            this.value = null;
        }
        else if (objects.size() == 1) {
            this.value = objects.get(0);
        }
        else {
            throw new FaultFailureException(
                    "Expected either no objects or a single object, instead fault query resolved to "
                            + objects.size()
                            + " objects.");
        }

        fault = false;
    }
}
