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
package org.objectstyle.cayenne.unit.util;

import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class MockDataObject implements DataObject {

    public DataContext getDataContext() {
        return null;
    }

    public void setDataContext(DataContext ctxt) {
    }

    public ObjectId getObjectId() {
        return null;
    }

    public void setObjectId(ObjectId objectId) {
    }

    public int getPersistenceState() {
        return 0;
    }

    public void setPersistenceState(int newState) {
    }

    public void writePropertyDirectly(String propertyName, Object val) {
    }

    public Object readPropertyDirectly(String propertyName) {
        return null;
    }

    public Object readNestedProperty(String path) {
        return null;
    }

    public Object readProperty(String propName) {
        return null;
    }

    public void writeProperty(String propName, Object val) {
    }

    public DataObject readToOneDependentTarget(String relName) {
        return null;
    }

    public void addToManyTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void setToOneTarget(String relName, DataObject val, boolean setReverse) {
    }

    public void setToOneDependentTarget(String relName, DataObject val) {
    }

    public Map getCommittedSnapshot() {
        return null;
    }

    public Map getCurrentSnapshot() {
        return null;
    }

    public void fetchFinished() {
    }

    public long getSnapshotVersion() {
        return 0;
    }

    public void setSnapshotVersion(long snapshotVersion) {
    }

    public void resolveFault() {
    }

    public void validateForInsert(ValidationResult validationResult) {
    }

    public void validateForUpdate(ValidationResult validationResult) {
    }

    public void validateForDelete(ValidationResult validationResult) {
    }
}