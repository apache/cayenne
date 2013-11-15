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

package org.apache.cayenne.wocompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.wocompat.parser.Parser;
import org.apache.commons.collections.IteratorUtils;

/**
 * Helper class used by EOModelProcessor. EOModelHelper loads an EOModel from the
 * specified location and gives its users access to the untyped EOModel information.
 */
public class EOModelHelper {

    private Parser plistParser = new Parser();
    protected URL modelUrl;
    protected Map entityIndex;
    protected Map entityClassIndex;
    protected Map entityQueryIndex;
    protected Map entityClientClassIndex;
    protected DataMap dataMap;
    private Map prototypeValues;

    /**
     * Creates helper instance and tries to locate EOModel and load index file.
     * 
     * @deprecated since 3.2, use {@link #EOModelHelper(URL)}.
     */
    @Deprecated
    public EOModelHelper(String path) throws Exception {
        this(new File(path).toURI().toURL());
    }

    public EOModelHelper(URL modelUrl) throws Exception {

        this.modelUrl = modelUrl;
        this.dataMap = new DataMap(findModelName(modelUrl.toExternalForm()));

        // load index file
        List modelIndex = (List) loadModelIndex().get("entities");

        // load entity indices
        entityIndex = new HashMap();
        entityClassIndex = new HashMap();
        entityClientClassIndex = new HashMap();
        entityQueryIndex = new HashMap();

        Iterator it = modelIndex.iterator();
        while (it.hasNext()) {
            Map info = (Map) it.next();
            String name = (String) info.get("name");

            entityIndex.put(name, loadEntityIndex(name));
            entityQueryIndex.put(name, loadQueryIndex(name));
            entityClassIndex.put(name, info.get("className"));
            Map entityPlistMap = entityPListMap(name);

            // get client class information
            Map internalInfo = (Map) entityPlistMap.get("internalInfo");

            if (internalInfo != null) {
                String clientClassName = (String) internalInfo
                        .get("_javaClientClassName");
                entityClientClassIndex.put(name, clientClassName);
            }
        }

        it = modelIndex.iterator();
        while (it.hasNext()) {
            Map info = (Map) it.next();
            String name = (String) info.get("name");
            Map entityPlistMap = entityPListMap(name);
            List classProperties = (List) entityPlistMap.get("classProperties");
            if (classProperties == null) {
                classProperties = Collections.EMPTY_LIST;
            }

            // get client class information
            Map internalInfo = (Map) entityPlistMap.get("internalInfo");

            List clientClassProperties = (internalInfo != null) ? (List) internalInfo
                    .get("_clientClassPropertyNames") : null;

            // guard against no internal info and no client class properties
            if (clientClassProperties == null) {
                clientClassProperties = Collections.EMPTY_LIST;
            }

            // there is a bug in EOModeler it sometimes keeps outdated properties in
            // the client property list. This removes them
            clientClassProperties.retainAll(classProperties);

            // remove all properties from the entity properties that are already defined
            // in
            // a potential parent class.
            String parentEntity = (String) entityPlistMap.get("parent");
            while (parentEntity != null) {
                Map parentEntityPListMap = entityPListMap(parentEntity);
                List parentClassProps = (List) parentEntityPListMap
                        .get("classProperties");
                classProperties.removeAll(parentClassProps);
                // get client class information of parent
                Map parentInternalInfo = (Map) parentEntityPListMap.get("internalInfo");

                if (parentInternalInfo != null) {
                    List parentClientClassProps = (List) parentInternalInfo
                            .get("_clientClassPropertyNames");
                    clientClassProperties.removeAll(parentClientClassProps);
                }

                parentEntity = (String) parentEntityPListMap.get("parent");
            }

            // put back processed properties to the map
            entityPlistMap.put("classProperties", classProperties);
            // add client classes directly for easier access
            entityPlistMap.put("clientClassProperties", clientClassProperties);
        }
    }

    /**
     * Performs Objective C data types conversion to Java types.
     * 
     * @since 1.1
     * @return String representation for Java type corresponding to String representation
     *         of Objective C type.
     */
    public String javaTypeForEOModelerType(String valueClassName, String valueType) {
        if (valueClassName == null) {
            return null;
        }

        if (valueClassName.equals("NSString")) {
            return String.class.getName();
        }

        if (valueClassName.equals("NSNumber")) {
            Class numericClass = numericAttributeClass(valueType);
            return (numericClass != null) ? numericClass.getName() : Number.class
                    .getName();
        }

        if (valueClassName.equals("NSCalendarDate"))
            return "java.sql.Timestamp";

        if (valueClassName.equals("NSDecimalNumber")) {
            Class numericClass = numericAttributeClass(valueType);
            return (numericClass != null) ? numericClass.getName() : BigDecimal.class
                    .getName();
        }

        if (valueClassName.equals("NSData"))
            return "byte[]";

        // don't know what the class is mapped to...
        // do some minimum sanity check and use as is
        try {
            return Class.forName(valueClassName).getName();
        }
        catch (ClassNotFoundException aClassNotFoundException) {
            try {
                return Class.forName("java.lang." + valueClassName).getName();
            }
            catch (ClassNotFoundException anotherClassNotFoundException) {
                try {
                    return Class.forName("java.util." + valueClassName).getName();
                }
                catch (ClassNotFoundException yetAnotherClassNotFoundException) {
                    try {
                        return ClassLoader
                                .getSystemClassLoader()
                                .loadClass(valueClassName)
                                .getName();
                    }
                    catch (ClassNotFoundException e) {
                        // likely a custom class
                        return valueClassName;
                    }
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    // TODO: create a lookup map, maybe XML-loaded...
    protected Class numericAttributeClass(String valueType) {
        if (valueType == null) {
            return null;
        }
        else if ("b".equals(valueType)) {
            return Byte.class;
        }
        else if ("s".equals(valueType)) {
            return Short.class;
        }
        else if ("i".equals(valueType)) {
            return Integer.class;
        }
        else if ("l".equals(valueType)) {
            return Long.class;
        }
        else if ("f".equals(valueType)) {
            return Float.class;
        }
        else if ("d".equals(valueType)) {
            return Double.class;
        }
        else if ("B".equals(valueType)) {
            return BigDecimal.class;
        }
        else if ("c".equals(valueType)) {
            return Boolean.class;
        }
        else {
            return null;
        }
    }

    /** Returns a DataMap associated with this helper. */
    public DataMap getDataMap() {
        return dataMap;
    }

    /** Returns EOModel location as URL. */
    public URL getModelUrl() {
        return modelUrl;
    }

    /**
     * Returns an iterator of model names.
     */
    public Iterator modelNames() {
        return entityClassIndex.keySet().iterator();
    }

    /**
     * Returns a list of model entity names.
     * 
     * @since 1.1
     */
    public List modelNamesAsList() {
        return new ArrayList(entityClassIndex.keySet());
    }

    public Map getPrototypeAttributeMapFor(String aPrototypeAttributeName) {
        if (prototypeValues == null) {

            Map eoPrototypesEntityMap = this.entityPListMap("EOPrototypes");

            // no prototypes
            if (eoPrototypesEntityMap == null) {
                prototypeValues = Collections.EMPTY_MAP;
            }
            else {
                List eoPrototypeAttributes = (List) eoPrototypesEntityMap
                        .get("attributes");

                prototypeValues = new HashMap();
                Iterator it = eoPrototypeAttributes.iterator();
                while (it.hasNext()) {
                    Map attrMap = (Map) it.next();

                    String attrName = (String) attrMap.get("name");

                    // TODO: why are we copying the original map? can we just use it as
                    // is?
                    Map prototypeAttrMap = new HashMap();
                    prototypeValues.put(attrName, prototypeAttrMap);

                    prototypeAttrMap.put("name", attrMap.get("name"));
                    prototypeAttrMap.put("prototypeName", attrMap.get("prototypeName"));
                    prototypeAttrMap.put("columnName", attrMap.get("columnName"));
                    prototypeAttrMap.put("valueClassName", attrMap.get("valueClassName"));
                    prototypeAttrMap.put("width", attrMap.get("width"));
                    prototypeAttrMap.put("allowsNull", attrMap.get("allowsNull"));
                    prototypeAttrMap.put("scale", attrMap.get("scale"));
                    prototypeAttrMap.put("valueType", attrMap.get("valueType"));
                }
            }
        }

        Map aMap = (Map) prototypeValues.get(aPrototypeAttributeName);
        if (null == aMap)
            aMap = Collections.EMPTY_MAP;

        return aMap;
    }

    /** Returns an info map for the entity called <code>entityName</code>. */
    public Map entityPListMap(String entityName) {
        return (Map) entityIndex.get(entityName);
    }

    /**
     * Returns the iterator over EOFetchSpecification names for a given entity.
     * 
     * @since 1.1
     */
    public Iterator queryNames(String entityName) {
        Map queryPlist = (Map) entityQueryIndex.get(entityName);
        if (queryPlist == null || queryPlist.isEmpty()) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        return queryPlist.keySet().iterator();
    }

    /**
     * Returns a map containing EOFetchSpecification information for entity name and query
     * name. Returns null if no such query is found.
     * 
     * @since 1.1
     */
    public Map queryPListMap(String entityName, String queryName) {
        Map queryPlist = (Map) entityQueryIndex.get(entityName);
        if (queryPlist == null || queryPlist.isEmpty()) {
            return null;
        }

        return (Map) queryPlist.get(queryName);
    }

    public String entityClass(String entityName, boolean getClientClass) {
        if (getClientClass) {
            return (String) entityClientClassIndex.get(entityName);
        }
        else {
            return (String) entityClassIndex.get(entityName);
        }
    }

    /** Loads EOModel index and returns it as a map. */
    protected Map loadModelIndex() throws Exception {
        InputStream indexIn = openIndexStream();
        try {
            plistParser.ReInit(indexIn);
            return (Map) plistParser.propertyList();
        }
        finally {
            indexIn.close();
        }
    }

    /**
     * Loads EOEntity information and returns it as a map.
     */
    protected Map loadEntityIndex(String entityName) throws Exception {
        InputStream entIn = openEntityStream(entityName);
        try {
            plistParser.ReInit(entIn);
            return (Map) plistParser.propertyList();
        }
        finally {
            entIn.close();
        }
    }

    /**
     * Loads EOFetchSpecification information and returns it as a map.
     */
    protected Map loadQueryIndex(String entityName) throws Exception {
        InputStream queryIn = null;

        // catch file open exceptions since not all entities have query files....
        try {
            queryIn = openQueryStream(entityName);
        }
        catch (IOException ioex) {
            return Collections.EMPTY_MAP;
        }

        try {
            plistParser.ReInit(queryIn);
            return (Map) plistParser.propertyList();
        }
        finally {
            queryIn.close();
        }
    }

    /** Returns EOModel name based on its path. */
    protected String findModelName(String path) {
        // strip trailing slashes
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }

        // strip path components
        int i1 = path.lastIndexOf("/");
        int i2 = path.lastIndexOf("\\");
        int i = (i1 > i2) ? i1 : i2;
        if (i >= 0) {
            path = path.substring(i + 1);
        }

        // strip .eomodeld suffix
        if (path.endsWith(".eomodeld")) {
            path = path.substring(0, path.length() - ".eomodeld".length());
        }

        return path;
    }

    /**
     * Returns InputStream to read an EOModel index file.
     */
    protected InputStream openIndexStream() throws Exception {
        return new URL(modelUrl, "index.eomodeld").openStream();
    }

    /**
     * Returns InputStream to read an EOEntity plist file.
     * 
     * @param entityName name of EOEntity to be loaded.
     * @return InputStream to read an EOEntity plist file or null if
     *         <code>entityname.plist</code> file can not be located.
     */
    protected InputStream openEntityStream(String entityName) throws Exception {
        return new URL(modelUrl, entityName + ".plist").openStream();
    }

    /**
     * Returns InputStream to read an EOFetchSpecification plist file.
     * 
     * @param entityName name of EOEntity to be loaded.
     * @return InputStream to read an EOEntity plist file or null if
     *         <code>entityname.plist</code> file can not be located.
     */
    protected InputStream openQueryStream(String entityName) throws Exception {
        return new URL(modelUrl, entityName + ".fspec").openStream();
    }
}
