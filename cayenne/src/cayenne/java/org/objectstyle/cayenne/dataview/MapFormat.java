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

import java.text.*;
import java.util.*;
import java.lang.reflect.*;

public class MapFormat extends Format {
  private LinkedHashMap formatMap;
  private Object[] values;
  private String[] formats;
  private String entryDelimiter = "|";
  private String valueFormatDelimiter = "#";
  private String nullFormat = "";
  private String nullValueDesignation = "null";

  public MapFormat() {
  }

  public MapFormat(String pattern, Class valueClass) {
    applyPattern(pattern, valueClass);
  }

  public MapFormat(Object[] values, String[] formats) {
    setMap(values, formats);
  }

  public void setEntryDelimiter(char delimiter) {
    entryDelimiter = String.valueOf(delimiter);
  }

  public char getEntryDelimiter() {
    return entryDelimiter.charAt(0);
  }

  public void setValueFormatDelimiter(char delimiter) {
    valueFormatDelimiter = String.valueOf(delimiter);
  }

  public char getValueFormatDelimiter() {
    return valueFormatDelimiter.charAt(0);
  }

  public void setNullValueDesignation(String nullValueDesignation) {
    this.nullValueDesignation = nullValueDesignation;
  }

  public String getNullValueDesignation() {
    return nullValueDesignation;
  }

  public String getNullFormat() {
    return nullFormat;
  }

  public void setMap(Object[] values, String[] formats) {
    this.values = new Object[values.length];
    this.formats = new String[formats.length];
    System.arraycopy(values, 0, this.values, 0, formats.length);
    System.arraycopy(formats, 0, this.formats, 0, formats.length);
    formatMap = new LinkedHashMap(this.values.length + 1);
    for (int i = 0; i < this.values.length; i++) {
      if (this.formats[i] == null)
        throw new NullPointerException("format cannot be null: " + values[i]);
      if (this.values[i] == null)
        nullFormat = this.formats[i];
      else
        formatMap.put(this.values[i], this.formats[i]);
    }
  }

  public Object[] getValues() {
    return values;
  }

  public String[] getFormats() {
    return formats;
  }

  public void applyPattern(String pattern) {
    applyPattern(pattern, String.class);
  }

  public void applyPattern(String pattern, Class valueClass) {
    formatMap = new LinkedHashMap();
    Constructor stringConstructor;
    try {
      stringConstructor =
          valueClass.getConstructor(new Class[] {String.class});
    } catch (Exception ex) {
      throw new IllegalArgumentException(valueClass + " has no String cunstructor");
    }
    StringTokenizer parser = new StringTokenizer(pattern, entryDelimiter);
    ArrayList valueList = new ArrayList();
    ArrayList formatList = new ArrayList();
    while (parser.hasMoreTokens()) {
      String pair = parser.nextToken().trim();
      int delimIndex = pair.indexOf(valueFormatDelimiter);
      Object value;
      String format = "";
      try {
        String valueStr;
        if (delimIndex < 0 || delimIndex >= pair.length() - 1) {
          valueStr = pair;
        } else {
          valueStr = pair.substring(0, delimIndex);
          format = pair.substring(delimIndex + 1);
        }
        if (nullValueDesignation.equals(valueStr)) {
          nullFormat = format;
          valueList.add(null);
          formatList.add(nullFormat);
        } else {
          value = stringConstructor.newInstance(new Object[] {valueStr});
          formatMap.put(value, format);
          valueList.add(value);
          formatList.add(format);
        }
      }
      catch (InstantiationException ex) {
        throw new IllegalArgumentException(valueClass + " " + ex);
      }catch (IllegalAccessException ex) {
        throw new IllegalArgumentException(valueClass + " " + ex);
      }catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(valueClass + " " + ex);
      }catch (InvocationTargetException ex) {
        throw new IllegalArgumentException(pattern + " incorrect pattern: " + pair);
      }
    }
    values = valueList.toArray();
    formats = (String[])formatList.toArray(new String[formatList.size()]);
  }

  public Object parseObject(String text, ParsePosition status) {
    int start = status.getIndex();
    int furthest = start;
    Object bestObject = null;
    Object tempObject = null;
    for (Iterator i = formatMap.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry)i.next();
      String tempString = (String)entry.getValue();
      if (text.regionMatches(start, tempString, 0, tempString.length())) {
        status.setIndex(start + tempString.length());
        tempObject = entry.getKey();
        if (status.getIndex() > furthest) {
          furthest = status.getIndex();
          bestObject = tempObject;
          if (furthest == text.length()) break;
        }
      }
    }
    if (nullFormat != null &&
        text.regionMatches(start, nullFormat, 0, nullFormat.length())) {
      status.setIndex(start + nullFormat.length());
      if (status.getIndex() > furthest) {
        furthest = status.getIndex();
        bestObject = null;
      }
    }
    status.setIndex(furthest);
    if (status.getIndex() == start) {
      status.setErrorIndex(furthest);
    }
    return bestObject;
  }

  public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
    if (obj == null) {
      if (nullFormat == null)
        throw new NullPointerException("object to format cannot be null");
      else return toAppendTo.append(nullFormat);
    }
    String formatStr = (String)formatMap.get(obj);
    if (formatStr == null)
      throw new IllegalArgumentException("cannot format the object " + obj);
    toAppendTo.append(formatStr);
    return toAppendTo;
  }

  public String toPattern() {
    StringBuffer pattern = new StringBuffer();
    boolean notFirst = false;
    for (Iterator i = formatMap.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry)i.next();
      if (notFirst) {
        pattern.append(entryDelimiter);
      } else notFirst = true;
      pattern.append(entry.getKey()).append(valueFormatDelimiter).append(entry.getValue());
    }
    if (nullValueDesignation != null && nullFormat != null) {
      if (notFirst)
        pattern.append(entryDelimiter);
      pattern.append(nullValueDesignation).append(valueFormatDelimiter).append(nullFormat);
    }
    return pattern.toString();
  }
}