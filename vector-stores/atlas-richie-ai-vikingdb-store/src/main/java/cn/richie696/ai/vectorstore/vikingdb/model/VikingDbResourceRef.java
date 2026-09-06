package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.util.Assert;

/**
 * Immutable VikingDB resource identity.
 */
public record VikingDbResourceRef(String projectName, String collectionName, String indexName) {
    public VikingDbResourceRef {
        Assert.hasText(collectionName, "collectionName must not be blank");
        Assert.hasText(indexName, "indexName must not be blank");
    }
}
