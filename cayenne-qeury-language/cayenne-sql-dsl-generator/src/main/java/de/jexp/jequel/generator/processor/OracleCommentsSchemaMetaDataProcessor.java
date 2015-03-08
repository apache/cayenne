/**
 * START_TAG_PLACEHOLDER FOREGROUND_PROCESSING RUNS_AS_JAVA_ON_CLIENT RUNS_AS_JAVA_ON_SERVER FINISH_TAG_PLACEHOLDER
 */
package de.jexp.jequel.generator.processor;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.SchemaMetaDataProcessor;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import static de.jexp.jequel.sql.Sql.*;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class OracleCommentsSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    @SuppressWarnings({"NonConstantFieldWithUpperCaseName"})
    public static class USER_COL_COMMENTS extends BaseTable<USER_COL_COMMENTS> {
        public Field<String> TABLE_NAME = string();
        public Field<String> COLUMN_NAME = string();
        public Field<String> COMMENTS = string();

        {
            initFields();
        }
    }

    public static USER_COL_COMMENTS UCC = new USER_COL_COMMENTS();

    private DataSource dataSource;

    public OracleCommentsSchemaMetaDataProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void processMetaData() {
        if (dataSource == null) {
            throw new IllegalStateException("No Datasource set");
        }

        Collection<String> tableNames = getSchemaMetaData().getTableNames();
        String commentSelect =
                Select(UCC.TABLE_NAME, UCC.COLUMN_NAME, UCC.COMMENTS)
                        .from(UCC)
                        .where(UCC.TABLE_NAME.in(param(tableNames)).and(UCC.COMMENTS.isNot(NULL))).toString();

        loadComments(commentSelect, tableNames);
    }

    protected void loadComments(String commentSelect, Collection<String> tableNames) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = getDataSource().getConnection();
            preparedStatement = connection.prepareStatement(commentSelect);
            int index = 1;
            for (String tableName : tableNames) {
                preparedStatement.setString(index++, tableName);
            }
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                processRow(resultSet);
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Error loading comment metadata for tables: " + tableNames, sqle);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) { // ignore
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) { // ignore
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) { // ignore
                }
            }
        }
    }

    public void processRow(ResultSet rs) throws SQLException {
        String tableName = rs.getString("TABLE_NAME");
        TableMetaData tableMetaData = getSchemaMetaData().getTable(tableName);
        if (tableMetaData != null) {
            String columnName = rs.getString("COLUMN_NAME");
            TableMetaDataColumn tableMetaDataColumn = tableMetaData.getColumn(columnName);
            if (tableMetaDataColumn != null) {
                tableMetaDataColumn.setRemark(rs.getString("COMMENTS"));
            }
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
