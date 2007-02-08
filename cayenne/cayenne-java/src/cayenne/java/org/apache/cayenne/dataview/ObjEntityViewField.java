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

import java.text.Format;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Descriptor for a single view field.
 * 
 * @since 1.1
 * @author Andriy Shapochka
 */
public class ObjEntityViewField {

    //ObjEntityView this field belongs to (owner of this field)
    private ObjEntityView owner;
    //This field maps whether to an ObjAttribute of the ObjEntity
    //the owner (view) corresponds to or an ObjRelationship with
    //the ObjEntity as a source in case this field is a lookup field
    private ObjAttribute objAttribute;

    private ObjRelationship objRelationship;

    //Field name - unique in the owner's context
    private String name = "";

    //field's data type, cannot be null
    private DataTypeEnum dataType = DataTypeEnum.UNKNOWN_TYPE;

    //field's calculation type, cannot be null
    private CalcTypeEnum calcType = CalcTypeEnum.NO_CALC_TYPE;

    //used for labeling (captioning) of this field in the GUI
    //in a JTable or on the input form, for example
    private String caption = "";

    //display format of the values for this field,
    //may differ from the edit format
    //for example, 1234567.5 dollars can be displayed as $1,234,567.50
    //but for edit it is more convenient to accept plain 1234567.5
    private Format displayFormat = null;

    //edit format of the values for this field
    private Format editFormat = null;

    //preferred index hints how the field should be placed in the ordered
    //list of all the fields of the owner.
    private int preferredIndex = -1;

    //its actual index in the list of the fields
    private int index = -1;

    //editability hint to the GUI
    private boolean editable = true;

    //visibility hint to the GUI
    private boolean visible = true;

    //the field may have a default value
    private Object defaultValue = null;

    //if the calc type is lookup then the field must refer to another field
    //to use its values
    private ObjEntityViewField lookupField = null;

    public ObjEntityViewField() {
    }

    public ObjEntityView getOwner() {
        return owner;
    }

    public DataView getRootOwner() {
        return getOwner().getOwner();
    }

    public int getIndex() {
        return index;
    }

    public int getPreferredIndex() {
        return preferredIndex;
    }

    public void setPreferredIndex(int preferredIndex) {
        Validate.isTrue(preferredIndex >= -1);
        this.preferredIndex = preferredIndex;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Validate.notNull(name);
        this.name = name;
    }

    public ObjAttribute getObjAttribute() {
        return objAttribute;
    }

    public void setObjAttribute(ObjAttribute objAttribute) {
        Validate.notNull(objAttribute);
        this.objAttribute = objAttribute;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    void setOwner(ObjEntityView owner) {
        this.owner = owner;
    }

    void setIndex(int index) {
        Validate.isTrue(owner == null || index >= 0);
        this.index = index;
    }

    public Class getJavaClass() {
        return getRootOwner().getDataTypeSpec().getJavaClass(dataType);
    }

    public Object getValue(DataObject obj) {
        Object rawValue = getRawValue(obj);
        return toValue(rawValue);
    }

    public Object toValue(Object rawValue) {
        Object v = null;
        if (isLookup()) {
            if (rawValue instanceof DataObject)
                v = lookupField.getValue((DataObject) rawValue);
            return v;
        }
        if (rawValue == null)
            return null;

        v = getRootOwner().getDataTypeSpec().toDataType(dataType, rawValue);
        return v;
    }

    public Object toRawValue(Object value) {
        if (value == null)
            return null;
        DataView rootOwner = getOwner().getOwner();
        if (isLookup()) {
            return rootOwner.getLookupCache().getDataObject(lookupField, value);
        }
        if (objAttribute == null)
            return null;

        String type = objAttribute.getType();
        Object v = null;
        try {
            Class untypedValueClass = Class.forName(type);
            v = rootOwner.getDataTypeSpec().fromDataType(
                    untypedValueClass,
                    dataType,
                    value);
        }
        catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return v;
    }

    public void setValue(DataObject obj, Object value) {
        Object rawValue = toRawValue(value);
        Object oldValue = getValue(obj);
        setRawValue(obj, rawValue);
        getRootOwner().fireFieldValueChangeEvent(this, obj, oldValue, value);
    }

    public String getFormattedValue(DataObject obj) {
        Object value = getRawValue(obj);
        String formattedValue = null;
        if (!isLookup()) {
            Format f = (displayFormat != null ? displayFormat : editFormat);
            if (f == null)
                formattedValue = ObjectUtils.toString(value);
            else {
                try {
                    formattedValue = f.format(value);
                }
                catch (Exception ex) {
                    formattedValue = "";
                }
            }
        }
        else {
            formattedValue = lookupField.getFormattedValue((DataObject) value);
        }
        return formattedValue;
    }

    public Object getRawValue(DataObject obj) {
        if (obj == null)
            return null;
        Object value = null;
        if (!isLookup() && objAttribute != null) {
            value = obj.readProperty(objAttribute.getName());
        }
        else if (isLookup() && objRelationship != null) {
            value = obj.readProperty(objRelationship.getName());
        }
        return value;
    }

    public void setRawValue(DataObject obj, Object value) {
        if (obj != null) {
            if (!isLookup() && objAttribute != null) {
                obj.writeProperty(objAttribute.getName(), value);
            }
            else if (isLookup() && objRelationship != null) {
                obj.setToOneTarget(
                        objRelationship.getName(),
                        (DataObject) value,
                        objRelationship.getReverseRelationship() != null);
            }
        }
    }

    public DataTypeEnum getDataType() {
        return dataType;
    }

    public void setDataType(DataTypeEnum dataType) {
        Validate.notNull(dataType);
        this.dataType = dataType;
    }

    public CalcTypeEnum getCalcType() {
        return calcType;
    }

    public void setCalcType(CalcTypeEnum calcType) {
        Validate.notNull(calcType);
        this.calcType = calcType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ObjRelationship getObjRelationship() {
        return objRelationship;
    }

    public void setObjRelationship(ObjRelationship objRelationship) {
        Validate.notNull(objRelationship);
        this.objRelationship = objRelationship;
    }

    public boolean isLookup() {
        return CalcTypeEnum.LOOKUP_TYPE.equals(calcType);
    }

    public Object[] getLookupValues() {
        if (!isLookup())
            return null;
        return getRootOwner().getLookupCache().getCachedValues(lookupField);
    }

    public ObjEntityViewField getLookupField() {
        return lookupField;
    }

    public void setLookupField(ObjEntityViewField lookupField) {
        this.lookupField = lookupField;
    }

    public Format getDisplayFormat() {
        if (displayFormat == null
                && isLookup()
                && lookupField != null
                && lookupField != this)
            return lookupField.getDisplayFormat();
        return displayFormat;
    }

    public void setDisplayFormat(Format displayFormat) {
        this.displayFormat = displayFormat;
    }

    public Format getEditFormat() {
        if (editFormat == null
                && isLookup()
                && lookupField != null
                && lookupField != this)
            return lookupField.getEditFormat();
        return editFormat;
    }

    public void setEditFormat(Format editFormat) {
        this.editFormat = editFormat;
    }

    public boolean isSameObjAttribute(ObjEntityViewField field) {
        if (objAttribute != null) {
            ObjAttribute fieldAttribute = field.getObjAttribute();
            if (fieldAttribute == null)
                return false;
            boolean same = (objAttribute.equals(fieldAttribute) || (objAttribute
                    .getEntity()
                    .equals(fieldAttribute.getEntity()) && objAttribute.getName().equals(
                    fieldAttribute.getName())));
            return same;
        }
        else if (isLookup()) {
            if (field.isLookup())
                return getLookupField().isSameObjAttribute(field.getLookupField());
            else
                return getLookupField().isSameObjAttribute(field);
        }
        return false;
    }
}
