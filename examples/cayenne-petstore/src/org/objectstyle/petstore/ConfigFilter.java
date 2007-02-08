package org.objectstyle.petstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.WebApplicationContextFilter;
import org.objectstyle.petstore.dao.AccountDao;
import org.objectstyle.petstore.dao.DaoManager;
import org.objectstyle.petstore.dao.ItemDao;
import org.objectstyle.petstore.dao.OrderDao;
import org.objectstyle.petstore.dao.PersistenceManager;
import org.objectstyle.petstore.dao.ProductDao;
import org.objectstyle.petstore.dao.impl.AccountCayenneDao;
import org.objectstyle.petstore.dao.impl.CayennePersistenceManager;
import org.objectstyle.petstore.dao.impl.ItemCayenneDao;
import org.objectstyle.petstore.dao.impl.OrderCayenneDao;
import org.objectstyle.petstore.dao.impl.ProductCayenneDao;

/**
 * A servlet filter that performs initial configuraion later makes sure a DataContext is
 * bound to each request thread. Startup configurion includes configuring DAOs, setting up
 * an embedded database, and sets up Cayenne.
 * 
 * @author Andrus Adamchik
 */
public class ConfigFilter extends WebApplicationContextFilter {

    static final String DERBY_SYSTEM_PROPERTY = "derby.system.home";

    public synchronized void init(FilterConfig config) throws ServletException {

        prepareDerby();

        // this will init Cayenne
        super.init(config);

        new DBSetupHelper(config.getServletContext(), Configuration
                .getSharedConfiguration()
                .getDomain()).setupDatabase();

        setupDao();
    }

    protected void prepareDerby() throws ServletException {
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("properties/derby.properties");
        if (in != null) {
            Properties props = new Properties();
            
            try {
                props.load(in);
            }
            catch (IOException e) {
                throw new ServletException("Error reading properties", e);
            }

            System.getProperties().putAll(props);
        }

        // setup Derby home to be Java TMP directory if not set explicitly

        if (System.getProperty(DERBY_SYSTEM_PROPERTY) == null) {
            System.setProperty(DERBY_SYSTEM_PROPERTY, System
                    .getProperty("java.io.tmpdir"));
        }

    }

    protected void setupDao() {

        // some prefer XML to configure stuff...IMO Java is just as good.

        Map daoMapping = new HashMap();
        daoMapping.put(AccountDao.class, new AccountCayenneDao());
        daoMapping.put(ItemDao.class, new ItemCayenneDao());
        daoMapping.put(OrderDao.class, new OrderCayenneDao());
        daoMapping.put(PersistenceManager.class, new CayennePersistenceManager());
        daoMapping.put(ProductDao.class, new ProductCayenneDao());

        DaoManager.setManager(new DaoManager(daoMapping));
    }
}
