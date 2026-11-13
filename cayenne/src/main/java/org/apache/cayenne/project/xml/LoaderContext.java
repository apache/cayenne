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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.project.ProjectLoadingMode;
import org.xml.sax.XMLReader;

/**
 * @since 4.1
 */
public class LoaderContext {

    private final XMLReader xmlReader;

    private final ProjectLoadingMode loadingMode;

    /**
     * @since 6.0
     */
    public LoaderContext(XMLReader reader, ProjectLoadingMode loadingMode) {
        this.xmlReader = reader;
        this.loadingMode = loadingMode;
    }

    /**
     * Returns true if the tool-only parts of the project model should be loaded.
     *
     * @since 6.0
     */
    public boolean isFullModel() {
        return loadingMode == ProjectLoadingMode.FULL;
    }

    public XMLReader getXmlReader() {
        return xmlReader;
    }
}
