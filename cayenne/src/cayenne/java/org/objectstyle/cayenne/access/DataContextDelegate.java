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

package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.query.GenericSelectQuery;

/**
 * Defines API for a DataContext "delegate" - an object that is temporarily passed control 
 * by DataContext at some critical points in the normal flow of execution. A delegate thus can 
 * modify the flow, abort an operation, modify the objects participating in an operation, 
 * or perform any other tasks it deems necessary. DataContextDelegate is shared by DataContext 
 * and its ObjectStore.
 * 
 * @see org.objectstyle.cayenne.access.DataContext
 * 
 * @author Mike Kienenberger
 * @author Andrus Adamchik
 * 
 * @since 1.1
 */
public interface DataContextDelegate {
	
    /** 
     * Invoked before a <code>GenericSelectQuery</code> is executed.  The delegate
     * may modify the <code>GenericSelectQuery</code> by returning a different
     * <code>GenericSelectQuery</code>, or may return null to discard the query.
     * 
     * @return the original or modified <code>GenericSelectQuery</code> or null to discard the query.
     */
	public GenericSelectQuery willPerformSelect(DataContext context, GenericSelectQuery query);

	/**
	 * Invoked by parent DataContext whenever an object change is detected.
     * This can be a change to the object snapshot, or a modification of an "independent"
     * relationship not resulting in a snapshot change. In the later case snapshot
     * argument may be null. If a delegate returns <code>true</code>, ObjectStore 
     * will attempt to merge the changes into an object.
	 */
    public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore);
    
    /**
     * Called after a successful merging of external changes to an object. 
     * If previosly a delegate returned <code>false</code> from
     * {@link #shouldMergeChanges(DataObject, DataRow)}, this method
     * is not invoked, since changes were not merged.
     */
    public void finishedMergeChanges(DataObject object);
    
    /**
     * Invoked by ObjectStore whenever it is detected that a database
     * row was deleted for object. If a delegate returns <code>true</code>,
     * ObjectStore will change MODIFIED objects to NEW (resulting in recreating the 
     * deleted record on next commit) and all other objects - to TRANSIENT. 
     * To block this behavior, delegate should return <code>false</code>, and
     * possibly do its own processing.
     * 
     * @param object DataObject that was deleted externally and is still present
     * in the ObjectStore associated with the delegate.
     */
    public boolean shouldProcessDelete(DataObject object);
    
    /**
     * Called after a successful processing of externally deleted object. 
     * If previosly a delegate returned <code>false</code> from
     * {@link #shouldProcessDelete(DataObject)}, this method
     * is not invoked, since no processing was done.
     */
    public void finishedProcessDelete(DataObject object);
}

