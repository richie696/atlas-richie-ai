package cn.richie696.ai.vectorstore.tencentvectordb;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TencentVectorDbFilterExpressionConverterTests {
    private final TencentVectorDbFilterExpressionConverter converter = new TencentVectorDbFilterExpressionConverter();

    @Test
    void convertsLogicalComparisonAndMembershipExpressions() {
        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.AND,
                new Filter.Expression(Filter.ExpressionType.GTE,
                        new Filter.Key("score"), new Filter.Value(80)),
                new Filter.Expression(Filter.ExpressionType.NIN,
                        new Filter.Key("region"), new Filter.Value(List.of("cn", "sg"))));

        assertThat(converter.convertExpression(expression))
                .isEqualTo("(score >= 80 AND region NOT IN (\"cn\", \"sg\"))");
    }

    @Test
    void escapesTencentStringLiteralsAndRejectsUnsafeKeys() {
        Filter.Expression escaped = new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("tenant"), new Filter.Value("a\\\"b"));
        Filter.Expression unsafe = new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("tenant || true"), new Filter.Value("acme"));

        assertThat(converter.convertExpression(escaped)).isEqualTo("tenant = \"a\\\\\\\"b\"");
        assertThatThrownBy(() -> converter.convertExpression(unsafe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe Tencent VectorDB filter field");
    }
}
