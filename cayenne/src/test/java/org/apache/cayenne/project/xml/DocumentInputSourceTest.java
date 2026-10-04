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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentInputSourceTest {

    @Test
    public void of() throws Exception {
        URL url = getClass().getResource("cayenne-testConfig1.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.toString());
        document.getDocumentElement().setAttribute("marker", "changed");

        InputSource source = DocumentInputSource.of(document, "test-id");
        assertEquals("test-id", source.getSystemId());

        Document reparsed = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source.getByteStream());
        assertEquals("changed", reparsed.getDocumentElement().getAttribute("marker"),
                "the serialized document must reflect the in-memory changes");
    }
}
