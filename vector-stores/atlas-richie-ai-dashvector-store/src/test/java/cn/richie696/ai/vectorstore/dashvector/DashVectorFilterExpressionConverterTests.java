package cn.richie696.ai.vectorstore.dashvector;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashVectorFilterExpressionConverterTests {
    private final DashVectorFilterExpressionConverter converter = new DashVectorFilterExpressionConverter();

    @Test
    void convertsLogicalComparisonAndMembershipExpressions() {
        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.AND,
                new Filter.Expression(Filter.ExpressionType.EQ,
                        new Filter.Key("tenant"), new Filter.Value("richie's")),
                new Filter.Expression(Filter.ExpressionType.IN,
                        new Filter.Key("region"), new Filter.Value(List.of("cn", "sg"))));

        assertThat(converter.convertExpression(expression))
                .isEqualTo("(tenant = 'richie''s' AND region IN ('cn', 'sg'))");
    }

    @Test
    void rejectsUnsafeFieldNames() {
        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("tenant; drop collection"), new Filter.Value("acme"));

        assertThatThrownBy(() -> converter.convertExpression(expression))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe DashVector filter field");
    }
}
