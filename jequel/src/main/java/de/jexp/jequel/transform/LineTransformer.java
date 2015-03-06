package de.jexp.jequel.transform;

import de.jexp.jequel.expression.Expressions;
import de.jexp.jequel.literals.*;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.BaseTable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh14 @ jexp.de
 * @since 12.11.2007 01:58:35 (c) 2007 jexp.de
 */
public class LineTransformer {
    private final Class<?> schemaClass;
    private final Map<String, BaseTable> tables = new HashMap<String, BaseTable>();
    private final Set<String> columnNames = new HashSet<String>();

    public LineTransformer(Class<?> schemaClass) {
        this(schemaClass, false);
    }

    public LineTransformer(Class schemaClass, boolean useSchema) {
        this.schemaClass = schemaClass;
        if (useSchema) {
            loadNames();
        }
    }

    protected void loadNames() {
        for (Field field : schemaClass.getFields()) {
            if (BaseTable.class.isAssignableFrom(field.getType())) {
                try {
                    BaseTable table = (BaseTable) field.get(null);
                    tables.put(table.getName(), table);
                    for (de.jexp.jequel.table.Field<?> tableField : table.getFields().values()) {
                        columnNames.add(tableField.getName());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not access field " + field, e);
                }
            }
        }
    }

    public String transformKeywords(String line) {
        line = line.replaceAll("^(\\s*)\\b(select|SELECT)\\b", "$1Select(");
        for (SelectKeyword keyword : SelectKeyword.values()) {
            String keywordName = keyword.name();
            String literal = NameUtils.constantNameToLowerCaseLiteral(keywordName);
            String replacement = (keyword == SelectKeyword.SELECT) ? "subSelect(" : ")." + keywordName.toLowerCase() + "(";
            line = line.replaceAll("\\b(" + literal + "|" + nullSafeUpcase(literal) + ")\\b", replacement);
        }
        return line.replaceFirst("$", ")");
    }

    public String transformUnaryOperations(String line) {
        for (UnaryOperator unaryOperator : UnaryOperator.values()) {
            String operatorName = unaryOperator.name();
            line = line.replaceAll("\\b" + operatorName + "\\b", operatorName.toLowerCase());
        }
        return line;
    }

    public String transformBinaryOperations(String line) {
        for (SqlKeyword binaryOperator : Operator.values()) {
            String literal = binaryOperator.getSqlKeyword();
            String operatorName = binaryOperator.name().toLowerCase();
            String replacement;
            if (literal == null) {
                literal = "\\b" + NameUtils.constantNameToLowerCaseLiteral(binaryOperator.name()) + "\\b";
                if (binaryOperator == Operator.IN) {
                    replacement = "." + operatorName;
                } else {
                    replacement = "." + operatorName + "(";
                }

                if (binaryOperator == Operator.AND || binaryOperator == Operator.OR) {
                    replacement = ")" + replacement;
                }
            } else {
                literal = "([^" + literal + "])" + escapeRegexp(literal) + "([^" + literal + "])";
                replacement = "$1." + operatorName + "($2";
            }
            line = line.replaceAll(literal, replacement);
        }
        return line;
    }

    private String escapeRegexp(String literal) {
        return literal.replaceAll("([.*+$-])", "\\\\$1");
    }

    public String cleanUp(String line) {
        line = line.replaceAll("(\"?\\)|\\b\\w*\\b)\\.append\\(\"?", "");
        line = line.replaceAll("\"?\\);", "");
        return line;
    }

    private static Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b[a-z][a-z_]+\\.[a-z][a-z_]+\\b");
    private static Pattern SCHEMA_IDENTIFIER_PATTERN = Pattern.compile("\\b([a-z][a-z_]+)(?:\\.([a-z][a-z_]+))?\\b");

    public String upcaseIdentifiers(String line) {
        Matcher identiferMatch = IDENTIFIER_PATTERN.matcher(line);
        StringBuffer sb = new StringBuffer(line.length());
        while (identiferMatch.find()) {
            identiferMatch.appendReplacement(sb, nullSafeUpcase(identiferMatch.group()));
        }
        identiferMatch.appendTail(sb);
        return sb.toString();
    }

    public String upcaseIdentifiersForSchema(String line) {
        Matcher identiferMatch = SCHEMA_IDENTIFIER_PATTERN.matcher(line);
        StringBuffer sb = new StringBuffer(line.length());
        while (identiferMatch.find()) {
            String identifier = nullSafeUpcase(identiferMatch.group(1));
            String columnMatch = nullSafeUpcase(identiferMatch.group(2));
            String replacement = identiferMatch.group();
            if (containsTableOrColumn(identifier, columnMatch)) {
                replacement = nullSafeUpcase(replacement);
            }
            identiferMatch.appendReplacement(sb, replacement);
        }
        identiferMatch.appendTail(sb);
        return sb.toString();
    }

    protected static String nullSafeUpcase(String string) {
        return string == null ? null : string.toUpperCase();
    }

    protected boolean containsTableOrColumn(String identifier, String columnMatch) {
        return containsTable(identifier, columnMatch) || containsColumn(identifier, columnMatch);
    }

    protected boolean containsTable(String identifier, String columnMatch) {
        BaseTable table = tables.get(identifier);
        if (table == null) {
            return false;
        }
        return noColumnGroup(columnMatch) || table.getField(columnMatch) != null;
    }

    protected boolean noColumnGroup(String columnMatch) {
        return columnMatch == null || columnMatch.length() == 0;
    }

    protected boolean containsColumn(String identifier, String columnMatch) {
        return columnNames.contains(identifier) && noColumnGroup(columnMatch);
    }

    public String removeWhiteSpace(String line) {
        line = line.replaceAll("([()])\\s+", "$1");
        line = line.replaceAll("\\s\\s+", " ");
        line = line.replaceAll("\"?\\);", "");
        return line;
    }

    public String getImports() {
        return staticImport(Expressions.class) +
                staticImport(Sql.class) +
                staticImport(getSchemaClass());
    }

    private String staticImport(Class<?> type) {
        return "import static " + type.getName() + ".*;\n";
    }

    public boolean ignoreLine(String line) {
        if (line.trim().length() == 0) {
            return true;
        }

        return line.matches("^.*([{}@]|^\\s*(//|/\\*)|\\s\\b(if|for|while|final|return|public|protected|new|private|class|import|package|throw|catch|finally)\\b|\\*/).*$");
        // && !keywords in line
    }

    public String transformLine(String sql) {
        sql = cleanUp(sql);
        sql = removeWhiteSpace(sql);
        sql = upcaseIdentifiers(sql);
        sql = transformBinaryOperations(sql);
        sql = transformUnaryOperations(sql);
        sql = transformKeywords(sql);
        return sql;
    }

    public Class<?> getSchemaClass() {
        return schemaClass;
    }
}
