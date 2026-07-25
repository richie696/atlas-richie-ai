package cn.richie696.ai.vectorstore.vikingdb;

/**
 * VikingDB 操作失败异常，保留操作名与集合名以便诊断。
 *
 * <p>封装数据面与控制面 SDK 异常，让调用方可以用统一类型处理所有 VikingDB 失败路径。
 * 异常消息按日志可 grep 的格式构造：始终以 {@code "VikingDB <operation>"} 开头。</p>
 */
public final class VikingDbVectorStoreException extends RuntimeException {

    /**
     * @param operation 失败的 SDK 调用短标签（如 {@code "upsertData"}、{@code "searchByVector"}、
     *                  {@code "schema initialization"}）。内嵌到异常消息中，
     *                  作为日志过滤的稳定标识。
     * @param collection 操作目标的集合名；若失败与具体集合无关则为 {@code null}
     *                  （例如 {@code assertSuccess} 中的空响应断言）。
     * @param cause 底层 SDK 异常；若失败为纯合成异常（例如校验抛出的
     *              {@code IllegalStateException}）则为 {@code null}。cause 作为
     *              {@link #getCause()} 保留以维持调用栈；其 message 可用时内联进
     *              包装消息，否则使用字面量 {@code "no cause available"}。
     */
    public VikingDbVectorStoreException(String operation, String collection, Throwable cause) {
        super("VikingDB " + operation + (collection == null ? "" : " failed for collection '" + collection + "'")
                + ": " + (cause == null ? "no cause available" : cause.getMessage()), cause);
    }
}