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

import java.util.EventListener;

import org.apache.cayenne.DataObject;

public class FieldValueChangeEvent extends DispatchableEvent {
  private DataObject modifiedObject;
  private Object oldValue;
  private Object newValue;

  public FieldValueChangeEvent(
      ObjEntityViewField source,
      DataObject modifiedObject,
      Object oldValue,
      Object newValue) {
    super(source);
    this.modifiedObject = modifiedObject;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }
  public void dispatch(EventListener listener) {
    ((FieldValueChangeListener)listener).fieldValueChanged(this);
  }
  public ObjEntityViewField getField() {
    return (ObjEntityViewField)getSource();
  }
  public DataObject getModifiedObject() {
    return modifiedObject;
  }
  public Object getNewValue() {
    return newValue;
  }
  public Object getOldValue() {
    return oldValue;
  }
}
