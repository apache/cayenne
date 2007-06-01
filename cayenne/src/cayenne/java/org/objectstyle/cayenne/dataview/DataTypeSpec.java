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
package org.objectstyle.cayenne.dataview;

import java.util.*;
import java.lang.reflect.*;
import java.math.*;
import java.text.*;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.*;
import org.apache.commons.lang.time.*;

public class DataTypeSpec {
  protected Map dataTypeClassMap = new HashMap();
  protected Format dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd",
      Locale.US);
  protected Format dateTimeFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss",
      Locale.US);

  public DataTypeSpec() {
    dataTypeClassMap.put(DataTypeEnum.BOOLEAN_TYPE, Boolean.class);
    dataTypeClassMap.put(DataTypeEnum.DATE_TYPE, Date.class);
    dataTypeClassMap.put(DataTypeEnum.DATETIME_TYPE, Date.class);
    dataTypeClassMap.put(DataTypeEnum.DOUBLE_TYPE, Double.class);
    dataTypeClassMap.put(DataTypeEnum.INTEGER_TYPE, Long.class);
    dataTypeClassMap.put(DataTypeEnum.MONEY_TYPE, Double.class);
    dataTypeClassMap.put(DataTypeEnum.PERCENT_TYPE, Double.class);
    dataTypeClassMap.put(DataTypeEnum.STRING_TYPE, String.class);
    dataTypeClassMap.put(DataTypeEnum.OBJECT_TYPE, Object.class);
    dataTypeClassMap.put(DataTypeEnum.UNKNOWN_TYPE, null);
  }

  public DataTypeEnum getDataType(String dataType) {
    return DataTypeEnum.getEnum(dataType);
  }

  public DataTypeEnum getDataType(int dataType) {
    return DataTypeEnum.getEnum(dataType);
  }

  public Class getJavaClass(DataTypeEnum dataType) {
    return (Class)dataTypeClassMap.get(dataType);
  }

  public Object create(DataTypeEnum dataType) {
    Class clazz = getJavaClass(dataType);
    if (clazz != null) {
      try {
        Object value = clazz.newInstance();
        if (DataTypeEnum.DATE_TYPE.equals(dataType))
          value = DateUtils.truncate(value, Calendar.DATE);
        return value;
      }
      catch (InstantiationException ex) {
        return null;
      }catch (IllegalAccessException ex) {
        return null;
      }
    }
    return null;
  }

  public Object create(DataTypeEnum dataType, String argument) {
    Class clazz = getJavaClass(dataType);
    if (clazz != null) {
      try {
        if (DataTypeEnum.DATE_TYPE.equals(dataType)) {
          return dateFormat.parseObject(argument);
        } else if (DataTypeEnum.DATETIME_TYPE.equals(dataType)) {
          return dateTimeFormat.parseObject(argument);
        }
        Constructor strConstructor = clazz.getConstructor(new Class[] {String.class});
        return strConstructor.newInstance(new Object[] {argument});
      }
      catch (ParseException ex) {
        return null;
      }
      catch (NoSuchMethodException ex) {
        return null;
      }
      catch (InvocationTargetException ex) {
        return null;
      }
      catch (InstantiationException ex) {
        return null;
      }catch (IllegalAccessException ex) {
        return null;
      }
    }
    return null;
  }

  public Object create(DataTypeEnum dataType, Object[] arguments) {
    if (arguments == null || arguments.length == 0)
      return create(dataType);

    Class clazz = getJavaClass(dataType);
    if (clazz != null) {
      try {
        Class[] argTypes = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
          argTypes[i] = arguments[i].getClass();
        }

        Constructor strConstructor = clazz.getConstructor(new Class[] {String.class});
        return strConstructor.newInstance(arguments);
      }
      catch (NoSuchMethodException ex) {
        return null;
      }
      catch (InvocationTargetException ex) {
        return null;
      }
      catch (InstantiationException ex) {
        return null;
      }catch (IllegalAccessException ex) {
        return null;
      }
    }
    return null;
  }

  public Object toDataType(DataTypeEnum dataType, Object untypedValue) {
    Class dataTypeClass = getJavaClass(dataType);
    if (dataTypeClass == null ||
        untypedValue == null ||
        ClassUtils.isAssignable(untypedValue.getClass(), dataTypeClass)) {
      if (DataTypeEnum.DATE_TYPE.equals(dataType) &&
          Date.class.equals(dataTypeClass)) {
        return DateUtils.truncate(untypedValue, Calendar.DATE);
      }
      return untypedValue;
    }

    Object v = null;
    String strUntypedValue = null;
    boolean isStringUntypedValue;
    Number numUntypedValue = null;
    boolean isNumberUntypedValue;
    if (isStringUntypedValue = untypedValue instanceof String)
      strUntypedValue = (String)untypedValue;
    if (isNumberUntypedValue = untypedValue instanceof Number)
      numUntypedValue = (Number)untypedValue;

    switch (dataType.getValue()) {
      case DataTypeEnum.BOOLEAN_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = BooleanUtils.toBooleanObject(numUntypedValue.intValue());
        else if (isStringUntypedValue)
          v = BooleanUtils.toBooleanObject(strUntypedValue);
        break;
      case DataTypeEnum.INTEGER_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = new Integer(numUntypedValue.intValue());
        else if (isStringUntypedValue)
          v = NumberUtils.createInteger(strUntypedValue);
        break;
      case DataTypeEnum.DOUBLE_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = new Double(numUntypedValue.doubleValue());
        else if (isStringUntypedValue)
          v = NumberUtils.createDouble(strUntypedValue);
        break;
      case DataTypeEnum.STRING_TYPE_VALUE:
        v = ObjectUtils.toString(untypedValue);
        break;
      case DataTypeEnum.DATE_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = DateUtils.truncate(new Date(numUntypedValue.longValue()), Calendar.DATE);
        break;
      case DataTypeEnum.DATETIME_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = new Date(numUntypedValue.longValue());
        break;
      case DataTypeEnum.MONEY_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = new Double(numUntypedValue.doubleValue());
        else if (isStringUntypedValue)
          v = NumberUtils.createDouble(strUntypedValue);
        break;
      case DataTypeEnum.PERCENT_TYPE_VALUE:
        if (isNumberUntypedValue)
          v = new Double(numUntypedValue.doubleValue());
        else if (isStringUntypedValue)
          v = NumberUtils.createDouble(strUntypedValue);
        break;
    }
    return v;
  }

  public Object fromDataType(
      Class untypedValueClass,
      DataTypeEnum dataType,
      Object typedValue) {
    if (typedValue == null)
      return null;
    Class dataTypeClass = getJavaClass(dataType);
//    Validate.isTrue(typedValue.getClass().equals(dataTypeClass));

    if (untypedValueClass == null)
      return typedValue;

    if (ClassUtils.isAssignable(dataTypeClass, untypedValueClass))
      return typedValue;

    String strTypedValue = null;
    boolean isStringTypedValue;
    Number numTypedValue = null;
    boolean isNumberTypedValue;
    Boolean boolTypedValue = null;
    boolean isBooleanTypedValue;
    Date dateTypedValue = null;
    boolean isDateTypedValue;

    if (isStringTypedValue = typedValue instanceof String)
      strTypedValue = (String)typedValue;
    if (isNumberTypedValue = typedValue instanceof Number)
      numTypedValue = (Number)typedValue;
    if (isBooleanTypedValue = typedValue instanceof Boolean)
      boolTypedValue = (Boolean)typedValue;
    if (isDateTypedValue = typedValue instanceof Date)
      dateTypedValue = (Date)typedValue;

    Object v = null;
    if (String.class.equals(untypedValueClass)) {
      v = ObjectUtils.toString(typedValue);
    } else if (BigDecimal.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = NumberUtils.createBigDecimal(strTypedValue);
      else if (isNumberTypedValue)
        v = new BigDecimal(numTypedValue.doubleValue());
      else if (isBooleanTypedValue)
        v = new BigDecimal(
            BooleanUtils.toInteger(boolTypedValue.booleanValue()));
      else if (isDateTypedValue)
        v = new BigDecimal(dateTypedValue.getTime());
    } else if (Boolean.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = BooleanUtils.toBooleanObject(strTypedValue);
      else if (isNumberTypedValue)
        v = BooleanUtils.toBooleanObject(numTypedValue.intValue());
      else if (isDateTypedValue)
        v = BooleanUtils.toBooleanObject((int)dateTypedValue.getTime());
    } else if (Byte.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = Byte.valueOf(strTypedValue);
      else if (isNumberTypedValue)
        v = new Byte(numTypedValue.byteValue());
      else if (isBooleanTypedValue)
        v = new Byte(
            (byte)BooleanUtils.toInteger(boolTypedValue.booleanValue()));
      else if (isDateTypedValue)
        v = new Byte((byte)dateTypedValue.getTime());
    } else if (byte[].class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = strTypedValue.getBytes();
    } else if (Double.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = NumberUtils.createDouble(strTypedValue);
      else if (isNumberTypedValue)
        v = new Double(numTypedValue.doubleValue());
      else if (isBooleanTypedValue)
        v = new Double(
            BooleanUtils.toInteger(boolTypedValue.booleanValue()));
      else if (isDateTypedValue)
        v = new Double(dateTypedValue.getTime());
    } else if (Float.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = NumberUtils.createFloat(strTypedValue);
      else if (isNumberTypedValue)
        v = new Float(numTypedValue.floatValue());
      else if (isBooleanTypedValue)
        v = new Float(
            BooleanUtils.toInteger(boolTypedValue.booleanValue()));
      else if (isDateTypedValue)
        v = new Float(dateTypedValue.getTime());
    } else if (Integer.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = NumberUtils.createInteger(strTypedValue);
      else if (isNumberTypedValue)
        v = new Integer(numTypedValue.intValue());
      else if (isBooleanTypedValue)
        v = BooleanUtils.toIntegerObject(boolTypedValue.booleanValue());
      else if (isDateTypedValue)
        v = new Integer((int)dateTypedValue.getTime());
    } else if (Long.class.equals(untypedValueClass)) {
      if (isStringTypedValue)
        v = NumberUtils.createLong(strTypedValue);
      else if (isNumberTypedValue)
        v = new Long(numTypedValue.longValue());
      else if (isBooleanTypedValue)
        v = new Long(
            BooleanUtils.toInteger(boolTypedValue.booleanValue()));
      else if (isDateTypedValue)
        v = new Long(dateTypedValue.getTime());
    } else if (java.sql.Date.class.equals(untypedValueClass)) {
      if (isNumberTypedValue)
        v = new java.sql.Date(numTypedValue.longValue());
      else if (isDateTypedValue)
        v = new java.sql.Date(dateTypedValue.getTime());
    } else if (java.sql.Time.class.equals(untypedValueClass)) {
      if (isNumberTypedValue)
        v = new java.sql.Time(numTypedValue.longValue());
      else if (isDateTypedValue)
        v = new java.sql.Time(dateTypedValue.getTime());
    } else if (java.sql.Timestamp.class.equals(untypedValueClass)) {
      if (isNumberTypedValue)
        v = new java.sql.Timestamp(numTypedValue.longValue());
      else if (isDateTypedValue)
        v = new java.sql.Timestamp(dateTypedValue.getTime());
    } else if (Date.class.equals(untypedValueClass)) {
      if (isNumberTypedValue)
        v = new Date(numTypedValue.longValue());
    }
    return v;
  }
}
