/*
 * ====================================================================
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
package org.objectstyle.cayenne.project;

import java.util.Arrays;

/**
 * Immutable holder of a selection path within a Cayenne project. Mostly used
 * by various tools (CayenneModeler comes to mind) to navigate Cayenne
 * projects. Contains a number of convenience methods to access path elements.
 * 
 * <p>
 * For instance, given a path <code>Project -> DataMap -> ObjEntity -> ObjAttribute</code>,
 * <code>getObject</code> will return ObjAttribute, <code>getObjectParent</code>-
 * ObjEntity, <code>getRoot</code>- Project.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ProjectPath {
    public static final Object[] EMPTY_PATH = new Object[0];

    protected Object[] path;

    public ProjectPath() {
        path = EMPTY_PATH;
    }

    /**
     * Constructor for ProjectPath.
     */
    public ProjectPath(Object object) {
        path = new Object[] { object };
    }

    /**
     * Constructor for ProjectPath.
     */
    public ProjectPath(Object[] path) {
        this.path = (path != null) ? path : EMPTY_PATH;
    }

    public Object[] getPath() {
        return path;
    }

    public boolean isEmpty() {
        return path == null || path.length == 0;
    }

    /**
     * Scans path, looking for the first element that is an instanceof <code>aClass</code>.
     */
    public Object firstInstanceOf(Class aClass) {
        for (int i = 0; i < path.length; i++) {
            if (path[i] != null && aClass.isAssignableFrom(path[i].getClass())) {
                return path[i];
            }
        }

        return null;
    }

    /**
     * Returns an instance of the path, expanding this one by appending an
     * object at the end.
     */
    public ProjectPath appendToPath(Object object) {
        if (object != null) {
            Object[] newPath = new Object[path.length + 1];

            if (path.length > 0) {
                System.arraycopy(path, 0, newPath, 0, path.length);
            }
            newPath[path.length] = object;
            return new ProjectPath(newPath);
        }
        else {
            return this;
        }
    }

    /**
     * 
     * @since 1.1
     */
    public ProjectPath subpathWithSize(int subpathSize) {
        if (subpathSize <= 0) {
            return new ProjectPath();
        }
        else if(subpathSize == path.length) {
            return this;
        }

        if (subpathSize > path.length) {
            throw new ArrayIndexOutOfBoundsException(
                "Subpath is longer than this path "
                    + subpathSize
                    + " components. Path size: "
                    + path.length);
        }

        Object[] newPath = new Object[subpathSize];
        System.arraycopy(path, 0, newPath, 0, subpathSize);
        return new ProjectPath(newPath);
    }

    /**
     * Returns a subpath to the first occurance of an object.
     * 
     * @since 1.1
     */
    public ProjectPath subpathOfObject(Object object) {
        for (int i = 0; i < path.length; i++) {
            if (path[i] == object) {
                // strip remaining objects
                return subpathWithSize(i + 1);
            }
        }

        return null;
    }

    /**
     * Returns the root or starting object of the path.
     */
    public Object getRoot() {
        if (path.length == 0) {
            return null;
        }

        return path[0];
    }

    /**
     * Returns the last object in the path.
     */
    public Object getObject() {
        if (path.length == 0) {
            return null;
        }

        // return last object
        return path[path.length - 1];
    }

    /**
     * Returns an object corresponding to the parent node of the node
     * represented by the path. This is the object next to last object in the
     * path.
     */
    public Object getObjectParent() {
        if (path.length == 0) {
            return null;
        }

        // return next to last object
        return (path.length > 1) ? path[path.length - 2] : null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[ProjectPath: ");
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }

            String token = (path[i] != null) ? path[i].getClass().getName() : "<null>";
            buf.append(token);
        }
        buf.append("]");
        return buf.toString();
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof ProjectPath)) {
            return false;
        }

        ProjectPath otherPath = (ProjectPath) object;
        return Arrays.equals(getPath(), otherPath.getPath());
    }
}
