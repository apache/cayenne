package org.apache.cayenne.modeler.editor;

import java.io.Serializable;

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
	public void encodeAsXML(XMLEncoder encoder) {

        encoder.print("<" + encodeCallbackTypeForXML(callbackType));
        encoder.print(" name=\"" + getName());

        encoder.println("\"/>");
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
