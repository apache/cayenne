package org.apache.cayenne.modeler.init;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.modeler.init.platform.OSXPlatformInitializer;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;

public class OSXCayenneModelerModule implements Module {

	public void configure(Binder binder) {
		binder.bind(PlatformInitializer.class).to(OSXPlatformInitializer.class);
	}
}
