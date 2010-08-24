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

package org.apache.cayenne.modeler.event;

import junit.framework.TestCase;

import org.apache.cayenne.map.event.MapEvent;

/**
 */
public class ModelerEventTest extends TestCase {

    public void testConstructor1() throws Exception {
    	Object src = new Object();
    	MapEvent e = new TestMapEvent(src);
    	assertSame(src, e.getSource());
    }
    
    public void testId() throws Exception  {
    	MapEvent e = new TestMapEvent(new Object());
    	assertEquals(MapEvent.CHANGE, e.getId());
    	
    	e.setId(MapEvent.ADD);
    	assertEquals(MapEvent.ADD, e.getId());
    }
    
    class TestMapEvent extends MapEvent {
        public TestMapEvent(Object source) {
            super(source);
            // TODO Auto-generated constructor stub
        }

        public TestMapEvent(Object source, String oldName) {
            super(source, oldName);
            // TODO Auto-generated constructor stub
        }

    	public String getNewName() {
    		return "";
    	}
    }
}

