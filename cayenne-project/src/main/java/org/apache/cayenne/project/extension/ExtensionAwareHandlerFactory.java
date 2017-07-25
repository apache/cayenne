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

package org.apache.cayenne.project.extension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.di.Inject;

/**
 * Project parser handlers factory that will use third-party extensions
 * to provide handlers for unknown tags.
 *
 * @see ProjectExtension
 * @see DefaultHandlerFactory
 *
 * @since 4.1
 */
public class ExtensionAwareHandlerFactory extends DefaultHandlerFactory {

    Map<String, LoaderDelegate> loaderDelegates = new ConcurrentHashMap<>();

    public ExtensionAwareHandlerFactory(@Inject List<ProjectExtension> extensions) {
        for(ProjectExtension extension : extensions) {
            LoaderDelegate delegate = extension.createLoaderDelegate();
            LoaderDelegate old = loaderDelegates.put(delegate.getTargetNamespace(), delegate);
            if(old != null) {
                throw new CayenneRuntimeException("Found two loader delegates for namespace %s",
                        delegate.getTargetNamespace());
            }
        }
    }

    @Override
    public NamespaceAwareNestedTagHandler createHandler(String namespace, String localName,
                                                        NamespaceAwareNestedTagHandler parent) {

        LoaderDelegate delegate = loaderDelegates.get(namespace);
        if(delegate != null) {
            NamespaceAwareNestedTagHandler handler = delegate.createHandler(parent, localName);
            if(handler != null) {
                return handler;
            }
        }

        return super.createHandler(namespace, localName, parent);
    }
}
