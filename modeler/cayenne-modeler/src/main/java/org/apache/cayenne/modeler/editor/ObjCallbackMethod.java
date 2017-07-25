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
package org.apache.cayenne.modeler.editor;

import java.io.Serializable;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class ObjCallbackMethod implements XMLSerializable,
	Serializable {
	
	private String name;
	private CallbackType callbackType;

	public ObjCallbackMethod(String name, CallbackType callbackType) {
		this.name = name;
		this.callbackType = callbackType;
	}

	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {

        encoder.start( encodeCallbackTypeForXML(callbackType)).attribute( name, getName()).end();
	}

	private String encodeCallbackTypeForXML(CallbackType type) {
		switch(type.getType()) {
			case POST_ADD : 
				return "post-add";
			case POST_LOAD :
				return "post-load";
			case POST_PERSIST :
				return "post-persist";
			case POST_REMOVE :
				return "post-remove";
			case POST_UPDATE :
				return "post-update";
			case PRE_PERSIST :
				return "pre-persist";
			case PRE_REMOVE :
				return "pre-remove";
			default:
				return "pre-update";
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;	
	}

	public CallbackType getCallbackType() {
		return callbackType;
	}

	public void setCallbackType(CallbackType callbackType) {
		this.callbackType = callbackType;
	}

}
