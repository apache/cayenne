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
package org.objectstyle.cayenne.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.project.DataMapFile;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default MapLoader. Its responsibilities include reading DataMaps
 * from XML files and saving DataMap objects back to XML.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 * @author Andriy Shapochka
 */
public class MapLoader extends DefaultHandler {
    private static final Logger logObj = Logger.getLogger(MapLoader.class);

    public static final String DATA_MAP_TAG = "data-map";
    public static final String PROPERTY_TAG = "property";
    public static final String DB_ENTITY_TAG = "db-entity";
    public static final String OBJ_ENTITY_TAG = "obj-entity";
    public static final String DB_ATTRIBUTE_TAG = "db-attribute";
    public static final String DB_ATTRIBUTE_DERIVED_TAG = "db-attribute-derived";
    public static final String DB_ATTRIBUTE_REF_TAG = "db-attribute-ref";
    public static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
    public static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";
    public static final String DB_RELATIONSHIP_TAG = "db-relationship";
    public static final String DB_RELATIONSHIP_REF_TAG = "db-relationship-ref";
    public static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";
    public static final String PROCEDURE_TAG = "procedure";
    public static final String PROCEDURE_PARAMETER_TAG = "procedure-parameter";

    // Query-related
    public static final String QUERY_TAG = "query";
    public static final String QUERY_RESULT_COLUMN_TAG = "result-column";
    public static final String QUERY_SQL_TAG = "sql";
    public static final String QUERY_QUALIFIER_TAG = "qualifier";
    public static final String QUERY_ORDERING_TAG = "ordering";
    public static final String QUERY_PREFETCH_TAG = "prefetch";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    public static final String DB_GENERATOR_TYPE_TAG = "db-generator-type";
    public static final String DB_GENERATOR_NAME_TAG = "db-generator-name";
    public static final String DB_KEY_CACHE_SIZE_TAG = "db-key-cache-size";

    // Reading from XML
    private DataMap dataMap;
    private DbEntity dbEntity;
    private ObjEntity objEntity;
    private DbRelationship dbRelationship;
    private ObjRelationship objRelationship;
    private DbAttribute attrib;
    private Procedure procedure;
    private QueryBuilder queryBuilder;
    private String sqlKey;
    private String descending;
    private String ignoreCase;

    private String currentTag;
    private StringBuffer charactersBuffer;
    private Map mapProperties;

    /** 
     * Prints DataMap encoded as XML to a provided PrintWriter.
     * 
     * @deprecated Since 1.1 use DataMap.encodeAsXML().
     */
    public synchronized void storeDataMap(PrintWriter out, DataMap map)
        throws DataMapException {
        map.encodeAsXML(out);
    }

    /** 
     * Loads a DataMap from XML input source. 
     */
    public synchronized DataMap loadDataMap(InputSource src) throws DataMapException {
        if (src == null) {
            throw new NullPointerException("Null InputSource.");
        }

        try {
            String mapName = mapNameFromLocation(src.getSystemId());
            dataMap = new DataMap(mapName);
            XMLReader parser = Util.createXmlReader();

            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            parser.parse(src);
        }
        catch (SAXException e) {
            dataMap = null;
            throw new DataMapException(
                "Wrong DataMap format, last processed tag: <" + currentTag,
                Util.unwindException(e));
        }
        catch (Exception e) {
            dataMap = null;
            throw new DataMapException(
                "Error loading DataMap, last processed tag: <" + currentTag,
                Util.unwindException(e));
        }
        return dataMap;
    }

    /**
     * @deprecated since 1.1 explicit DataMap dependencies are not supported, and 
     * {@link #loadDataMap(InputSource)} should be used.
     */
    public synchronized DataMap loadDataMap(InputSource src, List deps)
        throws DataMapException {
        DataMap map = loadDataMap(src);

        Iterator it = deps.iterator();
        while (it.hasNext()) {
            map.addDependency((DataMap) it.next());
        }

        return map;
    }

    /**
     * Loads DataMap from file specified by <code>uri</code> parameter.
     *
     * @throws DataMapException if source URI does not resolve to a valid map files
     */
    public DataMap loadDataMap(String uri) throws DataMapException {
        // configure resource locator
        ResourceLocator locator = configLocator();
        InputStream in = locator.findResourceStream(uri);
        if (in == null) {
            throw new DataMapException("Can't find data map " + uri);
        }

        try {
            InputSource inSrc = new InputSource(in);
            inSrc.setSystemId(uri);
            return loadDataMap(inSrc);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ioex) {
            }
        }
    }

    /**
     * Helper method to guess the map name from its location.
     */
    protected String mapNameFromLocation(String location) {
        if (location == null) {
            return "Untitled";
        }

        int lastSlash = location.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = location.lastIndexOf('\\');
        }

        if (lastSlash >= 0 && lastSlash + 1 < location.length()) {
            location = location.substring(lastSlash + 1);
        }

        if (location.endsWith(DataMapFile.LOCATION_SUFFIX)) {
            location =
                location.substring(
                    0,
                    location.length() - DataMapFile.LOCATION_SUFFIX.length());
        }

        return location;
    }

    /**
     * Creates, configures and returns ResourceLocator object used
     * to lookup DataMap files.
     */
    protected ResourceLocator configLocator() {
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(false);
        locator.setSkipHomeDirectory(false);

        // Configuration superclass statically defines what
        // ClassLoader to use for resources. This
        // allows applications to control where resources
        // are loaded from.
        locator.setClassLoader(Configuration.getResourceLoader());

        return locator;
    }

    public void startElement(
        String namespaceUri,
        String localName,
        String qName,
        Attributes attributes)
        throws SAXException {
        
        rememberCurrentTag(localName);
        if (localName.equals(DATA_MAP_TAG)) {
        }
        else if (localName.equals(DB_ENTITY_TAG)) {
            processStartDbEntity(attributes);
        }
        else if (localName.equals(DB_ATTRIBUTE_TAG)) {
            processStartDbAttribute(attributes);
        }
        else if (localName.equals(DB_ATTRIBUTE_DERIVED_TAG)) {
            processStartDerivedDbAttribute(attributes);
        }
        else if (localName.equals(DB_ATTRIBUTE_REF_TAG)) {
            processStartDbAttributeRef(attributes);
        }
        else if (localName.equals(OBJ_ENTITY_TAG)) {
            processStartObjEntity(attributes);
        }
        else if (localName.equals(OBJ_ATTRIBUTE_TAG)) {
            processStartObjAttribute(attributes);
        }
        else if (localName.equals(DB_RELATIONSHIP_TAG)) {
            processStartDbRelationship(attributes);
        }
        else if (localName.equals(DB_ATTRIBUTE_PAIR_TAG)) {
            processStartDbAttributePair(attributes);
        }
        else if (localName.equals(OBJ_RELATIONSHIP_TAG)) {
            processStartObjRelationship(attributes);
        }
        else if (localName.equals(DB_RELATIONSHIP_REF_TAG)) {
            processStartDbRelationshipRef(attributes);
        }
        else if (localName.equals(PROCEDURE_PARAMETER_TAG)) {
            processStartProcedureParameter(attributes);
        }
        else if (localName.equals(PROCEDURE_TAG)) {
            processStartProcedure(attributes);
        }
        else if (localName.equals(QUERY_TAG)) {
            processStartQuery(attributes);
        }
        else if (localName.equals(QUERY_RESULT_COLUMN_TAG)) {
            processStartQueryResultColumn(attributes);
        }
        else if (localName.equals(QUERY_SQL_TAG)) {
            charactersBuffer = new StringBuffer();
            processStartQuerySQL(attributes);
        }
        else if (localName.equals(QUERY_ORDERING_TAG)) {
            charactersBuffer = new StringBuffer();
            processStartQueryOrdering(attributes);
        }
        else if (localName.equals(QUERY_PREFETCH_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (localName.equals(QUERY_QUALIFIER_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (localName.equals(DB_KEY_GENERATOR_TAG)) {
            processStartDbKeyGenerator(attributes);
        }
        else if (localName.equals(DB_GENERATOR_TYPE_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (localName.equals(DB_GENERATOR_NAME_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (localName.equals(DB_KEY_CACHE_SIZE_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        // properties can belong to query or DataMap
        else if (localName.equals(PROPERTY_TAG)) {
            if (queryBuilder != null) {
                processStartQueryProperty(attributes);
            }
            else {
                processStartDataMapProperty(attributes);
            }
        }
    }

    public void endElement(String namespaceURI, String local_name, String qName)
        throws SAXException {
        if (local_name.equals(DATA_MAP_TAG)) {
            processEndDataMap();
        }
        else if (local_name.equals(DB_ENTITY_TAG)) {
            processEndDbEntity();
        }
        else if (local_name.equals(OBJ_ENTITY_TAG)) {
            processEndObjEntity();
        }
        else if (local_name.equals(DB_ATTRIBUTE_TAG)) {
            processEndDbAttribute();
        }
        else if (local_name.equals(DB_ATTRIBUTE_DERIVED_TAG)) {
            processEndDbAttribute();
        }
        else if (local_name.equals(DB_RELATIONSHIP_TAG)) {
            processEndDbRelationship();
        }
        else if (local_name.equals(OBJ_RELATIONSHIP_TAG)) {
            processEndObjRelationship();
        }
        else if (local_name.equals(DB_KEY_GENERATOR_TAG)) {
        }
        else if (local_name.equals(DB_GENERATOR_TYPE_TAG)) {
            processEndDbGeneratorType();
        }
        else if (local_name.equals(DB_GENERATOR_NAME_TAG)) {
            processEndDbGeneratorName();
        }
        else if (local_name.equals(DB_KEY_CACHE_SIZE_TAG)) {
            processEndDbKeyCacheSize();
        }
        else if (local_name.equals(PROCEDURE_PARAMETER_TAG)) {
            processEndProcedureParameter();
        }
        else if (local_name.equals(PROCEDURE_TAG)) {
            processEndProcedure();
        }
        else if (local_name.equals(QUERY_TAG)) {
            processEndQuery();
        }
        else if (local_name.equals(QUERY_SQL_TAG)) {
            processEndQuerySQL();
        }
        else if (local_name.equals(QUERY_QUALIFIER_TAG)) {
            processEndQualifier();
        }
        else if (local_name.equals(QUERY_ORDERING_TAG)) {
            processEndQueryOrdering();
        }
        else if (local_name.equals(QUERY_PREFETCH_TAG)) {
            processEndQueryPrefetch();
        }

        resetCurrentTag();
        charactersBuffer = null;
    }

    public void warning(SAXParseException e) throws SAXException {
        logObj.warn(
            "**Parsing warning**\n"
                + "Line:"
                + e.getLineNumber()
                + "\nMessage:"
                + e.getMessage());
        throw new SAXException("Warning!");
    }

    public void error(SAXParseException e) throws SAXException {
        logObj.error(
            "**Parsing error**\n"
                + "Line:"
                + e.getLineNumber()
                + "\nMessage:"
                + e.getMessage());
        throw new SAXException("Warning!");
    }

    public void fatalError(SAXParseException e) throws SAXException {
        logObj.fatal(
            "**Parsing fatal error**\n"
                + "Line:"
                + e.getLineNumber()
                + "\nMessage:"
                + e.getMessage());
        throw new SAXException("Warning!");
    }

    private void processStartDbEntity(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String parentName = atts.getValue("", "parentName");

        if (parentName != null) {
            dbEntity = new DerivedDbEntity(name);
            ((DerivedDbEntity) dbEntity).setParentEntityName(parentName);
        }
        else {
            dbEntity = new DbEntity(name);
        }

        if (!(dbEntity instanceof DerivedDbEntity)) {
            dbEntity.setSchema(atts.getValue("", "schema"));
            dbEntity.setCatalog(atts.getValue("", "catalog"));
        }

        dataMap.addDbEntity(dbEntity);
    }

    private void processStartDbAttributeRef(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if ((attrib instanceof DerivedDbAttribute)
            && (dbEntity instanceof DerivedDbEntity)) {
            DbEntity parent = ((DerivedDbEntity) dbEntity).getParentEntity();
            DbAttribute ref = (DbAttribute) parent.getAttribute(name);
            ((DerivedDbAttribute) attrib).addParam(ref);
        }
        else {
            throw new SAXException(
                "Referenced attributes are not supported by regular DbAttributes. "
                    + " Offending attribute name '"
                    + attrib.getName()
                    + "'.");
        }
    }

    private void processStartDbAttribute(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        attrib = new DbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
        dbEntity.addAttribute(attrib);

        String temp = atts.getValue("", "length");
        if (temp != null) {
            attrib.setMaxLength(Integer.parseInt(temp));
        }
        temp = atts.getValue("", "precision");
        if (temp != null) {
            attrib.setPrecision(Integer.parseInt(temp));
        }
        temp = atts.getValue("", "isPrimaryKey");
        if (temp != null && temp.equalsIgnoreCase(TRUE)) {
            attrib.setPrimaryKey(true);
        }
        temp = atts.getValue("", "isMandatory");
        if (temp != null && temp.equalsIgnoreCase(TRUE)) {
            attrib.setMandatory(true);
        }
    }

    private void processStartDerivedDbAttribute(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");
        String spec = atts.getValue("", "spec");

        attrib = new DerivedDbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
        ((DerivedDbAttribute) attrib).setExpressionSpec(spec);
        dbEntity.addAttribute(attrib);

        String temp = atts.getValue("", "length");
        if (temp != null) {
            attrib.setMaxLength(Integer.parseInt(temp));
        }
        temp = atts.getValue("", "precision");
        if (temp != null) {
            attrib.setPrecision(Integer.parseInt(temp));
        }
        temp = atts.getValue("", "isPrimaryKey");
        if (temp != null && temp.equalsIgnoreCase(TRUE)) {
            attrib.setPrimaryKey(true);
        }
        temp = atts.getValue("", "isMandatory");
        if (temp != null && temp.equalsIgnoreCase(TRUE)) {
            attrib.setMandatory(true);
        }

        temp = atts.getValue("", "isGroupBy");
        if (temp != null && temp.equalsIgnoreCase(TRUE)) {
            ((DerivedDbAttribute) attrib).setGroupBy(true);
        }
    }

    private void processStartDbKeyGenerator(Attributes atts) throws SAXException {
        DbKeyGenerator pkGenerator = new DbKeyGenerator();
        dbEntity.setPrimaryKeyGenerator(pkGenerator);
    }
    
    private void processStartQuerySQL(Attributes atts) throws SAXException {
        this.sqlKey = atts.getValue("", "adapter-class");
    }

    private void processStartObjEntity(Attributes atts) {
        objEntity = new ObjEntity(atts.getValue("", "name"));
        objEntity.setClassName(atts.getValue("", "className"));

        String readOnly = atts.getValue("", "readOnly");
        objEntity.setReadOnly(TRUE.equalsIgnoreCase(readOnly));

        String lockType = atts.getValue("", "lock-type");
        if ("optimistic".equals(lockType)) {
            objEntity.setDeclaredLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        }

        String superEntityName = atts.getValue("", "superEntityName");
        if (superEntityName != null) {
            objEntity.setSuperEntityName(superEntityName);
        }
        else {
            objEntity.setDbEntityName(atts.getValue("", "dbEntityName"));
            objEntity.setSuperClassName(atts.getValue("", "superClassName"));
        }

        dataMap.addObjEntity(objEntity);
    }

    private void processStartObjAttribute(Attributes atts) {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        String lock = atts.getValue("", "lock");

        ObjAttribute oa = new ObjAttribute(name);
        oa.setType(type);
        oa.setUsedForLocking(TRUE.equalsIgnoreCase(lock));
        objEntity.addAttribute(oa);
        String dbPath = atts.getValue("", "db-attribute-path");
        if (dbPath == null) {
            dbPath = atts.getValue("", "db-attribute-name");
        }
        oa.setDbAttributePath(dbPath);
    }

    private void processStartDbRelationship(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if (name == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartDbRelationship(),"
                    + " Unable to parse name. Attributes:\n"
                    + printAttributes(atts).toString());
        }

        String sourceName = atts.getValue("", "source");
        if (sourceName == null) {
            throw new SAXException("MapLoaderImpl::processStartDbRelationship() - null source entity");
        }

        DbEntity source = dataMap.getDbEntity(sourceName);
        if (source == null) {
            logObj.debug(
                "MapLoaderImpl::processStartDbRelationship() - Unable to find source "
                    + sourceName);
            return;
        }

        String toManyString = atts.getValue("", "toMany");
        boolean toMany = toManyString != null && toManyString.equalsIgnoreCase(TRUE);

        String toDependPkString = atts.getValue("", "toDependentPK");
        boolean toDependentPK =
            toDependPkString != null && toDependPkString.equalsIgnoreCase(TRUE);

        dbRelationship = new DbRelationship(name);
        dbRelationship.setSourceEntity(source);
        dbRelationship.setTargetEntityName(atts.getValue("", "target"));
        dbRelationship.setToMany(toMany);
        dbRelationship.setToDependentPK(toDependentPK);

        source.addRelationship(dbRelationship);
    }

    private void processStartDbRelationshipRef(Attributes atts) throws SAXException {
        // db-relationship-ref element is deprecated and is supported for backwards 
        // compatibility only

        String name = atts.getValue("", "name");
        if (name == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartDbRelationshipRef()"
                    + ", Null DbRelationship name for "
                    + objRelationship.getName());
        }

        String path = objRelationship.getDbRelationshipPath();
        path = (path != null) ? path + "." + name : name;
        objRelationship.setDbRelationshipPath(path);
    }

    private void processStartDbAttributePair(Attributes atts) throws SAXException {
        DbJoin join = new DbJoin(dbRelationship);
        join.setSourceName(atts.getValue("", "source"));
        join.setTargetName(atts.getValue("", "target"));
        dbRelationship.addJoin(join);
    }

    private void processStartObjRelationship(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if (null == name) {
            throw new SAXException(
                "MapLoaderImpl::processStartObjRelationship(),"
                    + " Unable to parse target. Attributes:\n"
                    + printAttributes(atts).toString());
        }

        String sourceName = atts.getValue("", "source");
        if (sourceName == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartObjRelationship(),"
                    + " Unable to parse source. Attributes:\n"
                    + printAttributes(atts).toString());
        }

        ObjEntity source = dataMap.getObjEntity(sourceName);
        if (source == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartObjRelationship(),"
                    + " Unable to find source "
                    + sourceName);
        }

        String deleteRuleName = atts.getValue("", "deleteRule");
        int deleteRule =
            (deleteRuleName != null)
                ? DeleteRule.deleteRuleForName(deleteRuleName)
                : DeleteRule.NO_ACTION;

        objRelationship = new ObjRelationship(name);
        objRelationship.setSourceEntity(source);
        objRelationship.setTargetEntityName(atts.getValue("", "target"));
        objRelationship.setDeleteRule(deleteRule);
        objRelationship.setUsedForLocking(
            TRUE.equalsIgnoreCase(atts.getValue("", "lock")));
        objRelationship.setDbRelationshipPath((atts.getValue("", "db-relationship-path")));
        source.addRelationship(objRelationship);
    }

    private void processStartProcedure(Attributes attributes) throws SAXException {

        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException(
                "MapLoaderImpl::processStartProcedure()," + " no procedure name.");
        }

        String schema = attributes.getValue("", "schema");
        String catalog = attributes.getValue("", "catalog");
        String returningValue = attributes.getValue("", "returningValue");

        procedure = new Procedure(name);
        procedure.setReturningValue(
            returningValue != null && returningValue.equalsIgnoreCase(TRUE));
        procedure.setSchema(schema);
        procedure.setCatalog(catalog);
        dataMap.addProcedure(procedure);
    }

    private void processStartProcedureParameter(Attributes attributes)
        throws SAXException {

        String name = attributes.getValue("", "name");
        if (name == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartProcedureParameter(),"
                    + " no procedure parameter name.");
        }

        ProcedureParameter parameter = new ProcedureParameter(name);

        String type = attributes.getValue("", "type");
        if (type != null) {
            parameter.setType(TypesMapping.getSqlTypeByName(type));
        }

        String length = attributes.getValue("", "length");
        if (length != null) {
            parameter.setMaxLength(Integer.parseInt(length));
        }

        String precision = attributes.getValue("", "precision");
        if (precision != null) {
            parameter.setPrecision(Integer.parseInt(precision));
        }

        String direction = attributes.getValue("", "direction");
        if ("in".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_PARAMETER);
        }
        else if ("out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.OUT_PARAMETER);
        }
        else if ("in_out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
        }

        procedure.addCallParameter(parameter);
    }

    private void processStartQuery(Attributes attributes) throws SAXException {
        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartQuery(), no query name.");
        }

        String builder = attributes.getValue("", "factory");

        // TODO: this is a hack to migrate between 1.1M6 and 1.1M7...
        // remove this at some point
        if (builder == null) {
            builder = SelectQueryBuilder.class.getName();
        }
        else if (builder.equals("org.objectstyle.cayenne.query.SelectQueryBuilder")) {
            builder = SelectQueryBuilder.class.getName();
        }

        try {
            queryBuilder = (QueryBuilder) Class.forName(builder).newInstance();
        }
        catch (Exception ex) {
            throw new SAXException(
                    "MapLoader::processStartQuery(), invalid query builder: " + builder);
        }

        String rootType = attributes.getValue("", "root");
        String rootName = attributes.getValue("", "root-name");
        String resultType = attributes.getValue("", "result-type");
        String selecting = attributes.getValue("", "selecting");

        queryBuilder.setName(name);
        queryBuilder.setRoot(dataMap, rootType, rootName);
        queryBuilder.setSelecting(selecting);
        queryBuilder.setResultType(resultType);
    }

    private void processStartQueryProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartQueryProperty(), no property name.");
        }

        String value = attributes.getValue("", "value");
        if (null == value) {
            throw new SAXException("MapLoader::processStartQueryProperty(), no property value.");
        }

        queryBuilder.addProperty(name, value);
    }
    
    private void processStartDataMapProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartDataMapProperty(), no property name.");
        }

        String value = attributes.getValue("", "value");
        if (null == value) {
            throw new SAXException("MapLoader::processStartDataMapProperty(), no property value.");
        }

        if(mapProperties == null) {
            mapProperties = new TreeMap();
        }
        
        mapProperties.put(name, value);
    }

    private void processStartQueryResultColumn(Attributes attributes)
        throws SAXException {
        String label = attributes.getValue("", "label");
        if (label == null) {
            throw new SAXException("MapLoader::processStartQueryResultColumn(), no label.");
        }

        String dbType = attributes.getValue("", "db-type");
        if (dbType == null) {
            throw new SAXException("MapLoader::processStartQueryResultColumn(), no db-type.");
        }

        String javaType = attributes.getValue("", "java-type");
        if (javaType == null) {
            throw new SAXException("MapLoader::processStartQueryResultColumn(), no java-type.");
        }

        queryBuilder.addResultColumn(label, dbType, javaType);
    }

    private void processEndQueryPrefetch() throws SAXException {
        queryBuilder.addPrefetch(charactersBuffer.toString());
    }

    private void processStartQueryOrdering(Attributes attributes) throws SAXException {
        descending = attributes.getValue("", "descending");
        ignoreCase = attributes.getValue("", "ignore-case");
    }

    private void processEndQuery() throws SAXException {
        dataMap.addQuery(queryBuilder.getQuery());
        queryBuilder = null;
    }

    private void processEndQuerySQL() throws SAXException {
        queryBuilder.addSql(charactersBuffer.toString(), sqlKey);
        sqlKey = null;
    }

    private void processEndQualifier() throws SAXException {
        String qualifier = charactersBuffer.toString();
        if (qualifier.trim().length() == 0) {
            return;
        }

        // qualifier can belong to ObjEntity or a query
        if (objEntity != null) {
            objEntity.setDeclaredQualifier(Expression.fromString(qualifier));
        }
        else {
            queryBuilder.setQualifier(qualifier);
        }
    }

    private void processEndQueryOrdering() throws SAXException {
        String path = charactersBuffer.toString();
        queryBuilder.addOrdering(path, descending, ignoreCase);
    }

    private void processEndDbAttribute() throws SAXException {
        attrib = null;
    }

    private void processEndDbEntity() {
        dbEntity = null;
    }

    private void processEndProcedure() {
        procedure = null;
    }

    private void processEndProcedureParameter() {
    }

    private void processEndDbGeneratorType() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        pkGenerator.setGeneratorType(charactersBuffer.toString());
        if (pkGenerator.getGeneratorType() == null) {
            dbEntity.setPrimaryKeyGenerator(null);
        }
    }

    private void processEndDbGeneratorName() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        pkGenerator.setGeneratorName(charactersBuffer.toString());
    }

    private void processEndDbKeyCacheSize() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        try {
            pkGenerator.setKeyCacheSize(new Integer(charactersBuffer.toString().trim()));
        }
        catch (Exception ex) {
            pkGenerator.setKeyCacheSize(null);
        }
    }
    
    private void processEndDataMap() {
        if(mapProperties != null) {
            dataMap.initWithProperties(mapProperties);
        }
        
        mapProperties = null;
    }

    private void processEndObjEntity() {
        objEntity = null;
    }

    private void processEndDbRelationship() {
        dbRelationship = null;
    }

    private void processEndObjRelationship() {
        objRelationship = null;
    }

    /** Prints the attributes. Used for error reporting purposes.*/
    private StringBuffer printAttributes(Attributes atts) {
        StringBuffer sb = new StringBuffer();
        String name, value;
        for (int i = 0; i < atts.getLength(); i++) {
            value = atts.getQName(i);
            name = atts.getValue(i);
            sb.append("Name: " + name + "\tValue: " + value + "\n");
        }
        return sb;
    }

    public void characters(char[] text, int start, int length)
        throws org.xml.sax.SAXException {
        if (charactersBuffer != null) {
            charactersBuffer.append(text, start, length);
        }
    }

    private void rememberCurrentTag(String tag) {
        currentTag = tag;
    }

    private void resetCurrentTag() {
        currentTag = null;
    }
}
