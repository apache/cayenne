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

import org.objectstyle.cayenne.util.Util;

/**
 * A FlattenedObjectId is a class that uniquely identifies 
 * an object that is the destination of a flattened relationship
 * that is defined by a series of toOne relationships.
 * 
 * <p>It encapsulates enough information (relationship, source object 
 * pk etc.) in order for DataContext to fetch the appropriate row
 * when the object is touched
 * </p>
 * 
 * @deprecated Since 1.1 FlattenedObjectId is deprecated and is no longer used in
 * Cayenne since it is not a valid ObjectId. Faults are used instead.
 * 
 * @author Craig Miskell
 */
public class FlattenedObjectId extends ObjectId {
	protected String relationshipName;
	protected DataObject sourceObject;

	/**
	 * Constructs a FlattenedObjectId.
	 */
	public FlattenedObjectId(
		Class objClass,
		DataObject aSourceObject,
		String aRelationshipName) {

		super(objClass, null);
		this.relationshipName = aRelationshipName;
		this.sourceObject = aSourceObject;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof FlattenedObjectId)) {
			return false;
		}

		if (this == object) {
			return true;
		}

		FlattenedObjectId id = (FlattenedObjectId) object;
		// use the class name because two Objectid's should be equal
		// even if their objClass'es were loaded by different class loaders.
		return objectClass.getName().equals(id.objectClass.getName())
			&& Util.nullSafeEquals(id.objectIdKeys, this.objectIdKeys)
			&& Util.nullSafeEquals(id.relationshipName, this.relationshipName);

	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int mapHash = (objectIdKeys != null) ? objectIdKeys.hashCode() : 0;
		// use the class name because two Objectid's should be equal
		// even if their objClass'es were loaded by different class loaders.
		return relationshipName.hashCode()
			+ objectClass.getName().hashCode()
			+ mapHash;
	}

	/**
	 * @see org.objectstyle.cayenne.ObjectId#isTemporary()
	 */
	public boolean isTemporary() {
		return true;
	}
	/**
	 * Returns the name of the flattened relationship from
	 * the source object (identified by sourceIdSnapshot) that
	 * leads to the object that this id represents
	 * @return String
	 */
	public String getRelationshipName() {
		return relationshipName;
	}

	/**
	 * Returns the id snapshot of the source object
	 * @return Map
	 */
	public DataObject getSourceObject() {
		return this.sourceObject;
	}

}
