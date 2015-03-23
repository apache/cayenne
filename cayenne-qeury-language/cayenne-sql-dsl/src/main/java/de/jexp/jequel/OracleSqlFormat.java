package de.jexp.jequel;

import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.Expression;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class OracleSqlFormat extends Sql92Format {

    @Override
    public <E extends Expression> String visit(Aliased<E> expression) {
        String res = expression.getAliased().accept(expVisitor);
        if (isBlank(expression.getAlias())) {
            return res;
        }

        return res + " " + expression.getAlias();
    }
}
