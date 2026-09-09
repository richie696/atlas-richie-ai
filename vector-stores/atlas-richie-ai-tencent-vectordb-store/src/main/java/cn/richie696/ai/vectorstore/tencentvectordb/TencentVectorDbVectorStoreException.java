package cn.richie696.ai.vectorstore.tencentvectordb;

/** Normalized Tencent VectorDB SDK failure. */
public final class TencentVectorDbVectorStoreException extends RuntimeException {
    private final String operation;
    private final String databaseName;
    private final String collectionName;

    public TencentVectorDbVectorStoreException(String operation, String databaseName,
                                               String collectionName, Throwable cause) {
        super("Tencent VectorDB " + operation + " failed for '" + databaseName + "." + collectionName
                + "': " + cause.getMessage(), cause);
        this.operation = operation;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
    }
    public String getOperation() { return operation; }
    public String getDatabaseName() { return databaseName; }
    public String getCollectionName() { return collectionName; }
}
