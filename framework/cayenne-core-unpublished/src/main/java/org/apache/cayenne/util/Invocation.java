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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Invocation represents a dynamic method invocation bound to a specific target. The
 * target is kept with a WeakReference and can therefore be reclaimed by the Garbage
 * Collector.
 * 
 */
public class Invocation {

    private WeakReference _target;
    private Method _method;
    private Class[] _parameterTypes;

    /**
     * Prevent use of empty default constructor
     */
    private Invocation() {
    }

    /**
     * Constructor for an Invocation without arguments in the target's method.
     * 
     * @see #Invocation(Object, String, Class[])
     */
    public Invocation(Object target, String methodName) throws NoSuchMethodException {
        this(target, methodName, (Class[]) null);
    }

    /**
     * Constructor for an Invocation with a single argument in the target's method.
     * 
     * @see #Invocation(Object, String, Class[])
     */
    public Invocation(Object target, String methodName, Class parameterType)
            throws NoSuchMethodException {
        this(target, methodName, new Class[] {
            parameterType
        });
    }

    /**
     * Constructor for an Invocation with arbitrary arguments in the target's method.
     * 
     * @param target
     * @param methodName
     * @param parameterTypes
     * @throws NoSuchMethodException if <code>methodName</code> could not be found in
     *             the target
     * @throws IllegalArgumentException if target or methodName are <code>null</code>,
     *             or parameterTypes is empty or contains <code>null</code> elements
     */
    public Invocation(Object target, String methodName, Class[] parameterTypes)
            throws NoSuchMethodException {
        super();

        if (target == null) {
            throw new IllegalArgumentException("target argument must not be null");
        }

        if (methodName == null) {
            throw new IllegalArgumentException("method name must not be null");
        }

        if (parameterTypes != null) {
            if (parameterTypes.length > 0) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i] == null) {
                        throw new IllegalArgumentException("parameter type["
                                + i
                                + "] must not be null");
                    }
                }
            }
            else {
                throw new IllegalArgumentException("parameter types must not be empty");
            }
        }

        // allow access to public methods of inaccessible classes, if such methods were
        // declared in a public interface

        _method = lookupMethodInHierarchy(target.getClass(), methodName, parameterTypes);

        if (_method == null) {
            throw new NoSuchMethodException("No such method: "
                    + target.getClass().getName()
                    + "."
                    + methodName);
        }

        if (!Util.isAccessible(_method)) {
            _method.setAccessible(true);
        }

        _parameterTypes = parameterTypes;
        _target = new WeakReference(target);
    }

    Method lookupMethodInHierarchy(
            Class objectClass,
            String methodName,
            Class[] parameterTypes) throws SecurityException, NoSuchMethodException {

        try {
            return objectClass.getDeclaredMethod(methodName, parameterTypes);
        }
        catch (NoSuchMethodException e) {

            Class superClass = objectClass.getSuperclass();
            if (superClass == null || superClass.getName().equals(Object.class.getName())) {
                throw e;
            }

            return lookupMethodInHierarchy(superClass, methodName, parameterTypes);
        }
    }

    /**
     * Invoke the target's method without any arguments.
     * 
     * @see #fire(Object[])
     */
    public boolean fire() {
        return this.fire(null);
    }

    /**
     * Invoke the target's method with a single argument.
     * 
     * @param argument an object passed to the target's method
     * @see #fire(Object[])
     */
    public boolean fire(Object argument) {
        return this.fire(new Object[] {
            argument
        });
    }

    /**
     * Invoke the target's method with an arbitrary number of arguments. The number of
     * arguments must be consistent with the arguments given at construction time of this
     * Invocation.
     * 
     * @param arguments an array of objects passed to the target's method
     * @return <code>true</code> if invocation of the method succeeded, otherwise
     *         <code>false</code>.
     * @throws IllegalArgumentException if the passed arguments are inconsistent with the
     *             arguments passed to this instance's constructor
     * @see #fire(Object[])
     */
    public boolean fire(Object[] arguments) {

        if (_parameterTypes == null) {
            if (arguments != null) {
                throw new IllegalArgumentException("arguments unexpectedly != null");
            }
        }
        else if (arguments == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }
        else if (_parameterTypes.length != arguments.length) {
            throw new IllegalArgumentException(
                    "inconsistent number of arguments: expected"
                            + _parameterTypes.length
                            + ", got "
                            + arguments.length);
        }

        Object currentTarget = _target.get();
        if (currentTarget == null) {
            return false;
        }

        try {
            _method.invoke(currentTarget, arguments);
            return true;
        }
        catch (InvocationTargetException ite) {
            // this is the only type of exception that can be rethrown, since
            // listener can have a valid need to respond to an event with exception,
            // and this does not indicate that it is being in invalid state

            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            else {
                throw new CayenneRuntimeException(cause);
            }
        }
        catch (Exception ex) {
            // all other exceptions indicate propblems with the listener,
            // so return invalid status
            return false;
        }
    }

    /**
     * @see Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj != null) && (obj.getClass().equals(this.getClass()))) {
            Invocation otherInvocation = (Invocation) obj;
            if (_method.equals(otherInvocation.getMethod())) {
                Object otherTarget = otherInvocation.getTarget();
                Object target = _target.get();

                if ((target == null) && (otherTarget == null)) {
                    return true;
                }

                if ((target == null) && (otherTarget != null)) {
                    return false;
                }

                if (target != null) {
                    return target.equals(otherTarget);
                }
            }

            return false;
        }
        else {
            return super.equals(obj);
        }
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        // IMPORTANT: DO NOT include Invocation target into whatever
        // algorithm is used to compute hashCode, since it is using a
        // WeakReference and can be released at a later time, altering
        // hashCode, and breaking collections using Invocation as a key
        // (e.g. event DispatchQueue)

        // TODO: use Jakarta commons HashBuilder
        int hash = 42, hashMultiplier = 59;
        return hash * hashMultiplier + _method.hashCode();
    }

    /**
     * @return the method to be invoked on the target
     */
    public Method getMethod() {
        return _method;
    }

    /**
     * @return the target object of this Invocation
     */
    public Object getTarget() {
        return _target.get();
    }

    /**
     * @return an array of Classes describing the target method's parameters
     */
    public Class[] getParameterTypes() {
        return _parameterTypes;
    }

}
