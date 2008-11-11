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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.project.ProjectPath;

/**
 * Defines tree traversal utility methods. Object tree sematics is determined using
 * {@link org.apache.cayenne.util.TreeNodeChild} property getter annotation.
 * 
 */
public class TraversalUtil {

    static final ClassTraversalDescriptor noopDescriptor = new ClassTraversalDescriptor();
    static final Map<String, ClassTraversalDescriptor> descriptors = new HashMap<String, ClassTraversalDescriptor>();

    private static Method[] traversableGetters(Class<?> nodeType) {

        Collection<Method> getters = null;

        Method[] methods = nodeType.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isAnnotationPresent(TreeNodeChild.class)
                    && !Void.TYPE.equals(methods[i].getReturnType())) {

                if (getters == null) {
                    getters = new ArrayList<Method>(5);
                }

                getters.add(methods[i]);
            }
        }

        return getters != null ? getters.toArray(new Method[getters.size()]) : null;
    }

    static synchronized ClassTraversalDescriptor getDescriptor(Class<?> nodeType) {
        String typeName = nodeType.getName();
        ClassTraversalDescriptor descriptor = descriptors.get(typeName);
        if (descriptor == null) {
            Method[] getters = traversableGetters(nodeType);
            descriptor = getters != null
                    ? new ClassTraversalDescriptor(getters)
                    : noopDescriptor;
            descriptors.put(typeName, descriptor);
        }

        return descriptor;
    }

    /**
     * Performs tree traversal with a given visitor starting with a given node.
     */
    public static void traverse(Object treeRoot, HierarchicalTreeVisitor visitor) {
        traverse(treeRoot, visitor, null);
    }

    static void traverse(
            Object node,
            HierarchicalTreeVisitor visitor,
            ProjectPath parentPath) {

        ProjectPath path = parentPath != null
                ? parentPath.appendToPath(node)
                : new ProjectPath(node);

        if (visitor.onStartNode(path)) {

            ClassTraversalDescriptor descriptor = getDescriptor(node.getClass());
            Class<?>[] childTypes = descriptor.getTraversableChildTypes();
            if (childTypes != null && childTypes.length > 0) {
                for (int i = 0; i < childTypes.length; i++) {

                    HierarchicalTreeVisitor childVisitor = visitor.childVisitor(
                            path,
                            childTypes[i]);
                    if (childVisitor != null) {
                        Object child = descriptor.getTraversableChild(node, i);

                        if (child == null) {
                            continue;
                        }
                        else if (child instanceof Collection) {
                            Collection<?> children = (Collection<?>) child;

                            if (children != null && !children.isEmpty()) {
                                for (Object collectionChild : children) {
                                    traverse(collectionChild, childVisitor, path);
                                }
                            }
                        }
                        else {
                            traverse(child, childVisitor, path);
                        }
                    }
                }
            }

            visitor.onFinishNode(path);
        }
    }

    static class ClassTraversalDescriptor {

        Class<?>[] traversableChildTypes;
        Method[] traversableGetters;

        ClassTraversalDescriptor() {

        }

        ClassTraversalDescriptor(Method[] traversableChildGetters) {
            this.traversableGetters = traversableChildGetters;
            this.traversableChildTypes = new Class[traversableChildGetters.length];
            for (int i = 0; i < traversableChildGetters.length; i++) {
                Class<?> type = traversableChildGetters[i].getReturnType();
                if (Collection.class.isAssignableFrom(type)) {
                    type = traversableChildGetters[i]
                            .getAnnotation(TreeNodeChild.class)
                            .type();

                    // TODO: andrus, 4/27/2006 - determine type from collection generics
                    // metadata.
                    if (void.class.equals(type)) {
                        throw new JpaProviderException("No type for collection defined: "
                                + traversableChildGetters[i].getName());
                    }
                }

                traversableChildTypes[i] = type;
            }
        }

        Class<?>[] getTraversableChildTypes() {
            return traversableChildTypes;
        }

        Object getTraversableChild(Object object, int childIndex) {

            try {
                return traversableGetters[childIndex].invoke(object, (Object[]) null);
            }
            catch (Exception e) {
                throw new JpaProviderException("Error reading traversible property", e);
            }
        }
    }
}
