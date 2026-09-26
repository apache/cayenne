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

package org.apache.cayenne.configuration.xml;

import java.util.Objects;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.upgrade.UpgradeHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Handler that can validate root tag name, version and namespace.
 *
 * @since 4.1
 */
public abstract class VersionAwareHandler extends NamespaceAwareNestedTagHandler {

    protected String rootTag;

    public VersionAwareHandler(LoaderContext loaderContext, String rootTag) {
        super(loaderContext);
        setAllowAllNamespaces(true);
        this.rootTag = Objects.requireNonNull(rootTag);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        if(rootTag.equals(localName)) {
            validateVersion(attributes);
            validateNamespace(namespaceURI);
        } else {
            throw new CayenneRuntimeException("Illegal XML root tag: %s, expected: %s", localName, rootTag);
        }
        return false;
    }

    /**
     * Checks that the document has the current project version, throwing {@link UnsupportedVersionException}
     * otherwise, so that the loader can upgrade the document in memory.
     */
    protected void validateVersion(Attributes attributes) {
        String version = attributes.getValue("project-version");
        if (!UpgradeHandler.CURRENT_VERSION.equals(version)) {
            throw new UnsupportedVersionException(version != null ? version : UpgradeHandler.UNKNOWN_VERSION);
        }
    }

    protected void validateNamespace(String realNamespace) {
        if(!targetNamespace.equals(realNamespace)) {
            throw new CayenneRuntimeException("Unknown XML namespace %s, expected %s. Probably xml was modified.",
                    realNamespace, targetNamespace);
        }
    }
}
