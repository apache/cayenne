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
package org.objectstyle.cayenne;

/** 
 * Defines a set of persistence states for DataObjects. PersistenceState describes
 * the state of data stored in a DataObject relative to the external persistence store. If an object's
 * state matches the state of the persistence store, the object is COMMITTED. If object is not 
 * intended to be persistent or is not explicitly made persistent, the state is TRANSIENT, and so on.
 * 
 * <p>DataObject persistence states should not be modified directly. Rather it is a responsibility
 * of a DataContext to maintain correct state of the objects that it manages.
 * 
 * @author Andrei Adamchik 
 */
public class PersistenceState {

    /** 
     * Returns String label for persistence state. 
     * Used for debugging. 
     */
    public static String persistenceStateName(int persistenceState) {
        switch (persistenceState) {
            case PersistenceState.TRANSIENT :
                return "transient";
            case PersistenceState.NEW :
                return "new";
            case PersistenceState.MODIFIED :
                return "modified";
            case PersistenceState.COMMITTED :
                return "committed";
            case PersistenceState.HOLLOW :
                return "hollow";
            case PersistenceState.DELETED :
                return "deleted";
            default :
                return "unknown";
        }
    }

    /** Describes a state of an object not registered with DataContext,
     *  and therefore having no persistence features.
     */
    public static final int TRANSIENT = 1;
    
    /** Describes a state of an object freshly registered with DataContext,
     *  but not committed to the database yet. So there is no corresponding
     *  database record for this object just yet. */
    public static final int NEW = 2;
    
    /** Describes a state of an object registered with DataContext,
     *  whose fields exactly match the state of a corresponding database row.
     *  This state is not fully "clean", since database record
     *  may have been externally modified. */
    public static final int COMMITTED = 3;
    
    
    /** Describes a state of an object registered with DataContext,
     *  and having a corresponding database row. This object state
     *  is known to be locally modified and different from the database
     *  state. */
    public static final int MODIFIED = 4;
    
    /** Describes a state of an object registered with DataContext,
     *  and having a corresponding database row. This object does not
     *  store any fields except for its id (it is "hollow"), so next 
     *  time it is accessed, it will be populated from the database 
     *  by the context. In this respect this is a real "clean"
     *  object. */
    public static final int HOLLOW = 5;
    
    /** Describes a state of an object registered with DataContext,
     *  that will be deleted from the database on the next commit. */
    public static final int DELETED = 6;
}

