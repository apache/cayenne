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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.util.Util;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @since 4.1
 */
public class XMLReaderProvider implements Provider<XMLReader> {

    private final boolean supportInclude;

    public XMLReaderProvider(boolean supportInclude) {
        this.supportInclude = supportInclude;
    }

    @Override
    public XMLReader get() throws DIRuntimeException {
        try {
            XMLReader reader = Util.createXmlReader();
            if(supportInclude) {
                reader.setFeature("http://apache.org/xml/features/xinclude", true);
            }
            return reader;
        } catch (SAXException | ParserConfigurationException ex) {
            throw new DIRuntimeException("Unable to create XMLReader", ex);
        }
    }
}
