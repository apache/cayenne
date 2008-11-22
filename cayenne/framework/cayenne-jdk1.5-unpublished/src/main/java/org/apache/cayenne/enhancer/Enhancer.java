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
package org.apache.cayenne.enhancer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * A ClassFileTransformer that delegates class enhancement to a chain of ASM transformers
 * provided by the {@link EnhancerVisitorFactory}.
 * 
 * @since 3.0
 */
public class Enhancer implements ClassFileTransformer {

    protected Log logger;
    protected EnhancerVisitorFactory visitorFactory;

    public Enhancer(EnhancerVisitorFactory visitorFactory) {
        logger = LogFactory.getLog(getClass());
        this.visitorFactory = visitorFactory;
    }

    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {

        ClassReader reader = new ClassReader(classfileBuffer);

        // optimization note: per ASM docs COMPUTE_FRAMES makes code generation 2x slower,
        // so we may investigate manual computation options, although that's likely a
        // pain.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        ClassVisitor visitor = visitorFactory.createVisitor(className, writer);
        if (visitor == null) {
            // per instrumentation docs, if no transformation occured, we must return null
            return null;
        }

        logger.info("enhancing class " + className);

        try {
            reader.accept(visitor, 0);
        }
        catch (DoubleEnhanceException e) {
            logger.info("class already enhanced, skipping: " + className);
            return null;
        }
        
        return writer.toByteArray();
    }
}
