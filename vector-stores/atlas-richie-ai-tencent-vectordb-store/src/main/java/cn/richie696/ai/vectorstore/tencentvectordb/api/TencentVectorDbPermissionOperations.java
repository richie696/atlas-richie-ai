package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.param.entity.BaseRes;
import com.tencent.tcvectordb.model.param.user.UserDescribeRes;
import com.tencent.tcvectordb.model.param.user.UserGrantParam;
import com.tencent.tcvectordb.model.param.user.UserListRes;
import com.tencent.tcvectordb.model.param.user.UserRevokeParam;

/** Tencent VectorDB database-user and privilege administration. */
public interface TencentVectorDbPermissionOperations {
    BaseRes createUser(String username, String password);
    UserDescribeRes describeUser(String username);
    UserListRes listUsers();
    BaseRes grant(UserGrantParam param);
    BaseRes revoke(UserRevokeParam param);
    BaseRes changePassword(String username, String password);
    BaseRes dropUser(String username);
}
