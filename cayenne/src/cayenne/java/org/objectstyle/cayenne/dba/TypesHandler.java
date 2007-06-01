/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.dba;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * TypesHandler provides JDBC-RDBMS types mapping. Loads types info from 
 * an XML file.
 * 
 * @author Andrei Adamchik
 */
public class TypesHandler {
    private static Logger logObj = Logger.getLogger(TypesHandler.class);

    private static Map handlerMap = new HashMap();

    protected Map typesMap;

    /** 
     * Returns TypesHandler using XML file located in the package of
     * <code>adapterClass</code>.
     * 
     * @deprecated Since 1.1 use {@link #getHandler(URL)}
     */
    public static TypesHandler getHandler(Class adapterClass) {
        return getHandler(Util.getPackagePath(adapterClass.getName()) + "/types.xml");
    }

    /**
     * @deprecated Since 1.1 use {@link #getHandler(URL)}
     */
    public static TypesHandler getHandler(String filePath) {
        URL url = ResourceLocator.findURLInClasspath(filePath);
        return getHandler(url);
    }

    /**
     * @since 1.1
     */
    public static TypesHandler getHandler(URL typesConfig) {
        synchronized (handlerMap) {
            TypesHandler handler = (TypesHandler) handlerMap.get(typesConfig);

            if (handler == null) {
                handler = new TypesHandler(typesConfig);
                handlerMap.put(typesConfig, handler);
            }

            return handler;
        }
    }

    /**
     * Creates new TypesHandler loading configuration info from the XML
     * file specified as <code>typesConfigPath</code> parameter.
     * 
     * @deprecated Since 1.1 use {@link #TypesHandler(URL)}
     */
    public TypesHandler(String typesConfigPath) {
        this(ResourceLocator.findURLInClasspath(typesConfigPath));
    }

    /**
     * Creates new TypesHandler loading configuration info from the XML
     * file specified as <code>typesConfigPath</code> parameter.
     * 
     * @since 1.1
     */
    public TypesHandler(URL typesConfig) {
        try {
            InputStream in = typesConfig.openStream();

            try {
                XMLReader parser = Util.createXmlReader();
                TypesParseHandler ph = new TypesParseHandler();
                parser.setContentHandler(ph);
                parser.setErrorHandler(ph);
                parser.parse(new InputSource(in));

                typesMap = ph.getTypes();
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException(
                    "Error creating TypesHandler '" + typesConfig + "'.",
                    ex);
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException ioex) {
                }
            }
        }
        catch (IOException ioex) {
            throw new CayenneRuntimeException(
                "Error opening config file '" + typesConfig + "'.",
                ioex);
        }
    }

    public String[] externalTypesForJdbcType(int type) {
        return (String[]) typesMap.get(new Integer(type));
    }

    /** 
     * Helper class to load types data from XML.
     */
    final class TypesParseHandler extends DefaultHandler {
        private static final String JDBC_TYPE_TAG = "jdbc-type";
        private static final String DB_TYPE_TAG = "db-type";
        private static final String NAME_ATTR = "name";

        private Map types = new HashMap();
        private List currentTypes = new ArrayList();
        private int currentType = TypesMapping.NOT_DEFINED;

        public Map getTypes() {
            return types;
        }

        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
            if (JDBC_TYPE_TAG.equals(localName)) {
                currentTypes.clear();
                String strType = atts.getValue("", NAME_ATTR);

                // convert to Types int value
                try {
                    currentType = Types.class.getDeclaredField(strType).getInt(null);
                }
                catch (Exception ex) {
                    currentType = TypesMapping.NOT_DEFINED;
                    logObj.info("type not found: '" + strType + "', ignoring.");
                }
            }
            else if (DB_TYPE_TAG.equals(localName)) {
                currentTypes.add(atts.getValue("", NAME_ATTR));
            }
        }

        public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
            if (JDBC_TYPE_TAG.equals(localName)
                && currentType != TypesMapping.NOT_DEFINED) {
                String[] typesAsArray = new String[currentTypes.size()];
                types.put(new Integer(currentType), currentTypes.toArray(typesAsArray));
            }
        }
    }
}
