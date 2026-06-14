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
package org.apache.cayenne.configuration.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper to setup a shared test JNDI environment.
 */
class JNDISetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(JNDISetup.class);

    private static final String ENV_SUBCONTEXT = "java:comp/env";

    private static final Map<String, Object> BINDINGS = new ConcurrentHashMap<>();

    private static volatile boolean setup;

    public static void doSetup() {
        if (setup) {
            return;
        }
        synchronized (JNDISetup.class) {
            if (setup) {
                return;
            }
            try {
                NamingManager.setInitialContextFactoryBuilder(new InMemoryContextFactoryBuilder());
            } catch (NamingException e) {
                LOGGER.error("Can't perform JNDI setup, ignoring...", e);
            }
            setup = true;
        }
    }

    private static class InMemoryContextFactoryBuilder implements InitialContextFactoryBuilder {
        @Override
        public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) {
            return env -> new InMemoryContext("");
        }
    }

    private static class InMemoryContext implements Context {

        private final String prefix;

        InMemoryContext(String prefix) {
            this.prefix = prefix;
        }

        private String fullName(String name) {
            return prefix.isEmpty() ? name : prefix + "/" + name;
        }

        @Override
        public Object lookup(String name) throws NamingException {
            String key = fullName(name);
            if (ENV_SUBCONTEXT.equals(key)) {
                return new InMemoryContext(key);
            }
            Object value = BINDINGS.get(key);
            if (value == null) {
                throw new NameNotFoundException(key);
            }
            return value;
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            return lookup(name.toString());
        }

        @Override
        public void bind(String name, Object obj) {
            BINDINGS.put(fullName(name), obj);
        }

        @Override
        public void bind(Name name, Object obj) {
            bind(name.toString(), obj);
        }

        @Override
        public void rebind(String name, Object obj) {
            BINDINGS.put(fullName(name), obj);
        }

        @Override
        public void rebind(Name name, Object obj) {
            rebind(name.toString(), obj);
        }

        @Override
        public void unbind(String name) {
            BINDINGS.remove(fullName(name));
        }

        @Override
        public void unbind(Name name) {
            unbind(name.toString());
        }

        @Override
        public void close() {
        }

        @Override
        public void rename(Name oldName, Name newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void rename(String oldName, String newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void destroySubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Context createSubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            return lookup(name);
        }

        @Override
        public Object lookupLink(String name) throws NamingException {
            return lookup(name);
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public NameParser getNameParser(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Name composeName(Name name, Name prefix) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public String composeName(String name, String prefix) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Object addToEnvironment(String propName, Object propVal) {
            return null;
        }

        @Override
        public Object removeFromEnvironment(String propName) {
            return null;
        }

        @Override
        public Hashtable<?, ?> getEnvironment() {
            return new Hashtable<>();
        }

        @Override
        public String getNameInNamespace() {
            return prefix;
        }
    }
}
