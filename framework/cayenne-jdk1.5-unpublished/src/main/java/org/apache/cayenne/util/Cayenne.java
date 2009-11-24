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
package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * Various utils for processing persistent objects and their properties
 * @since 3.1 
 */
public final class Cayenne {

    /**
     * Returns mapped ObjEntity for object. If an object is transient or is not
     * mapped returns null.
     */
    public static ObjEntity getObjEntity(Persistent p) {
        return (p.getObjectContext() != null) ? p.getObjectContext()
                .getEntityResolver()
                .lookupObjEntity(p) : null;
    }
    
    /**
     * Returns class descriptor for the object, <code>null</code> if the object is
     * transient or descriptor was not found
     */
    public static ClassDescriptor getClassDescriptor(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.TRANSIENT) {
            return null;
         }
         
         return object.getObjectContext().getEntityResolver().getClassDescriptor(
                 object.getObjectId().getEntityName());
    }
    
    /**
     * Returns property desctiptor for specified property
     * @param properyName path to the property
     * @return property descriptor, <code>null</code> if not found
     */
    public static Property getProperty(Persistent object, String properyName) {
        ClassDescriptor descriptor = getClassDescriptor(object);
        if (descriptor == null) {
            return null;
        }
        return descriptor.getProperty(properyName);
    }   
    
    /**
     * Returns a value of the property identified by a property path. Supports reading
     * both mapped and unmapped properties. Unmapped properties are accessed in a manner
     * consistent with JavaBeans specification.
     * <p>
     * Property path (or nested property) is a dot-separated path used to traverse object
     * relationships until the final object is found. If a null object found while
     * traversing path, null is returned. If a list is encountered in the middle of the
     * path, CayenneRuntimeException is thrown. Unlike
     * {@link #readPropertyDirectly(String)}, this method will resolve an object if it is
     * HOLLOW.
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>Read this object property:<br>
     * <code>String name = (String)CayenneUtils.readNestedProperty(artist, "name");</code><br>
     * <br>
     * </li>
     * <li>Read an object related to this object:<br>
     * <code>Gallery g = (Gallery)CayenneUtils.readNestedProperty(paintingInfo, "toPainting.toGallery");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read a property of an object related to this object: <br>
     * <code>String name = (String)CayenneUtils.readNestedProperty(painting, "toArtist.artistName");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship list:<br>
     * <code>List exhibits = (List)CayenneUtils.readNestedProperty(painting, "toGallery.exhibitArray");</code>
     * <br>
     * <br>
     * </li>
     * <li>Read to-many relationship in the middle of the path:<br>
     * <code>List<String> names = (List<String>)CayenneUtils.readNestedProperty(artist, "paintingArray.paintingName");</code>
     * <br>
     * <br>
     * </li>
     * </ul>
     */
    public static Object readNestedProperty(Persistent p, String path) {
        return readNestedProperty(p, path, tokenizePath(path), 0, 0);
    }

    /**
     * Recursively resolves nested property path
     */
    private static Object readNestedProperty(
            Persistent p,
            String path,
            String[] tokenizedPath,
            int tokenIndex,
            int pathIndex) {

        Object property = readSimpleProperty(p, tokenizedPath[tokenIndex]);

        if (tokenIndex == tokenizedPath.length - 1) { // last component
            return property;
        }

        pathIndex += tokenizedPath[tokenIndex].length() + 1;
        if (property == null) {
            return null;
        }
        else if (property instanceof Persistent) {
            return readNestedProperty(
                    (Persistent) property,
                    path,
                    tokenizedPath,
                    tokenIndex + 1,
                    tokenIndex);
        }
        else if (property instanceof Collection) {
            /**
             * Support for collection property in the middle of the path
             */
            Collection<Object> result = property instanceof List
                    ? new ArrayList<Object>()
                    : new HashSet<Object>();
            for (Object obj : (Collection<?>) property) {
                if (obj instanceof CayenneDataObject) {
                    Object rest = readNestedProperty(
                            (CayenneDataObject) obj,
                            path,
                            tokenizedPath,
                            tokenIndex + 1,
                            tokenIndex);
                    if (rest instanceof Collection) {
                        /**
                         * We don't want nested collections. E.g.
                         * readNestedProperty("paintingArray.paintingTitle") should return
                         * List<String>
                         */
                        result.addAll((Collection<?>) rest);
                    }
                    else {
                        result.add(rest);
                    }
                }
            }
            return result;
        }
        else {
            // read the rest of the path via introspection
            return PropertyUtils.getProperty(property, path.substring(pathIndex));
        }
    }

    private static final String[] tokenizePath(String path) {
        if (path == null) {
            throw new NullPointerException("Null property path.");
        }

        if (path.length() == 0) {
            throw new IllegalArgumentException("Empty property path.");
        }

        // take a shortcut for simple properties
        if (!path.contains(".")) {
            return new String[] {
                path
            };
        }

        StringTokenizer tokens = new StringTokenizer(path, ".");
        int length = tokens.countTokens();
        String[] tokenized = new String[length];
        for (int i = 0; i < length; i++) {
            String temp = tokens.nextToken();
            if (temp.endsWith("+")) {
                tokenized[i] = temp.substring(0, temp.length() - 1);
            }
            else {
                tokenized[i] = temp;
            }
        }
        return tokenized;
    }

    private static final Object readSimpleProperty(Persistent p, String propertyName) {
        Property property = getProperty(p, propertyName);

        if (property != null) {
            // side effect - resolves HOLLOW object
            return property.readProperty(p);
        }
        
        //handling non-persistent property
        Object result = null;
        if (p instanceof DataObject) {
            result = ((DataObject) p).readPropertyDirectly(propertyName);
        }
        
        if (result != null) {
            return result;
        }
     
        //there is still a change to return a property via introspection
        return PropertyUtils.getProperty(p, propertyName);
    }
    
    static void setReverse(
            final Persistent sourceObject,
            String propertyName,
            final Persistent targetObject) {
        
        ArcProperty property = (ArcProperty) getClassDescriptor(sourceObject).
            getProperty(propertyName);
        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitToMany(ToManyProperty property) {
                    property.addTargetDirectly(targetObject, sourceObject);
                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    property.setTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitAttribute(AttributeProperty property) {
                    return false;
                }

            });
            
            sourceObject.getObjectContext().getGraphManager().arcCreated(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());
    
            markAsDirty(targetObject);
        }
    }

    static void unsetReverse(
            final Persistent sourceObject,
            String propertyName,
            final Persistent targetObject) {
        
        ArcProperty property = (ArcProperty) getClassDescriptor(sourceObject).
            getProperty(propertyName);
        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitToMany(ToManyProperty property) {
                    property.removeTargetDirectly(targetObject, sourceObject);
                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    property.setTarget(targetObject, null, false);
                    return false;
                }

                public boolean visitAttribute(AttributeProperty property) {
                    return false;
                }

            });
            
            sourceObject.getObjectContext().getGraphManager().arcDeleted(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }
    
    /**
     * Changes object state to MODIFIED if needed, returning true if the change has
     * occurred, false if not.
     */
    static boolean markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
            return true;
        }

        return false;
    }
    
    private Cayenne() {}
}
