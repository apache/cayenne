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

package org.apache.cayenne.modeler.graph.extension;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.project.extension.LoaderDelegate;

/**
 * @since 4.1
 */
class GraphLoaderDelegate implements LoaderDelegate {

    Application application;

    GraphLoaderDelegate(Application application) {
        this.application = application;
    }

    @Override
    public String getTargetNamespace() {
        return GraphExtension.NAMESPACE;
    }

    @Override
    public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent, String tag) {
        if(GraphsRootHandler.GRAPHS_TAG.equals(tag)) {
            return new GraphsRootHandler(parent, application);
        }
        return null;
    }
}
