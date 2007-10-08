/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.Predicate;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Static utility methods to work with DOM trees.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class XMLUtil {

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
    static List replaceParent(Node oldParent, Node newParent) {

        List children = XMLUtil.getChildren(oldParent);

        Iterator it = children.iterator();
        while (it.hasNext()) {
            Element child = (Element) it.next();
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

        StringBuffer text = new StringBuffer();
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
    static List getChildren(Node node, final String name) {
        Predicate p = new Predicate() {

            public boolean evaluate(Object object) {
                if (object instanceof Element) {
                    Element e = (Element) object;
                    return name.equals(e.getNodeName());
                }

                return false;
            }
        };

        return allMatches(node.getChildNodes(), p);
    }

    /**
     * Returns all children of a given Node that are Elements.
     */
    static List getChildren(Node node) {
        Predicate p = new Predicate() {

            public boolean evaluate(Object object) {
                return object instanceof Element;
            }
        };

        return allMatches(node.getChildNodes(), p);
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

    private static List allMatches(NodeList list, Predicate predicate) {
        int len = list.getLength();
        List children = new ArrayList(len);

        for (int i = 0; i < len; i++) {
            Node node = list.item(i);
            if (predicate.evaluate(node)) {
                children.add(node);
            }
        }

        return children;
    }
}
