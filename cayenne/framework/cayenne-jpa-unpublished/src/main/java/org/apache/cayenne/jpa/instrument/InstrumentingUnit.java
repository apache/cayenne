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
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javax.persistence.spi.ClassTransformer;

import org.apache.cayenne.instrument.InstrumentUtil;
import org.apache.cayenne.jpa.JpaUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A unit that loads all transformers into a shared Instrumentation instance.
 * 
 */
public class InstrumentingUnit extends JpaUnit {

    protected Log logger;

    @Override
    public void addTransformer(final ClassTransformer transformer) {

        // wrap in a ClassFileTransformer
        ClassFileTransformer transformerWrapper = new ClassFileTransformer() {

            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {

                return transformer.transform(
                        loader,
                        className,
                        classBeingRedefined,
                        protectionDomain,
                        classfileBuffer);
            }
        };

        getLogger().info("*** Adding transformer: " + transformer);

        Instrumentation i = InstrumentUtil.getInstrumentation();
        if (i == null) {
            throw new IllegalStateException("Attempt to add a transformer failed - "
                    + "instrumentation is not initialized.");
        }

        i.addTransformer(transformerWrapper);
    }

    protected Log getLogger() {
        if (logger == null) {
            logger = LogFactory.getLog(getClass());
        }

        return logger;
    }
}
