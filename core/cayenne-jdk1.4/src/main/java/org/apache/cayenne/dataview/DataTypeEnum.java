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

import java.util.*;
import org.apache.commons.lang.enums.ValuedEnum;

public class DataTypeEnum extends ValuedEnum {
  public static final int UNKNOWN_TYPE_VALUE = -1;
  public static final int OBJECT_TYPE_VALUE = 0;
  public static final int STRING_TYPE_VALUE = 1;
  public static final int INTEGER_TYPE_VALUE = 2;
  public static final int DOUBLE_TYPE_VALUE = 3;
  public static final int MONEY_TYPE_VALUE = 4;
  public static final int PERCENT_TYPE_VALUE = 5;
  public static final int DATE_TYPE_VALUE = 6;
  public static final int DATETIME_TYPE_VALUE = 7;
  public static final int BOOLEAN_TYPE_VALUE = 9;
  public static final int DEFAULT_TYPE_VALUE = STRING_TYPE_VALUE;

  public static final String UNKNOWN_TYPE_NAME = "Unknown";
  public static final String OBJECT_TYPE_NAME = "Object";
  public static final String STRING_TYPE_NAME = "String";
  public static final String INTEGER_TYPE_NAME = "Integer";
  public static final String DOUBLE_TYPE_NAME = "Double";
  public static final String MONEY_TYPE_NAME = "Money";
  public static final String PERCENT_TYPE_NAME = "Percent";
  public static final String DATE_TYPE_NAME = "Date";
  public static final String DATETIME_TYPE_NAME = "Datetime";
  public static final String BOOLEAN_TYPE_NAME = "Boolean";
  public static final String DEFAULT_TYPE_NAME = STRING_TYPE_NAME;

  public static final DataTypeEnum  UNKNOWN_TYPE  = new DataTypeEnum( UNKNOWN_TYPE_NAME, UNKNOWN_TYPE_VALUE );
  public static final DataTypeEnum  OBJECT_TYPE  = new DataTypeEnum( OBJECT_TYPE_NAME, OBJECT_TYPE_VALUE );
  public static final DataTypeEnum  STRING_TYPE  = new DataTypeEnum( STRING_TYPE_NAME, STRING_TYPE_VALUE );
  public static final DataTypeEnum  INTEGER_TYPE  = new DataTypeEnum( INTEGER_TYPE_NAME, INTEGER_TYPE_VALUE );
  public static final DataTypeEnum  DOUBLE_TYPE  = new DataTypeEnum( DOUBLE_TYPE_NAME, DOUBLE_TYPE_VALUE );
  public static final DataTypeEnum  MONEY_TYPE  = new DataTypeEnum( MONEY_TYPE_NAME, MONEY_TYPE_VALUE );
  public static final DataTypeEnum  PERCENT_TYPE  = new DataTypeEnum( PERCENT_TYPE_NAME, PERCENT_TYPE_VALUE );
  public static final DataTypeEnum  DATE_TYPE  = new DataTypeEnum( DATE_TYPE_NAME, DATE_TYPE_VALUE );
  public static final DataTypeEnum  DATETIME_TYPE  = new DataTypeEnum( DATETIME_TYPE_NAME, DATETIME_TYPE_VALUE );
  public static final DataTypeEnum  BOOLEAN_TYPE  = new DataTypeEnum( BOOLEAN_TYPE_NAME, BOOLEAN_TYPE_VALUE );
  public static final DataTypeEnum  DEFAULT_TYPE  = STRING_TYPE;

  protected DataTypeEnum(String name, int value) {
    super(name, value);
  }

  public static DataTypeEnum getEnum(String dataType) {
     return (DataTypeEnum) getEnum(DataTypeEnum.class, dataType);
   }

   public static DataTypeEnum getEnum(int dataType) {
     return (DataTypeEnum) getEnum(DataTypeEnum.class, dataType);
   }

   public static Map getEnumMap() {
     return getEnumMap(DataTypeEnum.class);
   }

   public static List getEnumList() {
     return getEnumList(DataTypeEnum.class);
   }

   public static Iterator iterator() {
     return iterator(DataTypeEnum.class);
   }

   public final Class getEnumClass() {
     return DataTypeEnum.class;
   }

   public Class getJavaClass() {
     return null;
   }
}
