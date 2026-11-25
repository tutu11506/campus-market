package com.example.campusmarket.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置：
 * - 关闭 CSRF（前后端分离，使用 token 而非 cookie 会话）
 * - 无状态会话（不创建 HttpSession）
 * - CORS 收紧到前端开发地址 http://localhost:5173
 * - 放行登录/注册与公开 GET 接口，其余需登录，/admin/** 需 admin 角色
 * - 401/403 返回统一 JSON（与前端响应拦截器约定的 HTTP 状态码对齐）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 允许跨域的前端地址，逗号分隔。生产环境由 Nginx 同源反向代理，一般无需跨域，
     * 需要额外来源（ngrok、临时预览域名）时用 CORS_ALLOWED_ORIGINS 环境变量覆盖。
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    /**
     * 密码加密器：BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置源：只允许配置里列出的前端地址跨域
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 允许前端读取滑动续期下发的新 token
        config.setExposedHeaders(List.of("X-Refresh-Token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF
            .csrf(csrf -> csrf.disable())
            // 启用 CORS（使用上面的 CorsConfigurationSource）
            .cors(Customizer.withDefaults())
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 授权规则
            .authorizeHttpRequests(auth -> auth
                // 预检请求放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 登录、注册放行
                .requestMatchers("/user/login", "/user/register").permitAll()
                // 健康检查探活放行：只暴露 UP/DOWN 聚合状态，供 Docker healthcheck 使用
                .requestMatchers("/actuator/health").permitAll()
                // 商品、分类、商品评价的公开浏览接口（仅列表与详情），其余 GET 需登录
                .requestMatchers(HttpMethod.GET, "/goods/list", "/goods/{id}", "/goods/similar", "/goods/market-price",
                        "/goods/valuation", "/goods/platform-compare",
                        "/category/list", "/category/{id}", "/comment/goods/{goodsId}", "/comment/seller/{sellerId}").permitAll()
                // 首页聚合接口、缩略图、个性化推荐、校园专题公开访问（未登录时自动返回热门兜底）
                .requestMatchers(HttpMethod.GET, "/home/init", "/home/campus-topic", "/home/pickup-spots", "/home/active-hours",
                        "/home/deal-radar", "/home/market-overview", "/home/textbook-subjects",
                        "/home/location-detail", "/home/deal-trend", "/home/dorm-goods", "/home/dorm-detail",
                        "/home/demand-heat", "/home/graduation-sale", "/home/textbook-index", "/thumb/**",
                        "/recommend/personalized", "/recommend/repurchase-reminders").permitAll()
                // 校园闪送（即时履约）公开浏览
                .requestMatchers(HttpMethod.GET, "/flash/overview", "/flash/goods").permitAll()
                // 任务大厅公开浏览（发布/接单仍需登录）
                .requestMatchers(HttpMethod.GET, "/task/list", "/task/detail/**").permitAll()
                // 校园论坛公开浏览（发帖/回复/点赞仍需登录）
                .requestMatchers(HttpMethod.GET, "/forum/posts", "/forum/posts/**").permitAll()
                // 求购专区公开浏览（发布求购/报价仍需登录）
                .requestMatchers(HttpMethod.GET, "/demand/posts", "/demand/posts/**").permitAll()
                // 公益捐赠公开统计与社团目录（登记、我的记录仍需登录）
                .requestMatchers(HttpMethod.GET, "/donation/overview").permitAll()
                // 租赁专区公开浏览（发布出租、下单、我的租赁仍需登录）
                .requestMatchers(HttpMethod.GET, "/rental/items", "/rental/items/*").permitAll()
                // 拼单与搭子公开浏览（发起、参与、我的拼单/搭子仍需登录）
                .requestMatchers(HttpMethod.GET, "/group-buys", "/group-buys/*", "/partners", "/partners/*").permitAll()
                // 教材专区公开浏览（发布教材、课表导入、附件上传仍需登录）
                .requestMatchers(HttpMethod.GET,
                        "/textbooks", "/textbooks/*", "/textbooks/subjects",
                        "/textbooks/*/warnings",
                        "/textbooks/materials", "/textbooks/materials/*").permitAll()
                .requestMatchers("/textbooks/subjects").permitAll()
                .requestMatchers("/textbooks").permitAll()
                // 积分中心公开浏览（账户/流水/兑换/勋章解锁仍需登录）
                .requestMatchers(HttpMethod.GET,
                        "/points/rewards", "/points/leaderboard", "/points/badges/all").permitAll()
                // 风控公开查询：买家侧价格提示、卖家失信提醒
                .requestMatchers(HttpMethod.GET, "/risk/price/insight", "/risk/blacklist/check").permitAll()
                // 分类的增删改仅限管理员（防止普通登录用户越权篡改全局分类）
                .requestMatchers("/category/add", "/category/update", "/category/delete/**").hasAuthority("admin")
                // 上传的静态资源公开访问（GET）
                .requestMatchers(HttpMethod.GET, "/upload/**").permitAll()
                // Swagger UI / OpenAPI 文档公开访问
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml", "/webjars/**").permitAll()
                // 管理端接口与自动化预警中心需要 admin 角色（权限值存的是 "admin"，故用 hasAuthority）
                .requestMatchers("/admin/**", "/automation/**", "/risk/admin/**", "/dispute/admin/**",
                        "/subscribe/semester/scan").hasAuthority("admin")
                // 其余接口需登录
                .anyRequest().authenticated()
            )
            // 401/403 返回 JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或登录已过期，请重新登录"))
                .accessDeniedHandler((request, response, e) ->
                    writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "没有权限执行此操作"))
            )
            // 把 JWT 过滤器加到用户名密码过滤器之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用默认登录页与 HTTP Basic
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /**
     * 写出统一 JSON 错误响应（手动拼接，避免对 jackson-databind 的编译期依赖）
     */
    private static void writeJson(HttpServletResponse response, int httpStatus, int code, String msg) throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":" + code + ",\"msg\":\"" + escapeJson(msg) + "\",\"data\":null}";
        response.getWriter().write(json);
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
