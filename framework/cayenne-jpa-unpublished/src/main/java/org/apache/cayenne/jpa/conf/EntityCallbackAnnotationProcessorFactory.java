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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import org.apache.cayenne.jpa.map.JpaAbstractEntity;
import org.apache.cayenne.jpa.map.JpaLifecycleCallback;

class EntityCallbackAnnotationProcessorFactory extends AnnotationProcessorFactory {

    static abstract class AbstractCallbackAnnotationProcessor implements
            AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Method m = (Method) element;
            updateEntity((JpaAbstractEntity) context.peek(), new JpaLifecycleCallback(m
                    .getName()));
        }

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            // noop...
        }

        abstract void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback);
    }

    static final class PrePersistProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPrePersist(callback);
        }
    }

    static final class PostPersistProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPostPersist(callback);
        }
    }

    static final class PreRemoveProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPreRemove(callback);
        }
    }

    static final class PostRemoveProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPostRemove(callback);
        }
    }

    static final class PreUpdateProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPreUpdate(callback);
        }
    }

    static final class PostUpdateProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPostUpdate(callback);
        }
    }

    static final class PostLoadProcessor extends AbstractCallbackAnnotationProcessor {

        @Override
        void updateEntity(JpaAbstractEntity entity, JpaLifecycleCallback callback) {
            entity.setPostLoad(callback);
        }
    }
}
