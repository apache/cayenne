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

package org.apache.cayenne.project.upgrade.handlers;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.apache.cayenne.resource.URLResource;
import org.junit.Before;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * @since 4.1
 */
abstract class BaseUpgradeHandlerTest {

    UpgradeHandler handler;

    @Before
    public void createHandler() {
        handler = newHandler();
    }

    abstract UpgradeHandler newHandler();

    Document processProjectDom(String xmlResourceName) throws Exception {
        UpgradeUnit unit = new UpgradeUnit(new URLResource(getClass().getResource(xmlResourceName)),
                documentFromResource(xmlResourceName));
        handler.processProjectDom(unit);
        return unit.getDocument();
    }

    Document processDataMapDom(String xmlResourceName) throws Exception {
        UpgradeUnit unit = new UpgradeUnit(new URLResource(getClass().getResource(xmlResourceName)),
                documentFromResource(xmlResourceName));
        handler.processDataMapDom(unit);
        return unit.getDocument();
    }

    List<Document> processAllDataMapDomes(List<String> xmlResourceNames) throws Exception {
        List<UpgradeUnit> upgradeUnits = new ArrayList<>(xmlResourceNames.size());
        List<Document> documents = new ArrayList<>();
        for (String xmlResourceName : xmlResourceNames) {
            UpgradeUnit unit = new UpgradeUnit(new URLResource(getClass().getResource(xmlResourceName)),
                    documentFromResource(xmlResourceName));
            upgradeUnits.add(unit);
            documents.add(unit.getDocument());
        }
        handler.processAllDataMapDomes(upgradeUnits);
        return documents;
    }

    Document documentFromString(String xml) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }

    Document documentFromResource(String resource) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.parse(new InputSource(new InputStreamReader(getClass().getResourceAsStream(resource))));
    }

}
