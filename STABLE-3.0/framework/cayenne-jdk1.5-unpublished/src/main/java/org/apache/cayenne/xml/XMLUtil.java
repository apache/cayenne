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

package org.apache.cayenne.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.CollectionUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Static utility methods to work with DOM trees.
 * 
 * @since 1.2
 */
class XMLUtil {
    
    // note that per CAY-792, to be locale-safe the format must not contain literal parts
    static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss zzz"; 

    static DocumentBuilderFactory sharedFactory;

    /**
     * Creates a new instance of DocumentBuilder using the default factory.
     */
    static DocumentBuilder newBuilder() throws CayenneRuntimeException {
        if (sharedFactory == null) {
            sharedFactory = DocumentBuilderFactory.newInstance();
        }

        try {
            return sharedFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new CayenneRuntimeException("Can't create DocumentBuilder", e);
        }
    }

    /**
     * Moves all children of the oldParent to the newParent
     */
    static List<Element> replaceParent(Node oldParent, Node newParent) {

        List<Element> children = XMLUtil.getChildren(oldParent);
        for (Node child : children) {
            oldParent.removeChild(child);
            newParent.appendChild(child);
        }

        return children;
    }

    /**
     * Returns text content of a given Node.
     */
    static String getText(Node node) {

        NodeList nodes = node.getChildNodes();
        int len = nodes.getLength();

        if (len == 0) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Node child = nodes.item(i);

            if (child instanceof CharacterData) {
                text.append(((CharacterData) child).getData());
            }
        }

        return text.length() == 0 ? null : text.toString();
    }

    /**
     * Returns the first element among the direct children that has a matching name.
     */
    static Element getChild(Node node, final String name) {
        Predicate p = new Predicate() {

            public boolean evaluate(Object object) {
                if (object instanceof Element) {
                    Element e = (Element) object;
                    return name.equals(e.getNodeName());
                }

                return false;
            }
        };

        return (Element) firstMatch(node.getChildNodes(), p);
    }

    /**
     * Returns all elements among the direct children that have a matching name.
     */
    static List<Element> getChildren(Node node, final String name) {
        
        Predicate p = new Predicate() {
            public boolean evaluate(Object object) {
                if (object instanceof Element) {
                    Element e = (Element) object;
                    return name.equals(e.getNodeName());
                }
                return false;
            }
        };

        return (List<Element>) CollectionUtils.select(getChildren(node), p);
    }

    /**
     * Returns all children of a given Node that are Elements.
     */
    static List<Element> getChildren(Node node) {
        NodeList list = node.getChildNodes();
        int len = list.getLength();
        
        List<Element> children = new ArrayList<Element>(len);
        for (int i = 0; i < len; i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                children.add((Element) child);
            }
        }

        return children;
    }

    private static Node firstMatch(NodeList list, Predicate predicate) {
        int len = list.getLength();

        for (int i = 0; i < len; i++) {
            Node node = list.item(i);
            if (predicate.evaluate(node)) {
                return node;
            }
        }

        return null;
    }

}
