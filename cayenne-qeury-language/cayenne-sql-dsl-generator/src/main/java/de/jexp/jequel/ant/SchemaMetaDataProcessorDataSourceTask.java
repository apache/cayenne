package de.jexp.jequel.ant;

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

public class SchemaMetaDataProcessorDataSourceTask<T extends SchemaMetaDataProcessor> extends SchemaMetaDataProcessorTask<T> {
    private DriverManagerDataSource dataSource = new DriverManagerDataSource();


    protected T createProcessor(Class<T> metaDataProcessorClass, SchemaMetaData schemaMetaData) {
        T schemaMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        setDataSourceFor(schemaMetaDataProcessor);
        return schemaMetaDataProcessor;
    }

    protected void setDataSourceFor(T schemaMetaDataProcessor) {
        Class<? extends SchemaMetaDataProcessor> metaDataProcessorClass = schemaMetaDataProcessor.getClass();
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

    public void setPassword(String password) {
        dataSource.setPassword(password);
    }

    public void setDriver(String driverClassName) {
        ClassLoader loader = getClass().getClassLoader();
        // BugFix for Springs ClassUtils.forName()
        Thread.currentThread().setContextClassLoader(loader);
        dataSource.setDriverClassName(driverClassName);
    }

    public void setUrl(String url) {
        dataSource.setUrl(url);
    }

    public void setUserid(String userId) {
        dataSource.setUsername(userId);
    }

    protected DriverManagerDataSource getDataSource() {
        return dataSource;
    }

    public void fetchProperties(DriverManagerDataSource dataSource) {
        if (this.dataSource.getDriverClassName() == null)
            this.dataSource.setDriverClassName(dataSource.getDriverClassName());
        if (this.dataSource.getUrl() == null)
            this.dataSource.setUrl(dataSource.getUrl());
        if (this.dataSource.getUsername() == null)
            this.dataSource.setUsername(dataSource.getUsername());
        if (this.dataSource.getPassword() == null)
            this.dataSource.setPassword(dataSource.getPassword());
    }

    protected void initDatabaseTask(Task task) {
        // printTaskInfo(task);
        if (task instanceof SchemaMetaDataProcessorDataSourceTask) {
            ((SchemaMetaDataProcessorDataSourceTask) task).fetchProperties(getDataSource());
        } else if (task instanceof JDBCTask) {
            handleJdbcTask((JDBCTask) task);
        } else if (task.getTaskType().equals("sql")) {
            handleSqlType(task);
        }

    }

    protected void printTaskInfo(Task task) {
        System.out.println("task = " + task.getClass() + " type " + task.getTaskType() + " name " + task.getTaskName());
    }

    protected void handleSqlType(Task task) {
        if (task instanceof UnknownElement) {
            UnknownElement unknownElement = (UnknownElement) task;
            RuntimeConfigurable wrapper = unknownElement.getRuntimeConfigurableWrapper();
            Map attributeMap = wrapper.getAttributeMap();
            DriverManagerDataSource dataSource = getDataSource();
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
    private void loadDriverAntHack(UnknownElement sqlTask) {
        RuntimeConfigurable wrapper = sqlTask.getRuntimeConfigurableWrapper();
        String driverClassName = (String) wrapper.getAttributeMap().get("driver");
        if (driverClassName == null) return;
        Class sqlTaskClass = (Class) sqlTask.getProject().getTaskDefinitions().get(sqlTask.getTaskType());
        try {

            Method getLoaderMapMethod = getMethodWithName(sqlTaskClass, "getLoaderMap");
            if (getLoaderMapMethod == null) return;
            getLoaderMapMethod.setAccessible(true);
            //noinspection unchecked
            Map<String, ClassLoader> loaderMap = (Map<String, ClassLoader>) getLoaderMapMethod.invoke(null);
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

    private Method getMethodWithName(Class type, String methodName, Class... argTypes) {
        try {
            return type.getDeclaredMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            if (type.getSuperclass() != null)
                return getMethodWithName(type.getSuperclass(), methodName, argTypes);
            return null;
        }
    }

    protected void handleJdbcTask(JDBCTask jdbcTask) {
        DriverManagerDataSource dataSource = getDataSource();
        if (jdbcTask.getUrl() == null) {
            jdbcTask.setDriver(dataSource.getDriverClassName());
            jdbcTask.setUrl(dataSource.getUrl());
        }
        if (jdbcTask.getUserId() == null) jdbcTask.setUserid(dataSource.getUsername());
        if (jdbcTask.getPassword() == null) jdbcTask.setPassword(dataSource.getPassword());
    }
}
