package cn.richie696.ai.vectorstore.dashvector.api;

import com.aliyun.dashvector.models.PartitionStats;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.Status;

import java.util.List;

/** Bound-collection partition lifecycle and statistics operations. */
public interface DashVectorPartitionOperations {
    Response<Void> createPartition(String name, Integer timeoutSeconds);
    Response<Void> createPartition(String collectionName, String name, Integer timeoutSeconds);
    Response<Status> describePartition(String name);
    Response<Status> describePartition(String collectionName, String name);
    Response<List<String>> listPartitions();
    Response<List<String>> listPartitions(String collectionName);
    Response<PartitionStats> partitionStats(String name);
    Response<PartitionStats> partitionStats(String collectionName, String name);
    Response<Void> deletePartition(String name);
    Response<Void> deletePartition(String collectionName, String name);
}
