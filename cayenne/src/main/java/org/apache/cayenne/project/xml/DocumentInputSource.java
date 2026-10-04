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

import org.apache.cayenne.ConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Serializes a DOM so that it can be parsed by the SAX-based XML loaders, which switch content handlers on a real
 * XMLReader and so can not consume a DOM directly. Used to load documents upgraded in memory.
 */
final class DocumentInputSource {

    private DocumentInputSource() {
    }

    static InputSource of(Document document, String systemId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(out));
        } catch (TransformerException e) {
            throw new ConfigurationException("Error serializing upgraded configuration %s", e, systemId);
        }

        InputSource source = new InputSource(new ByteArrayInputStream(out.toByteArray()));
        source.setSystemId(systemId);
        return source;
    }
}
