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

import java.util.ArrayList;
import java.util.EventListener;

public class EventDispatcher {
  protected transient ArrayList listeners = new ArrayList(1);

  public void dispatch(DispatchableEvent e) {
    EventListener[] listenersCopy = null;
    synchronized(this) {
      if (hasListeners())
        listenersCopy = (EventListener[])listeners.toArray(new EventListener[listeners.size()]);
    }

    if (listenersCopy != null) {
      int count = listenersCopy.length;
      for (int index = 0; index < count; ++index) {
        e.dispatch(listenersCopy[index]);
      }
    }
  }

  public synchronized boolean hasListeners() {
    return !listeners.isEmpty();
  }

  public synchronized int getListenerCount() {
    return listeners.size();
  }

  public synchronized int find(EventListener listener) {
    return listeners.indexOf(listener);
  }

  public synchronized void add(EventListener listener) {
    if (find(listener) < 0)
      listeners.add(listener);
  }

  public synchronized void remove(EventListener listener) {
    listeners.remove(listener);
  }

  public synchronized void clear() {
    listeners.clear();
  }

  public static EventDispatcher add(EventDispatcher dispatcher, EventListener listener) {
    if (dispatcher == null)
      dispatcher = new EventDispatcher();
    dispatcher.add(listener);
    return dispatcher;
  }

  public final static EventDispatcher remove(EventDispatcher dispatcher, EventListener listener) {
    if (dispatcher != null) {
      dispatcher.remove(listener);
      if (!dispatcher.hasListeners())
        dispatcher = null;
    }
    return dispatcher;
  }
}
