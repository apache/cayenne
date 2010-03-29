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
package org.apache.cayenne.jpa.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;

import javax.persistence.spi.ClassTransformer;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A ClassFileTransformer decorator that wraps a Java instrumentation ClassFileTransformer
 * instance in a JPA ClassTransformer. UnitClassTranformer would only do transformation of
 * the mapped classes.
 * 
 */
public class UnitClassTransformer implements ClassTransformer {

    protected ClassLoader tempClassLoader;
    protected Log logger;
    protected ClassFileTransformer transformer;
    protected Map<String, JpaClassDescriptor> managedClasses;

    public UnitClassTransformer(Map<String, JpaClassDescriptor> managedClasses,
            ClassLoader tempClassLoader, ClassFileTransformer transformer) {
        this.transformer = transformer;
        this.managedClasses = managedClasses;
        this.tempClassLoader = tempClassLoader;
        this.logger = LogFactory.getLog(getClass());
    }

    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {

        if (tempClassLoader == loader) {
            return null;
        }

        if (isManagedClass(className)) {

            logger.info("Will transform managed class: " + className);

            try {
                return transformer.transform(
                        loader,
                        className,
                        classBeingRedefined,
                        protectionDomain,
                        classfileBuffer);
            }
            catch (IllegalClassFormatException e) {
                logger.warn("Error transforming class " + className, e);
                throw e;
            }
            catch (Throwable th) {
                logger.warn("Error transforming class " + className, th);
                throw new JpaProviderException("Error transforming class " + className, th);
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns true if a classname os a part of an entity map. Note that the class name is
     * expected in the internal format, separated by "/", not ".".
     */
    protected boolean isManagedClass(String className) {
        return managedClasses.containsKey(className.replace('/', '.'));
    }
}
