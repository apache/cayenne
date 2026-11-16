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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.dbimport.PatternParam;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * XML helpers for the project loading and saving tests.
 */
final class XMLTestUtils {

    private XMLTestUtils() {
    }

    static String encode(XMLSerializable serializable) {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            serializable.encodeAsXML(new XMLEncoder(writer, "\t"));
        }
        return out.toString();
    }

    static Document parse(String xml) throws Exception {
        return documentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    static Document parse(URL url) throws Exception {
        return documentBuilder().parse(url.toString());
    }

    static List<Element> childElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element) {
                elements.add(element);
            }
        }
        return elements;
    }

    static List<String> patterns(Collection<? extends PatternParam> params) {
        return params.stream().map(PatternParam::getPattern).toList();
    }

    static URL schema(String fileName) {
        return XMLTestUtils.class.getResource("/org/apache/cayenne/schema/15/" + fileName);
    }

    static void assertMatchesSchema(DataMap map) throws Exception {
        assertMatchesSchema(encode(map), "dataMap.xsd");
    }

    static void assertMatchesSchema(Project project) throws Exception {
        assertMatchesSchema(encode(project), "project.xsd");
    }

    private static void assertMatchesSchema(String xml, String schemaFileName) throws Exception {
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schema(schemaFileName)).newValidator()
                .validate(new StreamSource(new StringReader(xml)));
    }

    private static DocumentBuilder documentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }
}
