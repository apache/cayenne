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
package org.apache.cayenne.modeler.validation.extension;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.extension.LoaderDelegate;

/**
 * @since 5.0
 */
public class ValidationLoaderDelegate implements LoaderDelegate {

    private final DataChannelMetaData metaData;

    ValidationLoaderDelegate(@Inject DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public String getTargetNamespace() {
        return ValidationExtension.NAMESPACE;
    }

    @Override
    public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent, String tag) {
        if (ValidationConfigHandler.CONFIG_TAG.equals(tag)) {
            return new ValidationConfigHandler(parent, metaData);
        }
        return null;
    }
}
