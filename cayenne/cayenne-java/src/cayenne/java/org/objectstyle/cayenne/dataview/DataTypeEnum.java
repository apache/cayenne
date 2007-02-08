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
package org.objectstyle.cayenne.dataview;

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