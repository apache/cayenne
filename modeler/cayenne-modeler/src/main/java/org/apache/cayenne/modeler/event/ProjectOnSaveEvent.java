package org.apache.cayenne.modeler.event;

import org.apache.cayenne.event.CayenneEvent;

/**
 * Triggered while project is saved.
 */
public class ProjectOnSaveEvent extends CayenneEvent{

	public ProjectOnSaveEvent(Object source) {
		super(source);
	}
}
