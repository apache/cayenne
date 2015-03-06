package org.apache.tools.ant.taskdefs.optional.jequel;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.SchemaMetaDataProcessor;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.JDBCTask;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 21:27:31 (c) 2007 jexp.de
 */
public class SchemaMetaDataProcessorDataSourceTask<T extends SchemaMetaDataProcessor> extends SchemaMetaDataProcessorTask<T> {
    private final DriverManagerDataSource dataSource = new DriverManagerDataSource();


    protected T createProcessor(final Class<T> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        final T schemaMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        setDataSourceFor(schemaMetaDataProcessor);
        return schemaMetaDataProcessor;
    }

    protected void setDataSourceFor(final T schemaMetaDataProcessor) {
        final Class<? extends SchemaMetaDataProcessor> metaDataProcessorClass = schemaMetaDataProcessor.getClass();
        Method setDataSourceMethod = ClassUtils.getMethodIfAvailable(metaDataProcessorClass, "setDataSource", new Class[]{DataSource.class});
        if (setDataSourceMethod == null)
            setDataSourceMethod = ClassUtils.getMethodIfAvailable(metaDataProcessorClass, "setDatasource", new Class[]{DataSource.class});
        if (setDataSourceMethod != null) {
            try {
                setDataSourceMethod.invoke(schemaMetaDataProcessor, getDataSource());
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing setDataSource on " + schemaMetaDataProcessor, e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Error accessing setDataSource on " + schemaMetaDataProcessor, e);
            }
        }
    }

    public void setPassword(final String password) {
        dataSource.setPassword(password);
    }

    public void setDriver(final String driverClassName) {
        final ClassLoader loader = getClass().getClassLoader();
        // BugFix for Springs ClassUtils.forName()
        Thread.currentThread().setContextClassLoader(loader);
        dataSource.setDriverClassName(driverClassName);
    }

    public void setUrl(final String url) {
        dataSource.setUrl(url);
    }

    public void setUserid(final String userId) {
        dataSource.setUsername(userId);
    }

    protected DriverManagerDataSource getDataSource() {
        return dataSource;
    }

    public void fetchProperties(final DriverManagerDataSource dataSource) {
        if (this.dataSource.getDriverClassName() == null)
            this.dataSource.setDriverClassName(dataSource.getDriverClassName());
        if (this.dataSource.getUrl() == null)
            this.dataSource.setUrl(dataSource.getUrl());
        if (this.dataSource.getUsername() == null)
            this.dataSource.setUsername(dataSource.getUsername());
        if (this.dataSource.getPassword() == null)
            this.dataSource.setPassword(dataSource.getPassword());
    }

    protected void initDatabaseTask(final Task task) {
        // printTaskInfo(task);
        if (task instanceof SchemaMetaDataProcessorDataSourceTask) {
            ((SchemaMetaDataProcessorDataSourceTask) task).fetchProperties(getDataSource());
        } else if (task instanceof JDBCTask) {
            handleJdbcTask((JDBCTask) task);
        } else if (task.getTaskType().equals("sql")) {
            handleSqlType(task);
        }

    }

    protected void printTaskInfo(final Task task) {
        System.out.println("task = " + task.getClass() + " type " + task.getTaskType() + " name " + task.getTaskName());
    }

    protected void handleSqlType(final Task task) {
        if (task instanceof UnknownElement) {
            final UnknownElement unknownElement = (UnknownElement) task;
            final RuntimeConfigurable wrapper = unknownElement.getRuntimeConfigurableWrapper();
            final Map attributeMap = wrapper.getAttributeMap();
            final DriverManagerDataSource dataSource = getDataSource();
            if (!attributeMap.containsKey("driver")) {
                wrapper.setAttribute("driver", dataSource.getDriverClassName());
            }
            if (!attributeMap.containsKey("url")) {
                wrapper.setAttribute("url", dataSource.getUrl());
            }
            if (!attributeMap.containsKey("userid")) {
                wrapper.setAttribute("userid", dataSource.getUsername());
            }
            if (!attributeMap.containsKey("password")) {
                wrapper.setAttribute("password", dataSource.getPassword());
            }
            loadDriverAntHack(unknownElement);
        }
    }

    /**
     * very bad hack, sets the global driver->classloader cache of JdbcTask
     * as it doesn't get the Ant Classloader that was used to create this class but the one at startup time which does
     * not contain the classpath of our driver
     *
     * @param sqlTask Unknown Element of SQL Task
     */
    private void loadDriverAntHack(final UnknownElement sqlTask) {
        final RuntimeConfigurable wrapper = sqlTask.getRuntimeConfigurableWrapper();
        final String driverClassName = (String) wrapper.getAttributeMap().get("driver");
        if (driverClassName == null) return;
        final Class sqlTaskClass = (Class) sqlTask.getProject().getTaskDefinitions().get(sqlTask.getTaskType());
        try {

            final Method getLoaderMapMethod = getMethodWithName(sqlTaskClass, "getLoaderMap");
            if (getLoaderMapMethod == null) return;
            getLoaderMapMethod.setAccessible(true);
            //noinspection unchecked
            final Map<String, ClassLoader> loaderMap = (Map<String, ClassLoader>) getLoaderMapMethod.invoke(null);
            synchronized (loaderMap) {
                if (!loaderMap.containsKey(driverClassName)) {
                    loaderMap.put(driverClassName, getClass().getClassLoader());
                }
                wrapper.setAttribute("classpath", "");
            }
        } catch (Exception e) {
            System.err.println("Error setting class loader on sqlTask");
            e.printStackTrace(System.err);
        }
    }

    private Method getMethodWithName(final Class type, final String methodName, final Class... argTypes) {
        try {
            return type.getDeclaredMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            if (type.getSuperclass() != null)
                return getMethodWithName(type.getSuperclass(), methodName, argTypes);
            return null;
        }
    }

    protected void handleJdbcTask(final JDBCTask jdbcTask) {
        final DriverManagerDataSource dataSource = getDataSource();
        if (jdbcTask.getUrl() == null) {
            jdbcTask.setDriver(dataSource.getDriverClassName());
            jdbcTask.setUrl(dataSource.getUrl());
        }
        if (jdbcTask.getUserId() == null) jdbcTask.setUserid(dataSource.getUsername());
        if (jdbcTask.getPassword() == null) jdbcTask.setPassword(dataSource.getPassword());
    }
}
