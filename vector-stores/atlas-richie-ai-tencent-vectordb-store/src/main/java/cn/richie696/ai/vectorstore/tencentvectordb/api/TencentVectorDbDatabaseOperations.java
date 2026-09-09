package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.Database;

import java.util.List;

/** Tencent VectorDB database lifecycle operations. */
public interface TencentVectorDbDatabaseOperations {
    Database createDatabase(String name);
    Database createDatabaseIfNotExists(String name);
    boolean databaseExists(String name);
    List<String> listDatabases();
    Database dropDatabase(String name);
}
