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


package org.apache.cayenne.jpa.conf;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for annotation processors. Concrete subclasses can simply define inner
 * classes for the each type of annotation processors they support.
 * {@link #createProcessor(String)} method will use naming conventions to determine the
 * type of the processor.
 * 
 */
abstract class AnnotationProcessorFactory {

    static final String ANNOTATIONS_PACKAGE = "javax.persistence.";
    static final String PROCESSOR_NAME_SUFFIX = "Processor";
    static final AnnotationProcessor NOOP_PROCESSOR = new AnnotationProcessor() {

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
        }

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
        }
    };

    /**
     * Dervies processor inner class name, applying naming conventions.
     */
    static Class<?> processorClass(Class<?> factoryClass, String annotationFQN) {
        if (annotationFQN.startsWith(ANNOTATIONS_PACKAGE)) {

            // derive the processor name from the annotation unqualified name, so that we
            // do not have to configure the processors manually

            // assume that the processor class is an inner class of a concrete factory...

            String processorName = factoryClass.getName()
                    + "$"
                    + annotationFQN.substring(ANNOTATIONS_PACKAGE.length())
                    + PROCESSOR_NAME_SUFFIX;

            try {
                return Class.forName(processorName, true, Thread
                        .currentThread()
                        .getContextClassLoader());

            }
            catch (Exception e) {
                // there are a few unsupported annotations in the JPA package related
                // to Java EE conatiners
                return null;
            }
        }
        else {
            return null;
        }

    }

    /**
     * Returns an annotation class handled by the processor, applying naming conventions.
     */
    static Class<?> annotationClass(Class<?> processorClass) {
        String name = processorClass.getName();
        if (!name.endsWith(PROCESSOR_NAME_SUFFIX)) {
            return null;
        }

        int split = name.lastIndexOf('$');
        if (split <= 0) {
            return null;
        }

        String className = name.substring(split + 1);
        String annotationFQN = ANNOTATIONS_PACKAGE
                + className.substring(0, className.length()
                        - PROCESSOR_NAME_SUFFIX.length());

        try {
            return Class.forName(annotationFQN, true, Thread
                    .currentThread()
                    .getContextClassLoader());

        }
        catch (Exception e) {
            // there are a few unsupported annotations in the JPA package related
            // to Java EE containers
            return null;
        }
    }

    final Map<String, AnnotationProcessor> processors;

    AnnotationProcessorFactory() {
        this.processors = new HashMap<String, AnnotationProcessor>();
    }

    /**
     * Returns processor for a given annotation, caching it ofr future use. Returns null
     * if an annotation is not a JPA annotation.
     */
    AnnotationProcessor getProcessor(Annotation annotation) {

        String annotationName = annotation.annotationType().getName();
        AnnotationProcessor processor = processors.get(annotationName);

        if (processor == null) {
            processor = createProcessor(annotationName);
            processors.put(annotationName, processor);
        }

        return processor == NOOP_PROCESSOR ? null : processor;
    }

    /**
     * Creates a new processor for the annotation full class name.
     */
    AnnotationProcessor createProcessor(String annotationFQN) {

        Class<?> processorClass = processorClass(getClass(), annotationFQN);

        if (processorClass != null) {

            try {
                return (AnnotationProcessor) processorClass.newInstance();
            }
            catch (Exception e) {
                return NOOP_PROCESSOR;
            }
        }

        // not a JPA annotation...
        return NOOP_PROCESSOR;
    }
}
