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

package org.apache.cayenne.dataview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.text.Format;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A root of the view configuration hierarchy. Contains a number of ObjEntityViews.
 * 
 * @since 1.1
 * @author Andriy Shapochka
 */
public class DataView {

    private Locale locale = Locale.US;
    
    //Data type definitions
    private DataTypeSpec dataTypeSpec = new DataTypeSpec();
    
    //Format definitions
    private FormatFactory formatFactory = new FormatFactory();
    
    //ObjEntity lookup
    private EntityResolver entityResolver;

    private Map objEntityViews = new TreeMap();
    private Set lookupEntityViewFields;
    private LookupCache lookupCache = new LookupCache();
    private EventDispatcher fieldValueChangeDispatcher;
    
    //used internally to resolve field lookup dependencies
    private Map lookupReferenceTable;

    public DataView() {
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        Validate.notNull(entityResolver);
        this.entityResolver = entityResolver;
    }

    public void load(File[] xmlSources) throws IOException {
        Validate.noNullElements(xmlSources);
        SAXBuilder builder = new SAXBuilder();
        Document[] documents = new Document[xmlSources.length];
        for (int i = 0; i < xmlSources.length; i++) {
            try {
                documents[i] = builder.build(xmlSources[i]);
            }
            catch (JDOMException ex) {
                ex.printStackTrace();
            }
        }
        load(documents);
    }

    public void load(URL[] xmlSources) throws IOException {
        Validate.noNullElements(xmlSources);
        SAXBuilder builder = new SAXBuilder();
        Document[] documents = new Document[xmlSources.length];
        for (int i = 0; i < xmlSources.length; i++) {
            try {
                documents[i] = builder.build(xmlSources[i]);
            }
            catch (JDOMException ex) {
                ex.printStackTrace();
            }
        }
        load(documents);
    }

    public void load(Reader[] xmlSources) throws IOException {
        Validate.noNullElements(xmlSources);
        SAXBuilder builder = new SAXBuilder();
        Document[] documents = new Document[xmlSources.length];
        for (int i = 0; i < xmlSources.length; i++) {
            try {
                documents[i] = builder.build(xmlSources[i]);
            }
            catch (JDOMException ex) {
                ex.printStackTrace();
            }
        }
        load(documents);
    }

    public void load(InputStream[] xmlSources) throws IOException {
        Validate.noNullElements(xmlSources);
        SAXBuilder builder = new SAXBuilder();
        Document[] documents = new Document[xmlSources.length];
        for (int i = 0; i < xmlSources.length; i++) {
            try {
                documents[i] = builder.build(xmlSources[i]);
            }
            catch (JDOMException ex) {
                ex.printStackTrace();
            }
        }
        load(documents);
    }

    public void load(Document[] views) {
        Validate.noNullElements(views);
        lookupReferenceTable = new HashMap();
        for (int i = 0; i < views.length; i++) {
            Element root = views[i].getRootElement();
            List entityViews = root.getChildren("obj-entity-view");
            for (Iterator j = entityViews.iterator(); j.hasNext();) {
                Element entityViewElement = (Element) j.next();
                loadEntityView(entityViewElement);
            }
        }
        resolveLookupReferences();
        lookupReferenceTable = null;
    }

    private void resolveLookupReferences() {
        lookupEntityViewFields = new HashSet();
        for (Iterator i = lookupReferenceTable.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            ObjEntityViewField field = (ObjEntityViewField) entry.getKey();
            String[] lookupDescriptor = (String[]) entry.getValue();
            ObjEntityView lookupEntityView = getObjEntityView(lookupDescriptor[0]);
            ObjEntityViewField lookupField = lookupEntityView
                    .getField(lookupDescriptor[1]);
            field.setLookupField(lookupField);
            lookupEntityViewFields.add(lookupField);
        }
    }

    private void loadEntityView(Element element) {
        String name = element.getAttributeValue("name");
        Validate.isTrue(name != null && !objEntityViews.containsKey(name));
        String objEntityName = element.getAttributeValue("obj-entity-name");
        Validate.notNull(objEntityName);
        ObjEntity objEntity = entityResolver.getObjEntity(objEntityName);
        ObjEntityView entityView = new ObjEntityView();
        entityView.setName(name);
        entityView.setObjEntity(objEntity);
        objEntityViews.put(name, entityView);
        entityView.setOwner(this);
        List fields = element.getChildren("field");
        for (Iterator i = fields.iterator(); i.hasNext();) {
            Element fieldElement = (Element) i.next();
            loadField(entityView, fieldElement);
        }
    }

    private void loadField(ObjEntityView entityView, Element element) {
        String name = element.getAttributeValue("name");
        ObjEntityViewField field = new ObjEntityViewField();
        field.setName(name);
        String prefIndex = element.getAttributeValue("pref-index");
        field.setPreferredIndex(NumberUtils.toInt(prefIndex, -1));
        entityView.insertField(field);

        String calcType = element.getAttributeValue("calc-type");
        Validate.notNull(calcType);
        CalcTypeEnum fieldCalcType = CalcTypeEnum.getEnum(calcType);
        Validate.isTrue(
                CalcTypeEnum.NO_CALC_TYPE.equals(fieldCalcType)
                        || CalcTypeEnum.LOOKUP_TYPE.equals(fieldCalcType),
                "Calc Type not supported yet: ",
                fieldCalcType);
        field.setCalcType(fieldCalcType);

        ObjEntity objEntity = entityView.getObjEntity();

        if (CalcTypeEnum.NO_CALC_TYPE.equals(fieldCalcType)) {
            String objAttributeName = element.getAttributeValue("obj-attribute-name");
            Validate.notNull(objAttributeName);
            ObjAttribute objAttribute = (ObjAttribute) objEntity
                    .getAttribute(objAttributeName);
            field.setObjAttribute(objAttribute);
        }
        else if (CalcTypeEnum.LOOKUP_TYPE.equals(fieldCalcType)) {
            String objRelationshipName = element
                    .getAttributeValue("obj-relationship-name");
            Validate.notNull(objRelationshipName);
            ObjRelationship objRelationship = (ObjRelationship) objEntity
                    .getRelationship(objRelationshipName);
            field.setObjRelationship(objRelationship);
            Element lookupElement = element.getChild("lookup");
            Validate.notNull(lookupElement);
            String lookupEntityView = lookupElement
                    .getAttributeValue("obj-entity-view-name");
            Validate.notNull(lookupEntityView);
            String lookupEntityField = lookupElement.getAttributeValue("field-name");
            Validate.notNull(lookupEntityField);
            String[] lookupDescriptor = new String[] {
                    lookupEntityView, lookupEntityField
            };
            lookupReferenceTable.put(field, lookupDescriptor);
        }

        String dataType = element.getAttributeValue("data-type");
        Validate.notNull(dataType);
        field.setDataType(dataTypeSpec.getDataType(dataType));

        String editable = element.getAttributeValue("editable");
        field.setEditable(BooleanUtils.toBoolean(editable));

        String visible = element.getAttributeValue("visible");
        field.setVisible(BooleanUtils.toBoolean(visible));

        Element captionElement = element.getChild("caption");
        if (captionElement != null)
            field.setCaption(StringUtils.stripToEmpty(captionElement.getText()));

        Element editFormatElement = element.getChild("edit-format");
        if (editFormatElement != null) {
            String formatClassName = editFormatElement.getAttributeValue("class");
            Validate.notNull(formatClassName);
            Class formatClass;
            try {
                formatClass = Class.forName(formatClassName);
                Map parameters = DataView.childrenToMap(editFormatElement);
                Format format = formatFactory.createFormat(
                        formatClass,
                        locale,
                        parameters);
                field.setEditFormat(format);
            }
            catch (ClassNotFoundException ex) {
            }
        }

        Element displayFormatElement = element.getChild("display-format");
        if (displayFormatElement != null) {
            String formatClassName = displayFormatElement.getAttributeValue("class");
            Validate.notNull(formatClassName);
            Class formatClass;
            try {
                formatClass = Class.forName(formatClassName);
                Map parameters = DataView.childrenToMap(displayFormatElement);
                Format format = formatFactory.createFormat(
                        formatClass,
                        locale,
                        parameters);
                field.setDisplayFormat(format);
            }
            catch (ClassNotFoundException ex) {
            }
        }

        Element defaultValueElement = element.getChild("default-value");
        if (defaultValueElement != null) {
            String defaultValueStr = StringUtils.stripToEmpty(defaultValueElement
                    .getText());
            Object defaultValue = dataTypeSpec.create(
                    field.getDataType(),
                    defaultValueStr);
            field.setDefaultValue(defaultValue);
        }
    }

    public Set getObjEntityViewNames() {
        return Collections.unmodifiableSet(objEntityViews.keySet());
    }

    public Collection getObjEntityViews() {
        return Collections.unmodifiableCollection(objEntityViews.values());
    }

    public ObjEntityView getObjEntityView(String viewName) {
        return (ObjEntityView) objEntityViews.get(viewName);
    }

    public LookupCache getLookupCache() {
        return lookupCache;
    }

    public void setLookupCache(LookupCache lookupCache) {
        Validate.notNull(lookupCache);
        this.lookupCache = lookupCache;
    }

    public Set getLookupObjEntityViewFields() {
        return Collections.unmodifiableSet(lookupEntityViewFields);
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public DataTypeSpec getDataTypeSpec() {
        return dataTypeSpec;
    }

    public void setDataTypeSpec(DataTypeSpec dataTypeSpec) {
        this.dataTypeSpec = dataTypeSpec;
    }

    public FormatFactory getFormatFactory() {
        return formatFactory;
    }

    public void setFormatFactory(FormatFactory formatFactory) {
        this.formatFactory = formatFactory;
    }

    private static Map childrenToMap(Element element) {
        List children = element.getChildren();
        if (children.isEmpty())
            return Collections.EMPTY_MAP;
        else {
            Map map = new HashMap(children.size());
            for (Iterator i = children.iterator(); i.hasNext();) {
                Element child = (Element) i.next();
                map.put(child.getName(), StringUtils.stripToNull(child.getText()));
            }
            return map;
        }
    }

    public void addFieldValueChangeListener(FieldValueChangeListener listener) {
        fieldValueChangeDispatcher = EventDispatcher.add(
                fieldValueChangeDispatcher,
                listener);
    }

    public void removeFieldValueChangeListener(FieldValueChangeListener listener) {
        fieldValueChangeDispatcher = EventDispatcher.remove(
                fieldValueChangeDispatcher,
                listener);
    }

    public void clearFieldValueChangeListeners() {
        if (fieldValueChangeDispatcher != null) {
            fieldValueChangeDispatcher.clear();
            fieldValueChangeDispatcher = null;
        }
    }

    public void fireFieldValueChangeEvent(
            ObjEntityViewField source,
            DataObject modifiedObject,
            Object oldValue,
            Object newValue) {
        if (fieldValueChangeDispatcher != null && source.getRootOwner() == this) {
            fieldValueChangeDispatcher.dispatch(new FieldValueChangeEvent(
                    source,
                    modifiedObject,
                    oldValue,
                    newValue));
        }
    }
}
