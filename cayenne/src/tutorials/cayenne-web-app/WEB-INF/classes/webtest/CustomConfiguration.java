package webtest;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.WebApplicationListener;

/** 
 * Special subclass of ServletConfiguration that enables 
 * logging of Cayenne queries and can also perform some 
 * custom tasks on servlet container startup.
 */
public class CustomConfiguration extends WebApplicationListener {

    public CustomConfiguration() {
        super();
        this.configureLogging();
    }

    protected void configureLogging() {
        // debug configuration
        Configuration.setLoggingLevel(Level.WARN);
    }
}
