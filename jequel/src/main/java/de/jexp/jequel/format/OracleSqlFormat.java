package de.jexp.jequel.format;

import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.table.Table;

/**
 * @author mh14 @ jexp.de
 * @since 06.11.2007 02:39:07 (c) 2007 jexp.de
 */
public class OracleSqlFormat extends Sql92Format {
    protected String formatAlias(String expressionString, Aliased aliased) {
        if (!(aliased instanceof Table)) {
            return super.formatAlias(expressionString, aliased);
        }
        return expressionString + " " + aliased.getAlias();
    }
}
