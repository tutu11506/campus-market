package com.example.campusmarket.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 慢 SQL 采样：在 Executor 层统计每次 Mapper 调用的真实执行耗时，
 * 超过阈值（默认 500ms）打一条 WARN 日志，日志里带 Mapper 方法签名与截断后的 SQL。
 *
 * 说明：MyBatis 自身的 log-impl 只负责打印 SQL 文本，不做耗时统计；
 * 项目为避免参数泄露已关闭 SQL 全量打印，因此慢 SQL 只能靠这个拦截器采样。
 * 开启后不需要调高任何日志级别，也不会把正常 SQL 刷进日志。
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);

    /** 日志里 SQL 的最大长度，避免超长 IN 列表刷屏 */
    private static final int MAX_SQL_LENGTH = 500;

    @Value("${app.slow-sql.enabled:true}")
    private boolean enabled;

    @Value("${app.slow-sql.threshold-ms:500}")
    private long thresholdMs;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            if (enabled) {
                long costMs = (System.nanoTime() - start) / 1_000_000L;
                if (costMs >= thresholdMs) {
                    logSlowSql(invocation, costMs);
                }
            }
        }
    }

    private void logSlowSql(Invocation invocation, long costMs) {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement statement = (MappedStatement) args[0];
            Object parameter = args.length > 1 ? args[1] : null;
            String sql = statement.getBoundSql(parameter).getSql();
            log.warn("[慢SQL] {} 耗时 {}ms（阈值 {}ms）| SQL: {}",
                    statement.getId(), costMs, thresholdMs, shorten(sql));
        } catch (Exception e) {
            // 采样本身不能影响业务：取 SQL 文本失败时只记录耗时
            log.warn("[慢SQL] 执行耗时 {}ms，SQL 文本解析失败: {}", costMs, e.getMessage());
        }
    }

    private String shorten(String sql) {
        if (sql == null) {
            return "-";
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_SQL_LENGTH
                ? normalized
                : normalized.substring(0, MAX_SQL_LENGTH) + "...(截断)";
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 配置走 Spring 的 app.slow-sql.*，这里不需要额外属性
    }
}
