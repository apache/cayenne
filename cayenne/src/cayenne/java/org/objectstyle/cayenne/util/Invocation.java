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

package org.objectstyle.cayenne.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * Invocation represents a dynamic method invocation bound to a specific target.
 * The target is kept with a WeakReference and can therefore be reclaimed by the
 * Garbage Collector.
 * 
 * @author Holger Hoffstaette
 * @author Dirk Olmes
 */
public class Invocation extends Object {
    private static final Logger log = Logger.getLogger(Invocation.class);

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
     * Constructor for an Invocation with a single argument in the target's
     * method.
     * 
     * @see #Invocation(Object, String, Class[])
     */
    public Invocation(Object target, String methodName, Class parameterType)
        throws NoSuchMethodException {
        this(target, methodName, new Class[] { parameterType });
    }

    /**
     * Constructor for an Invocation with arbitrary arguments in the target's
     * method.
     * 
     * @param target
     * @param methodName
     * @param parameterTypes
     * @throws NoSuchMethodException if <code>methodName</code> could not be
     * found in the target
     * @throws IllegalArgumentException if target or methodName are
     * <code>null</code>, or parameterTypes is empty or contains
     * <code>null</code> elements
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
                        throw new IllegalArgumentException(
                            "parameter type[" + i + "] must not be null");
                    }
                }
            }
            else {
                throw new IllegalArgumentException("parameter types must not be empty");
            }
        }

        Method method = null;

        // allow access to public methods of inaccessible classes, if such methods were
        // declared in a public interface
        Class targetClass = target.getClass();
        Class[] interfaces = targetClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            try {
                method = interfaces[i].getMethod(methodName, parameterTypes);
                break;
            }
            catch (NoSuchMethodException ex) {
                // ignoring, keep looking
            }
        }

        _method =
            (method != null) ? method : targetClass.getMethod(methodName, parameterTypes);
        _parameterTypes = parameterTypes;
        _target = new WeakReference(target);
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
        return this.fire(new Object[] { argument });
    }

    /**
     * Invoke the target's method with an arbitrary number of arguments.
     * The number of arguments must be consistent with the arguments given at
     * construction time of this Invocation.
     * 
     * @param arguments an array of objects passed to the target's method
     * 
     * @return <code>true</code> if invocation of the method succeeded,
     * otherwise <code>false</code>.
     * 
     * @throws IllegalArgumentException if the passed arguments are inconsistent
     * with the arguments passed to this instance's constructor
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
            log.info("null target for: " + _method.getName());
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
            log.info("exception firing '" + _method.getName() + "'", ex);
            return false;
        }
    }

    /**
     * @see Object#equals(java.lang.Object)
     */
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
