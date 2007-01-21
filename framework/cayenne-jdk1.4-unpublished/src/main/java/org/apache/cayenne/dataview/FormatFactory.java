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
import java.text.*;

public class FormatFactory {
  private Map builders = new HashMap();

  public FormatFactory() {
    registerBuilder(DecimalFormat.class, new DecimalFormatBuilder());
    registerBuilder(NumberFormat.class, new DecimalFormatBuilder());
    registerBuilder(SimpleDateFormat.class, new SimpleDateFormatBuilder());
    registerBuilder(DateFormat.class, new SimpleDateFormatBuilder());
    registerBuilder(ChoiceFormat.class, new ChoiceFormatBuilder());
    registerBuilder(MessageFormat.class, new MessageFormatBuilder());
    registerBuilder(MapFormat.class, new MapFormatBuilder());
  }

  public Format createFormat(
      Class formatClass,
      Locale locale,
      Map parameters) {
    if (locale == null)
      locale = Locale.getDefault();
    Builder builder = getBuilder(formatClass);
    Format format = builder.create(locale, parameters);
    return format;
  }

  public Builder registerBuilder(Class formatClass, Builder builder) {
    return (Builder)builders.put(formatClass, builder);
  }

  public Builder unregisterBuilder(Class formatClass) {
    return (Builder)builders.remove(formatClass);
  }

  public Builder getBuilder(Class formatClass) {
    return (Builder)builders.get(formatClass);
  }

  public static interface Builder {
    Format create(Locale locale, Map parameters);
  }

  public static class DecimalFormatBuilder implements Builder {
    public Format create(Locale locale, Map parameters) {
      String pattern = (String)parameters.get("pattern");
      DecimalFormatSymbols sym = new DecimalFormatSymbols(locale);
      DecimalFormat format = new DecimalFormat();
      format.setDecimalFormatSymbols(sym);
      if (pattern != null)
        format.applyPattern(pattern);
      return format;
    }
  }

  public static class SimpleDateFormatBuilder implements Builder {
    public Format create(Locale locale, Map parameters) {
      String pattern = (String)parameters.get("pattern");
      DateFormatSymbols sym = new DateFormatSymbols(locale);
      SimpleDateFormat format = new SimpleDateFormat();
      format.setDateFormatSymbols(sym);
      if (pattern != null)
        format.applyPattern(pattern);
      return format;
    }
  }

  public static class ChoiceFormatBuilder implements Builder {
    public Format create(Locale locale, Map parameters) {
      String pattern = (String)parameters.get("pattern");
      ChoiceFormat format = new ChoiceFormat(pattern);
      return format;
    }
  }

  public static class MessageFormatBuilder implements Builder {
    public Format create(Locale locale, Map parameters) {
      String pattern = (String)parameters.get("pattern");
      MessageFormat format = new MessageFormat(pattern, locale);
      return format;
    }
  }

  public static class MapFormatBuilder implements Builder {
    public Format create(Locale locale, Map parameters) {
      MapFormat format = new MapFormat();
      String pattern = (String)parameters.get("pattern");
      String valueClassName = (String)parameters.get("value-class");
      String nullValueDesignation = (String)parameters.get("null-value");
      String entryDelimiter = (String)parameters.get("entry-delimiter");
      String valueFormatDelimiter = (String)parameters.get("value-delimiter");
      if (entryDelimiter != null && entryDelimiter.length() > 0)
        format.setEntryDelimiter(entryDelimiter.charAt(0));
      if (valueFormatDelimiter != null && valueFormatDelimiter.length() > 0)
        format.setValueFormatDelimiter(valueFormatDelimiter.charAt(0));
      if (nullValueDesignation != null)
        format.setNullValueDesignation(nullValueDesignation);
      Class valueClass;
      try {
        valueClass = Class.forName(valueClassName);
        format.applyPattern(pattern, valueClass);
        return format;
      }
      catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException("Value class " + valueClassName + " not found");
      }
    }
  }
}
