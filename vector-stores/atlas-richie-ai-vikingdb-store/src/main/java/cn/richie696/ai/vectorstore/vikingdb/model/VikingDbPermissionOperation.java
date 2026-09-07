package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.Locale;

/**
 * Logical VikingDB operations used for IAM diagnosis.
 *
 * <p>The vendor's current V2 policy documentation publishes read wildcards and a write wildcard,
 * rather than one stable V2 IAM action per SDK endpoint. This enum preserves the endpoint-level
 * diagnostic while exposing only documented policy hints to callers.</p>
 */
public enum VikingDbPermissionOperation {
    COLLECTION_READ("getCollection", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    COLLECTION_CREATE("createCollection", "vikingdb:*", "VikingdbFullAccess"),
    COLLECTION_DELETE("deleteCollection", "vikingdb:*", "VikingdbFullAccess"),
    INDEX_READ("getIndex", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    INDEX_CREATE("createIndex", "vikingdb:*", "VikingdbFullAccess"),
    INDEX_UPDATE("updateIndex", "vikingdb:*", "VikingdbFullAccess"),
    INDEX_ENABLE("enableIndex", "vikingdb:*", "VikingdbFullAccess"),
    INDEX_DISABLE("disableIndex", "vikingdb:*", "VikingdbFullAccess"),
    INDEX_DELETE("deleteIndex", "vikingdb:*", "VikingdbFullAccess"),
    DOCUMENT_UPSERT("upsertData", "vikingdb:*", "VikingdbFullAccess"),
    DOCUMENT_DELETE("deleteData", "vikingdb:*", "VikingdbFullAccess"),
    DOCUMENT_FETCH("fetchDataInIndex", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    FILTER_DELETE("filterDelete", "vikingdb:*", "VikingdbFullAccess"),
    VECTOR_SEARCH("searchByVector", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    KEYWORD_SEARCH("searchByKeywords", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    MULTIMODAL_SEARCH("searchByMultiModal", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    RERANK("rerank", "vikingdb:Get* / vikingdb:List* / vikingdb:Read*", "VikingdbReadOnlyAccess"),
    UNKNOWN("unknown", "VikingdbFullAccess", "VikingdbFullAccess");

    private final String sdkOperation;
    private final String iamActionHint;
    private final String managedPolicy;

    VikingDbPermissionOperation(String sdkOperation, String iamActionHint, String managedPolicy) {
        this.sdkOperation = sdkOperation;
        this.iamActionHint = iamActionHint;
        this.managedPolicy = managedPolicy;
    }

    public String sdkOperation() { return sdkOperation; }
    public String iamActionHint() { return iamActionHint; }
    public String managedPolicy() { return managedPolicy; }

    public static VikingDbPermissionOperation fromSdkOperation(String operation) {
        String normalized = operation == null ? "" : operation.toLowerCase(Locale.ROOT);
        for (VikingDbPermissionOperation value : values()) {
            if (value != UNKNOWN && normalized.startsWith(value.sdkOperation.toLowerCase(Locale.ROOT))) return value;
        }
        if (normalized.contains("schema initialization")) return COLLECTION_CREATE;
        return UNKNOWN;
    }
}
