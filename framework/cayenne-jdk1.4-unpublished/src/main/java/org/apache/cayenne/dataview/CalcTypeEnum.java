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

public class CalcTypeEnum extends ValuedEnum {
  public static final int NO_CALC_TYPE_VALUE = 1;
  public static final int CALC_TYPE_VALUE = 2;
  public static final int LOOKUP_TYPE_VALUE = 3;

  public static final String NO_CALC_TYPE_NAME = "nocalc";
  public static final String CALC_TYPE_NAME = "calc";
  public static final String LOOKUP_TYPE_NAME = "lookup";

  public static final CalcTypeEnum  NO_CALC_TYPE  = new CalcTypeEnum( NO_CALC_TYPE_NAME, NO_CALC_TYPE_VALUE );
  public static final CalcTypeEnum  CALC_TYPE  = new CalcTypeEnum( CALC_TYPE_NAME, CALC_TYPE_VALUE );
  public static final CalcTypeEnum  LOOKUP_TYPE  = new CalcTypeEnum( LOOKUP_TYPE_NAME, LOOKUP_TYPE_VALUE );

  protected CalcTypeEnum(String name, int value) {
    super(name, value);
  }

  public static CalcTypeEnum getEnum(String calcType) {
     return (CalcTypeEnum) getEnum(CalcTypeEnum.class, calcType);
   }

   public static CalcTypeEnum getEnum(int calcType) {
     return (CalcTypeEnum) getEnum(CalcTypeEnum.class, calcType);
   }

   public static Map getEnumMap() {
     return getEnumMap(CalcTypeEnum.class);
   }

   public static List getEnumList() {
     return getEnumList(CalcTypeEnum.class);
   }

   public static Iterator iterator() {
     return iterator(CalcTypeEnum.class);
   }

   public final Class getEnumClass() {
     return CalcTypeEnum.class;
   }
}
