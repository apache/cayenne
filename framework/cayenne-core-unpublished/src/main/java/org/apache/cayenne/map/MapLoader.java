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

package org.apache.cayenne.map;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default MapLoader. Its responsibilities include reading DataMaps from XML files and
 * saving DataMap objects back to XML.
 */
public class MapLoader extends DefaultHandler {

    // TODO: andrus, 7/17/2006 - move upgrade logic out of here
    final static String _1_2_PACKAGE_PREFIX = "org.objectstyle.cayenne.";
    final static String _2_0_PACKAGE_PREFIX = "org.apache.cayenne.";

    private static Log logger = LogFactory.getLog(MapLoader.class);

    public static final String DATA_MAP_TAG = "data-map";
    public static final String PROPERTY_TAG = "property";

    /**
     * @since 3.0
     */
    public static final String EMBEDDABLE_TAG = "embeddable";

    /**
     * @since 3.0
     */
    public static final String EMBEDDABLE_ATTRIBUTE_TAG = "embeddable-attribute";

    /**
     * @since 3.0
     */
    public static final String EMBEDDED_ATTRIBUTE_TAG = "embedded-attribute";

    /**
     * @since 3.0
     */
    public static final String EMBEDDABLE_ATTRIBUTE_OVERRIDE_TAG = "embeddable-attribute-override";

    public static final String DB_ENTITY_TAG = "db-entity";
    public static final String OBJ_ENTITY_TAG = "obj-entity";
    public static final String DB_ATTRIBUTE_TAG = "db-attribute";
    public static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
    public static final String OBJ_ATTRIBUTE_OVERRIDE_TAG = "attribute-override";
    public static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";
    public static final String DB_RELATIONSHIP_TAG = "db-relationship";
    public static final String DB_RELATIONSHIP_REF_TAG = "db-relationship-ref";
    public static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";
    public static final String PROCEDURE_TAG = "procedure";
    public static final String PROCEDURE_PARAMETER_TAG = "procedure-parameter";

    // lifecycle listeners and callbacks related
    public static final String POST_ADD_TAG = "post-add";
    public static final String PRE_PERSIST_TAG = "pre-persist";
    public static final String POST_PERSIST_TAG = "post-persist";
    public static final String PRE_UPDATE_TAG = "pre-update";
    public static final String POST_UPDATE_TAG = "post-update";
    public static final String PRE_REMOVE_TAG = "pre-remove";
    public static final String POST_REMOVE_TAG = "post-remove";
    public static final String POST_LOAD_TAG = "post-load";

    // Query-related
    public static final String QUERY_TAG = "query";

    public static final String QUERY_SQL_TAG = "sql";
    public static final String QUERY_EJBQL_TAG = "ejbql";
    public static final String QUERY_QUALIFIER_TAG = "qualifier";
    public static final String QUERY_ORDERING_TAG = "ordering";
    public static final String QUERY_PREFETCH_TAG = "prefetch";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    public static final String DB_GENERATOR_TYPE_TAG = "db-generator-type";
    public static final String DB_GENERATOR_NAME_TAG = "db-generator-name";
    public static final String DB_KEY_CACHE_SIZE_TAG = "db-key-cache-size";

    /**
     * @since 3.0
     */
    public static final String OBJ_ENTITY_ROOT = "obj-entity";

    /**
     * @since 3.0
     */
    public static final String DB_ENTITY_ROOT = "db-entity";

    /**
     * @since 3.0
     */
    public static final String PROCEDURE_ROOT = "procedure";

    /**
     * @since 3.0
     */
    public static final String DATA_MAP_ROOT = "data-map";

    /**
     * @since 3.0
     */
    public static final String JAVA_CLASS_ROOT = "java-class";

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    // Reading from XML
    private String mapVersion;
    private DataMap dataMap;
    private DbEntity dbEntity;
    private ObjEntity objEntity;
    private Embeddable embeddable;
    private EmbeddedAttribute embeddedAttribute;
    private DbRelationship dbRelationship;
    private ObjRelationship objRelationship;
    private DbAttribute attrib;
    private Procedure procedure;
    private QueryLoader queryBuilder;
    private String sqlKey;

    private String descending;
    private String ignoreCase;

    private Map<String, StartClosure> startTagOpMap;
    private Map<String, EndClosure> endTagOpMap;
    private String currentTag;
    private Attributes currentAttributes;
    private StringBuilder charactersBuffer;
    private Map<String, Object> mapProperties;

    public MapLoader() {
        // compile tag processors.
        startTagOpMap = new HashMap<String, StartClosure>(40);
        endTagOpMap = new HashMap<String, EndClosure>(40);

        startTagOpMap.put(DATA_MAP_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDataMap(attributes);
            }
        });

        startTagOpMap.put(DB_ENTITY_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbEntity(attributes);
            }
        });

        startTagOpMap.put(DB_ATTRIBUTE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbAttribute(attributes);
            }
        });

        startTagOpMap.put(OBJ_ENTITY_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartObjEntity(attributes);
            }
        });

        startTagOpMap.put(OBJ_ATTRIBUTE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartObjAttribute(attributes);
            }
        });

        startTagOpMap.put(OBJ_ATTRIBUTE_OVERRIDE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartAttributeOverride(attributes);
            }
        });

        startTagOpMap.put(EMBEDDABLE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartEmbeddable(attributes);
            }
        });

        startTagOpMap.put(EMBEDDABLE_ATTRIBUTE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartEmbeddableAttribute(attributes);
            }
        });

        startTagOpMap.put(EMBEDDABLE_ATTRIBUTE_OVERRIDE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartEmbeddableAttributeOverride(attributes);
            }
        });

        startTagOpMap.put(EMBEDDED_ATTRIBUTE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartEmbeddedAttribute(attributes);
            }
        });

        startTagOpMap.put(DB_RELATIONSHIP_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbRelationship(attributes);
            }
        });

        startTagOpMap.put(DB_ATTRIBUTE_PAIR_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbAttributePair(attributes);
            }
        });

        startTagOpMap.put(OBJ_RELATIONSHIP_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartObjRelationship(attributes);
            }
        });

        startTagOpMap.put(DB_RELATIONSHIP_REF_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbRelationshipRef(attributes);
            }
        });

        startTagOpMap.put(PROCEDURE_PARAMETER_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartProcedureParameter(attributes);
            }
        });

        startTagOpMap.put(PROCEDURE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartProcedure(attributes);
            }
        });

        startTagOpMap.put(QUERY_EJBQL_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                charactersBuffer = new StringBuilder();
            }
        });

        startTagOpMap.put(QUERY_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartQuery(attributes);
            }
        });

        startTagOpMap.put(QUERY_SQL_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                charactersBuffer = new StringBuilder();
                processStartQuerySQL(attributes);
            }
        });

        startTagOpMap.put(QUERY_ORDERING_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                charactersBuffer = new StringBuilder();
                processStartQueryOrdering(attributes);
            }
        });

        startTagOpMap.put(DB_KEY_GENERATOR_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartDbKeyGenerator(attributes);
            }
        });

        startTagOpMap.put(PROPERTY_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                // properties can belong to query or DataMap
                if (queryBuilder != null) {
                    processStartQueryProperty(attributes);
                }
                else {
                    processStartDataMapProperty(attributes);
                }
            }
        });

        startTagOpMap.put(POST_ADD_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPostAdd(attributes);
            }
        });

        startTagOpMap.put(PRE_PERSIST_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPrePersist(attributes);
            }
        });

        startTagOpMap.put(POST_PERSIST_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPostPersist(attributes);
            }
        });

        startTagOpMap.put(PRE_UPDATE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPreUpdate(attributes);
            }
        });

        startTagOpMap.put(POST_UPDATE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPostUpdate(attributes);
            }
        });

        startTagOpMap.put(PRE_REMOVE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPreRemove(attributes);
            }
        });

        startTagOpMap.put(POST_REMOVE_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPostRemove(attributes);
            }
        });

        startTagOpMap.put(POST_LOAD_TAG, new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                processStartPostLoad(attributes);
            }
        });

        StartClosure resetBuffer = new StartClosure() {

            @Override
            void execute(Attributes attributes) throws SAXException {
                charactersBuffer = new StringBuilder();
            }
        };

        startTagOpMap.put(QUERY_PREFETCH_TAG, resetBuffer);
        startTagOpMap.put(QUERY_QUALIFIER_TAG, resetBuffer);
        startTagOpMap.put(DB_GENERATOR_TYPE_TAG, resetBuffer);
        startTagOpMap.put(DB_GENERATOR_NAME_TAG, resetBuffer);
        startTagOpMap.put(DB_KEY_CACHE_SIZE_TAG, resetBuffer);

        endTagOpMap.put(DATA_MAP_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDataMap();
            }
        });
        endTagOpMap.put(DB_ENTITY_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbEntity();
            }
        });
        endTagOpMap.put(OBJ_ENTITY_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndObjEntity();
            }
        });
        endTagOpMap.put(EMBEDDABLE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndEmbeddable();
            }
        });
        endTagOpMap.put(EMBEDDABLE_ATTRIBUTE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndEmbeddedAttribute();
            }
        });

        endTagOpMap.put(DB_ATTRIBUTE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbAttribute();
            }
        });

        endTagOpMap.put(DB_RELATIONSHIP_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbRelationship();
            }
        });
        endTagOpMap.put(OBJ_RELATIONSHIP_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndObjRelationship();
            }
        });
        endTagOpMap.put(DB_GENERATOR_TYPE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbGeneratorType();
            }
        });
        endTagOpMap.put(DB_GENERATOR_NAME_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbGeneratorName();
            }
        });
        endTagOpMap.put(DB_KEY_CACHE_SIZE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndDbKeyCacheSize();
            }
        });
        endTagOpMap.put(PROCEDURE_PARAMETER_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndProcedureParameter();
            }
        });
        endTagOpMap.put(PROCEDURE_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndProcedure();
            }
        });
        endTagOpMap.put(QUERY_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndQuery();
            }
        });
        endTagOpMap.put(QUERY_SQL_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndQuerySQL();
            }
        });

        endTagOpMap.put(QUERY_EJBQL_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndEjbqlQuery();
            }
        });

        endTagOpMap.put(QUERY_QUALIFIER_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndQualifier();
            }
        });
        endTagOpMap.put(QUERY_ORDERING_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndQueryOrdering();
            }
        });
        endTagOpMap.put(QUERY_PREFETCH_TAG, new EndClosure() {

            @Override
            void execute() throws SAXException {
                processEndQueryPrefetch();
            }
        });
    }

    private void processStartDataMap(Attributes attributes) {
        this.mapVersion = attributes.getValue("", "project-version");
    }

    private void processStartPostAdd(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            // new callback tags - children of "obj-entity"
            objEntity.getCallbackMap().getPostAdd().addCallbackMethod(methodName);
        }
    }

    private void processStartPrePersist(Attributes attributes) {

        // 3.0 -> 3.0.0.1 upgrade hack... treat pre-persist as post-add
        // only 3.0 used "pre-persist" in a "post-add" sense
        if ("3.0".equals(mapVersion)) {
            processStartPostAdd(attributes);
        }
        else {

            String methodName = attributes.getValue("", "method-name");

            if (objEntity != null) {
                // new callback tags - children of "obj-entity"
                objEntity.getCallbackMap().getPrePersist().addCallbackMethod(methodName);
            }
        }
    }

    private void processStartPostPersist(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPostPersist().addCallbackMethod(methodName);
        }
    }

    private void processStartPreUpdate(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPreUpdate().addCallbackMethod(methodName);
        }
    }

    private void processStartPostUpdate(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPostUpdate().addCallbackMethod(methodName);
        }
    }

    private void processStartPreRemove(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPreRemove().addCallbackMethod(methodName);
        }
    }

    private void processStartPostRemove(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPostRemove().addCallbackMethod(methodName);
        }
    }

    private void processStartPostLoad(Attributes attributes) {
        String methodName = attributes.getValue("", "method-name");
        if (objEntity != null) {
            objEntity.getCallbackMap().getPostLoad().addCallbackMethod(methodName);
        }
    }

    /**
     * Loads a DataMap from XML input source.
     */
    public synchronized DataMap loadDataMap(InputSource src)
            throws CayenneRuntimeException {
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
            throw new CayenneRuntimeException(
                    "Wrong DataMap format, last processed tag: "
                            + constructCurrentStateString(),
                    Util.unwindException(e));
        }
        catch (Exception e) {
            dataMap = null;
            throw new CayenneRuntimeException(
                    "Error loading DataMap, last processed tag: "
                            + constructCurrentStateString(),
                    Util.unwindException(e));
        }
        return dataMap;
    }

    /**
     * Constructs error message for displaying as exception message
     */
    private Appendable constructCurrentStateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(currentTag);

        if (currentAttributes != null) {
            for (int i = 0; i < currentAttributes.getLength(); i++) {
                sb
                        .append(" ")
                        .append(currentAttributes.getLocalName(i))
                        .append("=")
                        .append("\"")
                        .append(currentAttributes.getValue(i))
                        .append("\"");
            }
        }
        sb.append(">");

        return sb;
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

        if (location.endsWith(DATA_MAP_LOCATION_SUFFIX)) {
            location = location.substring(
                    0,
                    location.length() - DATA_MAP_LOCATION_SUFFIX.length());
        }

        return location;
    }

    @Override
    public void startElement(
            String namespaceUri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {

        rememberCurrentState(localName, attributes);

        StartClosure op = startTagOpMap.get(localName);
        if (op != null) {
            op.execute(attributes);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        EndClosure op = endTagOpMap.get(localName);
        if (op != null) {
            op.execute();
        }

        resetCurrentState();
        charactersBuffer = null;
    }

    private void processStartEmbeddable(Attributes atts) {
        embeddable = new Embeddable(atts.getValue("", "className"));
        dataMap.addEmbeddable(embeddable);
    }

    private void processStartEmbeddableAttribute(Attributes atts) {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");
        String dbName = atts.getValue("", "db-attribute-name");

        EmbeddableAttribute ea = new EmbeddableAttribute(name);
        ea.setType(type);
        ea.setDbAttributeName(dbName);
        embeddable.addAttribute(ea);
    }

    private void processStartEmbeddedAttribute(Attributes atts) {

        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        embeddedAttribute = new EmbeddedAttribute(name);
        embeddedAttribute.setType(type);
        objEntity.addAttribute(embeddedAttribute);
    }

    private void processStartEmbeddableAttributeOverride(Attributes atts) {
        String name = atts.getValue("", "name");
        String dbName = atts.getValue("", "db-attribute-path");
        embeddedAttribute.addAttributeOverride(name, dbName);
    }

    private void processStartDbEntity(Attributes atts) {
        String name = atts.getValue("", "name");

        dbEntity = new DbEntity(name);
        dbEntity.setSchema(atts.getValue("", "schema"));
        dbEntity.setCatalog(atts.getValue("", "catalog"));

        dataMap.addDbEntity(dbEntity);
    }

    private void processStartDbAttribute(Attributes atts) {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        attrib = new DbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
        dbEntity.addAttribute(attrib);

        String length = atts.getValue("", "length");
        if (length != null) {
            attrib.setMaxLength(Integer.parseInt(length));
        }

        // this is an obsolete 1.2 'precision' attribute that really meant 'scale'
        String pseudoPrecision = atts.getValue("", "precision");
        if (pseudoPrecision != null) {
            attrib.setScale(Integer.parseInt(pseudoPrecision));
        }

        String precision = atts.getValue("", "attributePrecision");
        if (precision != null) {
            attrib.setAttributePrecision(Integer.parseInt(precision));
        }

        String scale = atts.getValue("", "scale");
        if (scale != null) {
            attrib.setScale(Integer.parseInt(scale));
        }

        attrib.setPrimaryKey(TRUE.equalsIgnoreCase(atts.getValue("", "isPrimaryKey")));
        attrib.setMandatory(TRUE.equalsIgnoreCase(atts.getValue("", "isMandatory")));
        attrib.setGenerated(TRUE.equalsIgnoreCase(atts.getValue("", "isGenerated")));
    }

    private void processStartDbKeyGenerator(Attributes atts) {
        DbKeyGenerator pkGenerator = new DbKeyGenerator();
        dbEntity.setPrimaryKeyGenerator(pkGenerator);
    }

    private void processStartQuerySQL(Attributes atts) {
        this.sqlKey = convertClassNameFromV1_2(atts.getValue("", "adapter-class"));
    }

    private void processStartObjEntity(Attributes atts) {
        objEntity = new ObjEntity(atts.getValue("", "name"));
        objEntity.setClassName(atts.getValue("", "className"));
        objEntity.setClientClassName(atts.getValue("", "clientClassName"));

        String isAbstract = atts.getValue("", "abstract");
        objEntity.setAbstract(TRUE.equalsIgnoreCase(isAbstract));

        String readOnly = atts.getValue("", "readOnly");
        objEntity.setReadOnly(TRUE.equalsIgnoreCase(readOnly));

        String serverOnly = atts.getValue("", "serverOnly");
        objEntity.setServerOnly(TRUE.equalsIgnoreCase(serverOnly));

        String excludeSuperclassListeners = atts.getValue(
                "",
                "exclude-superclass-listeners");
        objEntity.setExcludingSuperclassListeners(TRUE
                .equalsIgnoreCase(excludeSuperclassListeners));

        String excludeDefaultListeners = atts.getValue("", "exclude-default-listeners");
        objEntity.setExcludingDefaultListeners(TRUE
                .equalsIgnoreCase(excludeDefaultListeners));

        String lockType = atts.getValue("", "lock-type");
        if ("optimistic".equals(lockType)) {
            objEntity.setDeclaredLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        }

        String superEntityName = atts.getValue("", "superEntityName");
        if (superEntityName != null) {
            objEntity.setSuperEntityName(superEntityName);
        }
        else {
            objEntity.setSuperClassName(atts.getValue("", "superClassName"));
            objEntity.setClientSuperClassName(atts.getValue("", "clientSuperClassName"));
        }

        objEntity.setDbEntityName(atts.getValue("", "dbEntityName"));

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

    private void processStartAttributeOverride(Attributes atts) {
        String name = atts.getValue("", "name");
        String dbPath = atts.getValue("", "db-attribute-path");

        objEntity.addAttributeOverride(name, dbPath);
    }

    private void processStartDbRelationship(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if (name == null) {
            throw new SAXException("MapLoader::processStartDbRelationship(),"
                    + " Unable to parse name. Attributes:\n"
                    + printAttributes(atts));
        }

        String sourceName = atts.getValue("", "source");
        if (sourceName == null) {
            throw new SAXException(
                    "MapLoader::processStartDbRelationship() - null source entity");
        }

        DbEntity source = dataMap.getDbEntity(sourceName);
        if (source == null) {
            return;
        }

        String toManyString = atts.getValue("", "toMany");
        boolean toMany = toManyString != null && toManyString.equalsIgnoreCase(TRUE);

        String toDependPkString = atts.getValue("", "toDependentPK");
        boolean toDependentPK = toDependPkString != null
                && toDependPkString.equalsIgnoreCase(TRUE);

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
            throw new SAXException("MapLoader::processStartDbRelationshipRef()"
                    + ", Null DbRelationship name for "
                    + objRelationship.getName());
        }

        String path = objRelationship.getDbRelationshipPath();
        path = (path != null) ? path + "." + name : name;
        objRelationship.setDbRelationshipPath(path);
    }

    private void processStartDbAttributePair(Attributes atts) {
        DbJoin join = new DbJoin(dbRelationship);
        join.setSourceName(atts.getValue("", "source"));
        join.setTargetName(atts.getValue("", "target"));
        dbRelationship.addJoin(join);
    }

    private void processStartObjRelationship(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartObjRelationship(),"
                    + " Unable to parse target. Attributes:\n"
                    + printAttributes(atts));
        }

        String collectionType = atts.getValue("", "collection-type");
        String mapKey = atts.getValue("", "map-key");

        String sourceName = atts.getValue("", "source");
        if (sourceName == null) {
            throw new SAXException("MapLoader::processStartObjRelationship(),"
                    + " Unable to parse source. Attributes:\n"
                    + printAttributes(atts));
        }

        ObjEntity source = dataMap.getObjEntity(sourceName);
        if (source == null) {
            throw new SAXException("MapLoader::processStartObjRelationship(),"
                    + " Unable to find source "
                    + sourceName);
        }

        String deleteRuleName = atts.getValue("", "deleteRule");
        int deleteRule = (deleteRuleName != null) ? DeleteRule
                .deleteRuleForName(deleteRuleName) : DeleteRule.NO_ACTION;

        objRelationship = new ObjRelationship(name);
        objRelationship.setSourceEntity(source);
        objRelationship.setTargetEntityName(atts.getValue("", "target"));
        objRelationship.setDeleteRule(deleteRule);
        objRelationship
                .setUsedForLocking(TRUE.equalsIgnoreCase(atts.getValue("", "lock")));
        objRelationship.setDeferredDbRelationshipPath((atts.getValue(
                "",
                "db-relationship-path")));
        objRelationship.setCollectionType(collectionType);
        objRelationship.setMapKey(mapKey);
        source.addRelationship(objRelationship);
    }

    private void processStartProcedure(Attributes attributes) throws SAXException {

        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException("MapLoader::processStartProcedure(),"
                    + " no procedure name.");
        }

        String schema = attributes.getValue("", "schema");
        String catalog = attributes.getValue("", "catalog");
        String returningValue = attributes.getValue("", "returningValue");

        procedure = new Procedure(name);
        procedure.setReturningValue(returningValue != null
                && returningValue.equalsIgnoreCase(TRUE));
        procedure.setSchema(schema);
        procedure.setCatalog(catalog);
        dataMap.addProcedure(procedure);
    }

    private void processStartProcedureParameter(Attributes attributes)
            throws SAXException {

        String name = attributes.getValue("", "name");
        if (name == null) {
            throw new SAXException("MapLoader::processStartProcedureParameter(),"
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

        if (builder == null) {
            builder = SelectQueryBuilder.class.getName();
        }
        else {
            // TODO: this is a hack to migrate between 1.1M6 and 1.1M7...
            // remove this at some point
            if (builder.equals("org.objectstyle.cayenne.query.SelectQueryBuilder")) {
                builder = SelectQueryBuilder.class.getName();
            }
            // upgrade from v. <= 1.2
            else {
                builder = convertClassNameFromV1_2(builder);
            }
        }

        try {
            queryBuilder = (QueryLoader) Class.forName(builder).newInstance();
        }
        catch (Exception ex) {
            throw new SAXException(
                    "MapLoader::processStartQuery(), invalid query builder: " + builder);
        }

        String rootType = attributes.getValue("", "root");
        String rootName = attributes.getValue("", "root-name");
        String resultEntity = attributes.getValue("", "result-entity");

        queryBuilder.setName(name);
        queryBuilder.setRoot(dataMap, rootType, rootName);

        // TODO: Andrus, 2/13/2006 'result-type' is only used in ProcedureQuery and is
        // deprecated in 1.2
        if (!Util.isEmptyString(resultEntity)) {
            queryBuilder.setResultEntity(resultEntity);
        }
    }

    private void processStartQueryProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException(
                    "MapLoader::processStartQueryProperty(), no property name.");
        }

        String value = attributes.getValue("", "value");
        if (null == value) {
            throw new SAXException(
                    "MapLoader::processStartQueryProperty(), no property value.");
        }

        queryBuilder.addProperty(name, value);
    }

    private void processStartDataMapProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException(
                    "MapLoader::processStartDataMapProperty(), no property name.");
        }

        String value = attributes.getValue("", "value");
        if (null == value) {
            throw new SAXException(
                    "MapLoader::processStartDataMapProperty(), no property value.");
        }

        if (mapProperties == null) {
            mapProperties = new TreeMap<String, Object>();
        }

        mapProperties.put(name, value);
    }

    private void processEndQueryPrefetch() {
        queryBuilder.addPrefetch(charactersBuffer.toString());
    }

    private void processStartQueryOrdering(Attributes attributes) {
        descending = attributes.getValue("", "descending");
        ignoreCase = attributes.getValue("", "ignore-case");
    }

    private void processEndQuery() {
        dataMap.addQuery(queryBuilder.getQuery());
        queryBuilder = null;
    }

    private void processEndEjbqlQuery() throws SAXException {
        queryBuilder.setEjbql(charactersBuffer.toString());
    }

    private void processEndQuerySQL() {
        queryBuilder.addSql(charactersBuffer.toString(), sqlKey);
        sqlKey = null;
    }

    private void processEndQualifier() {
        String qualifier = charactersBuffer.toString();
        if (qualifier.trim().length() == 0) {
            return;
        }

        // qualifier can belong to ObjEntity, DbEntity or a query
        if (objEntity != null) {
            objEntity.setDeclaredQualifier(Expression.fromString(qualifier));
        }
        else if (dbEntity != null) {
            dbEntity.setQualifier(Expression.fromString(qualifier));
        }
        else {
            queryBuilder.setQualifier(qualifier);
        }
    }

    private void processEndQueryOrdering() {
        String path = charactersBuffer.toString();
        queryBuilder.addOrdering(path, descending, ignoreCase);
    }

    private void processEndDbAttribute() {
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
        if (mapProperties != null) {
            dataMap.initWithProperties(mapProperties);
        }

        mapProperties = null;
        mapVersion = null;
    }

    private void processEndObjEntity() {
        objEntity = null;
    }

    private void processEndEmbeddable() {
        embeddable = null;
    }

    private void processEndEmbeddedAttribute() {
        embeddedAttribute = null;
    }

    private void processEndDbRelationship() {
        dbRelationship = null;
    }

    private void processEndObjRelationship() {
        objRelationship = null;
    }

    /** Prints the attributes. Used for error reporting purposes. */
    private StringBuffer printAttributes(Attributes atts) {
        StringBuffer sb = new StringBuffer();
        String name, value;
        for (int i = 0; i < atts.getLength(); i++) {
            value = atts.getQName(i);
            name = atts.getValue(i);
            sb
                    .append("Name: ")
                    .append(name)
                    .append("\tValue: ")
                    .append(value)
                    .append("\n");
        }
        return sb;
    }

    @Override
    public void characters(char[] text, int start, int length)
            throws org.xml.sax.SAXException {
        if (charactersBuffer != null) {
            charactersBuffer.append(text, start, length);
        }
    }

    private void rememberCurrentState(String tag, Attributes attrs) {
        currentTag = tag;
        currentAttributes = attrs;
    }

    private void resetCurrentState() {
        currentTag = null;
        currentAttributes = null;
    }

    /**
     * @since 2.0
     */
    String convertClassNameFromV1_2(String name) {
        if (name == null) {
            return null;
        }

        // upgrade from v. <= 1.2
        if (name.startsWith(_1_2_PACKAGE_PREFIX)) {
            return _2_0_PACKAGE_PREFIX + name.substring(_1_2_PACKAGE_PREFIX.length());
        }

        return name;
    }

    abstract class StartClosure {

        abstract void execute(Attributes attributes) throws SAXException;
    }

    abstract class EndClosure {

        abstract void execute() throws SAXException;
    }
}
