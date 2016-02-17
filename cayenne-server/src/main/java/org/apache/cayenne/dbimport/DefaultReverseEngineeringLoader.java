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
package org.apache.cayenne.dbimport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @since 4.0.
 */
public class DefaultReverseEngineeringLoader implements ReverseEngineeringLoader {

    private static final Log LOG = LogFactory.getLog(ReverseEngineeringLoader.class);

    @Override
    public ReverseEngineering load(InputStream inputStream) throws IOException, ReverseEngineeringLoaderException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);

            ReverseEngineering engineering = new ReverseEngineering();

            Element root = doc.getDocumentElement();
            engineering.setSkipRelationshipsLoading(loadBoolean(root, "skipRelationshipsLoading"));
            engineering.setSkipPrimaryKeyLoading(loadBoolean(root, "skipPrimaryKeyLoading"));
            engineering.setTableTypes(loadTableTypes(root));

            engineering.setCatalogs(loadCatalogs(root));
            engineering.setSchemas(loadSchemas(root));
            engineering.setIncludeTables(loadIncludeTables(root));
            engineering.setExcludeTables(loadExcludeTables(root));
            engineering.setIncludeColumns(loadIncludeColumns(root));
            engineering.setExcludeColumns(loadExcludeColumns(root));
            engineering.setIncludeProcedures(loadIncludeProcedures(root));
            engineering.setExcludeProcedures(loadExcludeProcedures(root));

            return engineering;
        } catch (ParserConfigurationException e) {
            throw new ReverseEngineeringLoaderException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new ReverseEngineeringLoaderException(e.getMessage(), e);
        }
    }

    private Boolean loadBoolean(Element root, String name) {
        return Boolean.valueOf(loadByName(root, name));
    }

    private Collection<ExcludeProcedure> loadExcludeProcedures(Node parent) {
        return loadPatternParams(ExcludeProcedure.class, getElementsByTagName(parent, "excludeProcedure"));
    }

    private Collection<IncludeProcedure> loadIncludeProcedures(Node parent) {
        return loadPatternParams(IncludeProcedure.class, getElementsByTagName(parent, "includeProcedure"));
    }

    private Collection<ExcludeColumn> loadExcludeColumns(Node parent) {
        return loadPatternParams(ExcludeColumn.class, getElementsByTagName(parent, "excludeColumn"));
    }

    private Collection<IncludeColumn> loadIncludeColumns(Node parent) {
        return loadPatternParams(IncludeColumn.class, getElementsByTagName(parent, "includeColumn"));
    }

    private Collection<ExcludeTable> loadExcludeTables(Node parent) {
        return loadPatternParams(ExcludeTable.class, getElementsByTagName(parent, "excludeTable"));
    }

    private Collection<IncludeTable> loadIncludeTables(Node parent) {
        List<Node> includeTables = getElementsByTagName(parent, "includeTable");
        Collection<IncludeTable> res = new LinkedList<IncludeTable>();
        for (Node node : includeTables) {
            IncludeTable includeTable = new IncludeTable();

            includeTable.setPattern(loadPattern(node));
            includeTable.setIncludeColumns(loadIncludeColumns(node));
            includeTable.setExcludeColumns(loadExcludeColumns(node));
            res.add(includeTable);
        }
        return res;
    }

    private Collection<Schema> loadSchemas(Node parent) {
        List<Node> schemas = getElementsByTagName(parent, "schema");
        Collection<Schema> res = new LinkedList<Schema>();
        for (Node schemaNode : schemas) {
            Schema schema = new Schema();

            schema.setName(loadName(schemaNode));
            schema.setIncludeTables(loadIncludeTables(schemaNode));
            schema.setExcludeTables(loadExcludeTables(schemaNode));
            schema.setIncludeColumns(loadIncludeColumns(schemaNode));
            schema.setExcludeColumns(loadExcludeColumns(schemaNode));
            schema.setIncludeProcedures(loadIncludeProcedures(schemaNode));
            schema.setExcludeProcedures(loadExcludeProcedures(schemaNode));
            res.add(schema);
        }

        return res;
    }

    private Collection<Catalog> loadCatalogs(Node parent) {
        List<Node> catalogs = getElementsByTagName(parent, "catalog");
        Collection<Catalog> res = new LinkedList<Catalog>();
        for (Node catalogNode : catalogs) {
            Catalog catalog = new Catalog();

            catalog.setName(loadName(catalogNode));
            catalog.setSchemas(loadSchemas(catalogNode));
            catalog.setIncludeTables(loadIncludeTables(catalogNode));
            catalog.setExcludeTables(loadExcludeTables(catalogNode));
            catalog.setIncludeColumns(loadIncludeColumns(catalogNode));
            catalog.setExcludeColumns(loadExcludeColumns(catalogNode));
            catalog.setIncludeProcedures(loadIncludeProcedures(catalogNode));
            catalog.setExcludeProcedures(loadExcludeProcedures(catalogNode));

            res.add(catalog);
        }

        return res;
    }

    private Collection<String> loadTableTypes(Node parent) {
        List<Node> types = getElementsByTagName(parent, "tableType");
        Collection<String> res = new LinkedList<String>();
        for (Node typeNode : types) {
            res.add(loadName(typeNode));
        }

        return res;
    }

    private String loadName(Node catalogNode) {
        return loadByName(catalogNode, "name");
    }

    private String loadPattern(Node catalogNode) {
        return loadByName(catalogNode, "pattern");
    }

    private String loadByName(Node node, String attrName) {
        Node name = node.getAttributes().getNamedItem(attrName);
        if (name != null) {
            return name.getTextContent();
        }

        String content = node.getTextContent().trim();
        if (!content.isEmpty()) {
            return content;
        }

        List<Node> names = getElementsByTagName(node, attrName);
        if (names.isEmpty()) {
            return null;
        }

        return names.get(0).getTextContent();
    }

    private <T extends PatternParam> Collection<T> loadPatternParams(Class<T> clazz, List<Node> nodes) {
        Collection<T> res = new LinkedList<T>();
        for (Node node : nodes) {
            try {
                T obj = clazz.newInstance();
                obj.setPattern(loadPattern(node));

                res.add(obj);
            } catch (InstantiationException e) {
                LOG.info(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                LOG.info(e.getMessage(), e);
            }
        }
        return res;
    }

    private List<Node> getElementsByTagName(Node catalogNode, String name) {
        List<Node> nodes = new LinkedList<Node>();
        NodeList childNodes = catalogNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (name.equals(item.getNodeName())) {
                nodes.add(item);
            }
        }

        return nodes;
    }
}
