package org.objectstyle.cayenne.conf;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ConfigLoaderTst extends CayenneTestCase {
    private static Logger logObj = Logger.getLogger(ConfigLoaderTst.class);

    public void testLoadDomains() throws Exception {
        Iterator it = new ConfigLoaderSimpleSuite().getCases().iterator();
        while (it.hasNext()) {
            ConfigLoaderCase aCase = (ConfigLoaderCase) it.next();
            logObj.debug("Starting Case: " + aCase);

            Configuration conf = new EmptyConfiguration();
            ConfigLoaderDelegate delegate = conf.getLoaderDelegate();
            ConfigLoader helper = new ConfigLoader(delegate);
            aCase.test(helper);
        }
    }

}
