package org.apache.cayenne.modeler.event;

import java.util.EventListener;

/** 
 * Interface for classes that are interested in ProjectOnSave events. 
 */
public interface ProjectOnSaveListener extends EventListener{
	
	/** Changes made before saving project	 */
	public void beforeSaveChanges(ProjectOnSaveEvent e);
}
