# 腾讯云 VectorDB 实网验收

## 当前状态

**待测试（外部环境不可用）**

- 插件实现、Spring Boot AutoConfiguration、本地单元测试和全仓构建保留。
- 当前无法取得可用的腾讯云 VectorDB 账号、实例 Endpoint 和 API Key，且该产品暂不支持公开注册，因此未执行真实厂商边界测试。
- 本地测试或 Mock 成功不作为腾讯云鉴权、协议兼容、数据一致性或高级能力可用性的实网证据。

## 待验收矩阵

| 能力 | 当前证据 | 实网状态 |
|---|---|---|
| Starter 依赖替换与 AutoConfiguration | 本地 ApplicationContext 测试通过 | 待实网启动验收 |
| Spring AI 标准 Add / Delete / Similarity Search | 已实现并通过本地编译、映射测试 | 待测试 |
| Database / Collection / Alias 生命周期 | SDK 接口已接入 | 待测试 |
| 原生文档 Insert / Update / Query / Delete / Count | SDK 接口已接入 | 待测试 |
| Vector / ID / Embedding Items 检索 | SDK 接口已接入 | 待测试 |
| Full Text / Hybrid Search | SDK 接口已接入 | 待测试 |
| Index 新增、修改与重建 | SDK 接口已接入 | 待测试 |
| 用户、授权与回收 | SDK 接口已接入 | 待测试 |
| AI Database、文件上传与文件详情 | SDK 接口已接入 | 待测试 |
| Image URL 与 Atomic Embedding | SDK 接口已接入 | 待测试 |
| 错误码、超时与最终一致性 | 仅有本地适配层证据 | 待测试 |

## 恢复条件

取得可用的测试账号后，需要准备以下运行时配置，且不得写入仓库：

- `TENCENT_VECTORDB_URL`
- `TENCENT_VECTORDB_API_KEY`
- 可用用户名、实例规格及允许创建和删除临时资源的权限

恢复验收时应使用唯一测试 Database/Collection，覆盖成功、权限拒绝、资源生命周期、最终一致性和幂等清理，并在结束后确认临时资源全部删除。
