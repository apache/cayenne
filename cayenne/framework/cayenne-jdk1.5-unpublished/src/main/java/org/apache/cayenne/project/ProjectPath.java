/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.project;

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
    public <T> T firstInstanceOf(Class<T> aClass) {
        for (Object aPath : path) {
            if (aPath != null && aClass.isAssignableFrom(aPath.getClass())) {
                return (T) aPath;
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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
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

    @Override
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
