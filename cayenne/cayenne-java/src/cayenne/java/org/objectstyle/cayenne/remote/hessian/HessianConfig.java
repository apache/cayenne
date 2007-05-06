/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.remote.hessian;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.util.Util;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.SerializerFactory;

/**
 * A utility class that configures Hessian serialization properties using reflection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class HessianConfig {

    /**
     * Creates a Hessian SerializerFactory configured with zero or more
     * AbstractSerializerFactory extensions. Extensions are specified as class names. This
     * method can inject EntityResolver if an extension factory class defines
     * <em>setEntityResolver(EntityResolver)</em> method.
     * 
     * @param factoryNames an array of factory class names. Each class must be a concrete
     *            subclass of <em>com.caucho.hessian.io.AbstractSerializerFactory</em>
     *            and have a default constructor.
     * @param resolver if not null, EntityResolver will be injected into all factories
     *            that implement <em>setEntityResolver(EntityResolver)</em> method.
     */
    public static SerializerFactory createFactory(
            String[] factoryNames,
            EntityResolver resolver) {

        SerializerFactory factory = new SerializerFactory();

        if (factoryNames != null && factoryNames.length > 0) {

            for (int i = 0; i < factoryNames.length; i++) {

                try {
                    factory.addFactory(loadFactory(factoryNames[i], resolver));
                }
                catch (Exception e) {
                    throw new CayenneRuntimeException("Error configuring factory class "
                            + factoryNames[i], e);
                }
            }
        }

        return factory;
    }

    static AbstractSerializerFactory loadFactory(
            String factoryName,
            EntityResolver resolver) throws Exception {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class factoryClass = Class.forName(factoryName, true, loader);

        if (!AbstractSerializerFactory.class.isAssignableFrom(factoryClass)) {
            throw new IllegalArgumentException(factoryClass
                    + " is not a AbstractSerializerFactory");
        }

        Constructor c = factoryClass.getDeclaredConstructor(new Class[] {});
        if (!Util.isAccessible(c)) {
            c.setAccessible(true);
        }

        AbstractSerializerFactory object = (AbstractSerializerFactory) c
                .newInstance(null);

        if (resolver != null) {
            try {

                Method setter = factoryClass.getDeclaredMethod(
                        "setEntityResolver",
                        new Class[] {
                            EntityResolver.class
                        });

                if (!Util.isAccessible(setter)) {
                    setter.setAccessible(true);
                }

                setter.invoke(object, new Object[] {
                    resolver
                });
            }
            catch (Exception e) {
                // ignore injection exception
            }
        }

        return object;
    }

}
