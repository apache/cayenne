/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.template.parser;

import java.lang.reflect.Method;

import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.template.Context;

/**
 * @since 4.1
 */
public class ASTMethod extends IdentifierNode {

    public ASTMethod(int id) {
        super(id);
    }

    /**
     * Evaluate method call to an Object
     */
    public Object evaluateAsObject(Context context, Object parentObject) {
        if(parentObject == null) {
            throw new IllegalStateException("To evaluate method node parent object should be set.");
        }

        try {
            // first try default property resolution
            return PropertyUtils.getProperty(parentObject, getIdentifier());
        } catch (IllegalArgumentException ex) {
            // if it fails, try direct method call
            methodsLoop:
            for(Method m : parentObject.getClass().getMethods()) {
                if(m.getName().equals(getIdentifier())) {
                    // check count of arguments
                    if(m.getParameterTypes().length != jjtGetNumChildren()) {
                        continue;
                    }
                    int i = 0;
                    Object[] arguments = new Object[jjtGetNumChildren()];
                    for(Class<?> parameterType : m.getParameterTypes()) {
                        ASTExpression child = (ASTExpression)jjtGetChild(i);
                        try {
                            if (parameterType.isAssignableFrom(Object.class)) {
                                arguments[i] = child.evaluateAsObject(context);
                            } else if (parameterType.isAssignableFrom(String.class)) {
                                arguments[i] = child.evaluateAsString(context);
                            } else if (parameterType.isAssignableFrom(Boolean.class) || parameterType.isAssignableFrom(boolean.class)) {
                                arguments[i] = child.evaluateAsBoolean(context);
                            } else if (parameterType.isAssignableFrom(Double.class) || parameterType.isAssignableFrom(double.class)) {
                                arguments[i] = child.evaluateAsDouble(context);
                            } else if (parameterType.isAssignableFrom(Float.class) || parameterType.isAssignableFrom(float.class)) {
                                arguments[i] = (float) child.evaluateAsDouble(context);
                            } else if (parameterType.isAssignableFrom(Long.class) || parameterType.isAssignableFrom(long.class)) {
                                arguments[i] = child.evaluateAsLong(context);
                            } else if (parameterType.isAssignableFrom(Integer.class) || parameterType.isAssignableFrom(int.class)) {
                                arguments[i] = (int) child.evaluateAsLong(context);
                            } else if (parameterType.isAssignableFrom(Object[].class)) {
                                arguments[i] = child.evaluateAsObject(context);
                            } else {
                                continue methodsLoop;
                            }
                        } catch (UnsupportedOperationException ignored) {
                            continue methodsLoop;
                        }
                        i++;
                    }

                    try {
                        m.setAccessible(true);
                        return m.invoke(parentObject, arguments);
                    } catch (Exception ignored) {
                        // continue
                        System.out.println("test");
                    }
                }
            }
        }

        throw new IllegalArgumentException("Unable to resolve method " + getIdentifier() +
                " with " + jjtGetNumChildren() + " args for object " + parentObject);
    }

    @Override
    public void evaluate(Context context) {
        throw new UnsupportedOperationException("Unable evaluate method directly, must be solved via ASTVariable");
    }

}
