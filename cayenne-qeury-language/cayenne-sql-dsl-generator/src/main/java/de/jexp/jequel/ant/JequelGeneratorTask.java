package de.jexp.jequel.ant;

import de.jexp.jequel.generator.data.SchemaMetaData;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Ant task for generating schema metadata
 * can be configured with database information
 * transfers this information to children if not set otherwise
 */
public class JequelGeneratorTask extends SchemaMetaDataProcessorDataSourceTask implements TaskContainer {
    private final Collection<Task> tasks = new LinkedList<Task>();
    private SchemaMetaData schemaMetaData = new SchemaMetaData();

    public void execute() throws BuildException {
        for (Task task : tasks) {
            initDatabaseTask(task);
            if (task instanceof SchemaMetaDataProcessorTask) {
                handleSchemaMetaDataProcessorTask((SchemaMetaDataProcessorTask) task);
            } else {
                task.perform();
            }
        }
    }

    private void handleSchemaMetaDataProcessorTask(SchemaMetaDataProcessorTask schemaMetaDataProcessorTask) {
        schemaMetaDataProcessorTask.setSchemaMetaData(schemaMetaData);
        schemaMetaDataProcessorTask.perform();
        schemaMetaData = schemaMetaDataProcessorTask.getSchemaMetaData();
    }


    public void addTask(Task task) {
        addToTasks(task);
    }

    private <T extends Task> T addToTasks(T task) {
        this.tasks.add(task);
        return task;
    }

    public void setSchema(String schema) {
        schemaMetaData = new SchemaMetaData(schema);
    }

    public SchemaCrawlerLoadSchemaMetaDataTask createCrawlerLoad() {
        return addToTasks(new SchemaCrawlerLoadSchemaMetaDataTask());
    }

    public ResourceBundleCommentsSchemaMetaDataProcessorTask createResourceBundleComments() {
        return addToTasks(new ResourceBundleCommentsSchemaMetaDataProcessorTask());
    }

    public OracleUserCommentsSchemaMetaDataTask createOracleComments() {
        return addToTasks(new OracleUserCommentsSchemaMetaDataTask());
    }

    public SchemaMetaDataProcessorTask createProcessor() {
        return addToTasks(new SchemaMetaDataProcessorTask());
    }

    public SchemaMetaDataProcessorDataSourceTask createDataSourceProcessor() {
        return addToTasks(new SchemaMetaDataProcessorDataSourceTask());
    }

    public SchemaMetaDataProcessorTask createDumpSchema() {
        return addToTasks(new DumpSchemaMetaDataProcessorTask());
    }

    public GeneratorSchemaMetaDataProcessorTask createGenerate() {
        return addToTasks(new GeneratorSchemaMetaDataProcessorTask());
    }

    public RelationShipMetaDataProcessorTask createRelationships() {
        return addToTasks(new RelationShipMetaDataProcessorTask());
    }

}

