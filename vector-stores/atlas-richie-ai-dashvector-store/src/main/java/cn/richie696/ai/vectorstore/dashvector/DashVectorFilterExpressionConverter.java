package cn.richie696.ai.vectorstore.dashvector;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Collectors;

/** Converts Spring AI filters to DashVector's SQL-WHERE expression syntax. */
public final class DashVectorFilterExpressionConverter implements FilterExpressionConverter {
    @Nonnull
    @Override
    public String convertExpression(@Nonnull Filter.Expression expression) {
        Assert.notNull(expression, "filter expression must not be null");
        return expression(expression);
    }

    private String expression(Filter.Expression value) {
        return switch (value.type()) {
            case AND -> binary(value, "AND");
            case OR -> binary(value, "OR");
            case EQ -> comparison(value, "=");
            case NE -> comparison(value, "!=");
            case GT -> comparison(value, ">");
            case GTE -> comparison(value, ">=");
            case LT -> comparison(value, "<");
            case LTE -> comparison(value, "<=");
            case IN -> membership(value, false);
            case NIN -> membership(value, true);
            case NOT -> "NOT (" + nested(value.left()) + ")";
            case ISNULL -> key(value.left()) + " IS NULL";
            case ISNOTNULL -> key(value.left()) + " IS NOT NULL";
        };
    }

    private String binary(Filter.Expression value, String operator) {
        return "(" + nested(value.left()) + " " + operator + " " + nested(value.right()) + ")";
    }

    private String comparison(Filter.Expression value, String operator) {
        return key(value.left()) + " " + operator + " " + literal(rawValue(value.right()));
    }

    private String membership(Filter.Expression value, boolean negated) {
        Object raw = rawValue(value.right());
        if (!(raw instanceof Collection<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("IN/NIN requires a non-empty collection value");
        }
        return key(value.left()) + (negated ? " NOT IN (" : " IN (")
                + values.stream().map(this::literal).collect(Collectors.joining(", ")) + ")";
    }

    private String nested(Filter.Operand operand) {
        if (operand instanceof Filter.Group group) return "(" + expression(group.content()) + ")";
        if (operand instanceof Filter.Expression expression) return this.expression(expression);
        throw new IllegalArgumentException("Expected nested filter expression, got: " + operand);
    }

    private String key(Filter.Operand operand) {
        if (!(operand instanceof Filter.Key key)) throw new IllegalArgumentException("Expected filter key");
        if (!key.key().matches("[A-Za-z_][A-Za-z0-9_.]*"))
            throw new IllegalArgumentException("Unsafe DashVector filter field: " + key.key());
        return key.key();
    }

    private Object rawValue(Filter.Operand operand) {
        if (operand instanceof Filter.Value value) return value.value();
        throw new IllegalArgumentException("Expected filter value");
    }

    private String literal(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }
}
