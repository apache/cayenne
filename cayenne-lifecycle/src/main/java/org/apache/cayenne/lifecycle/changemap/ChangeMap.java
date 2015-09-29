package org.apache.cayenne.lifecycle.changemap;

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.ObjectId;

/**
 * Represents a map of changes for a graph of persistent objects.
 * 
 * @since 4.0
 */
public interface ChangeMap {

	/**
	 * Returns a map of changes. Note the same change sometimes can be present
	 * in the map twice. If ObjectId of an object has changed during the commit,
	 * the change will be accessible by both pre-commit and post-commit ID. To
	 * get unique changes, call {@link #getUniqueChanges()}.
	 */
	Map<ObjectId, ? extends ObjectChange> getChanges();

	Collection<? extends ObjectChange> getUniqueChanges();
}
