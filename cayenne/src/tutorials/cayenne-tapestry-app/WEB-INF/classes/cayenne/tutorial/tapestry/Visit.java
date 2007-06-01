package cayenne.tutorial.tapestry;

import java.io.Serializable;

import org.objectstyle.cayenne.access.DataContext;

/**
 * The artist application session object.  Each user
 * visit has its own cayenne data context.
 * 
 * @author Eric Schneider
 */

public class Visit implements Serializable {

	private DataContext dataContext;

	public Visit() {
		super();
		dataContext = DataContext.createDataContext();
	}

	public DataContext getDataContext() {
		return dataContext;
	}
}
