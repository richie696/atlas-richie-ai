package cn.richie696.ai.vectorstore.dashvector;

/** Normalized DashVector SDK failure. */
public final class DashVectorVectorStoreException extends RuntimeException {
    private final String operation;
    private final String collectionName;
    private final Integer code;
    private final String requestId;

    public DashVectorVectorStoreException(String operation, String collectionName,
                                          Integer code, String requestId, String message) {
        super("DashVector " + operation + " failed for collection '" + collectionName + "': " + message);
        this.operation = operation;
        this.collectionName = collectionName;
        this.code = code;
        this.requestId = requestId;
    }

    public String getOperation() { return operation; }
    public String getCollectionName() { return collectionName; }
    public Integer getCode() { return code; }
    public String getRequestId() { return requestId; }
}
