package com.aurora.admin.controller;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aurora.admin.config.ConfigCache;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.LoginRequest;
import com.aurora.admin.dto.LoginResponse;
import com.aurora.admin.dto.RegisterRequest;
import com.aurora.admin.entity.User;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.JwtUtil;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String RATE_LIMIT_LOGIN_KEY = "rate_limit:login:";
    private static final String RATE_LIMIT_REGISTER_KEY = "rate_limit:register:";
    private static final String AVAILABLE_PREFIX = "available:";

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private com.aurora.admin.mapper.MenuMapper menuMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ConfigCache configCache;

    // In-memory fallback buckets for when Redis is unavailable
    private final ConcurrentHashMap<String, Bucket> loginBucketsLocal = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBucketsLocal = new ConcurrentHashMap<>();

    private boolean redisAvailable = false;

    @PostConstruct
    public void init() {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().get("rate_limit:test");
                redisAvailable = true;
                log.info("[RateLimit] Redis available, using distributed rate limiting");
            } catch (Exception e) {
                log.warn("[RateLimit] Redis unavailable, using local buckets");
                redisAvailable = false;
            }
        } else {
            log.info("[RateLimit] RedisTemplate not configured, using local buckets");
        }
    }

    private Bucket getLoginBucket(String ip) {
        if (redisAvailable) {
            return getRedisLoginBucket(ip);
        }
        return loginBucketsLocal.computeIfAbsent(ip, k -> createLoginBucket());
    }

    private Bucket getRegisterBucket(String ip) {
        if (redisAvailable) {
            return getRedisRegisterBucket(ip);
        }
        return registerBucketsLocal.computeIfAbsent(ip, k -> createRegisterBucket());
    }

    private Bucket getRedisLoginBucket(String ip) {
        return getOrCreateBucket(getRedisLoginKey(ip), 5, Duration.ofMinutes(15), this::createLoginBucket);
    }

    private Bucket getRedisRegisterBucket(String ip) {
        return getOrCreateBucket(getRedisRegisterKey(ip), 3, Duration.ofHours(1), this::createRegisterBucket);
    }

    private Bucket getOrCreateBucket(String key, int tokens, Duration duration, java.util.function.Supplier<Bucket> creator) {
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr != null && countStr.startsWith(AVAILABLE_PREFIX)) {
                int available = Integer.parseInt(countStr.split(":")[1]);
                Bucket bucket = creator.get();
                int toConsume = tokens - available;
                if (toConsume > 0) {
                    bucket.tryConsume(toConsume);
                }
                return bucket;
            }
            return creator.get();
        } catch (Exception e) {
            return creator.get();
        }
    }

    private void saveBucketCount(String key, Bucket bucket, long ttlSeconds) {
        try {
            long available = bucket.getAvailableTokens();
            redisTemplate.opsForValue().set(key, AVAILABLE_PREFIX + available, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to save bucket count to Redis", e);
        }
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request, String forwardedFor) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getRedisLoginKey(String ip) {
        return RATE_LIMIT_LOGIN_KEY + ip;
    }

    private String getRedisRegisterKey(String ip) {
        return RATE_LIMIT_REGISTER_KEY + ip;
    }

    /**
     * 用户注册。
     *
     * <h3>注册规则</h3>
     * <ol>
     *   <li><b>限流</b>：同一 IP 每小时最多 3 次注册（Bucket4j），
     *       Redis 可用时使用分布式计数，否则回退到本地内存桶。</li>
     *   <li><b>总开关</b>：检查系统配置 {@code register.enabled}，
     *       关闭时返回 403（{@link ConfigCache#getBoolean ConfigCache}）。</li>
     *   <li><b>用户名唯一</b>：重复用户名返回 400。</li>
     *   <li><b>邮箱唯一</b>：重复邮箱返回 400。</li>
     *   <li><b>密码加密</b>：通过 {@link PasswordEncoder}（BCrypt）存储。</li>
     *   <li><b>默认角色</b>：注册用户默认分配 ROLE_USER。</li>
     * </ol>
     *
     * @param request  注册请求（username、password、email），通过 {@code @Valid} 校验
     * @param forwardedFor X-Forwarded-For 头，用于获取真实客户端 IP
     * @param httpRequest 原始 HTTP 请求
     * @return 200 注册成功 / 400 参数或唯一性校验失败 / 403 注册已关闭 / 429 触发限流
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request,
                                @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest, forwardedFor);
        Bucket bucket = getRegisterBucket(ip);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(429, "注册过于频繁，请稍后再试"));
        }

        if (redisAvailable) {
            saveBucketCount(getRedisRegisterKey(ip), bucket, 3600);
        }

        if (!configCache.getBoolean("register.enabled", true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "注册功能已关闭"));
        }

        User existUser = userService.findByUsername(request.getUsername());
        if (existUser != null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "注册失败，用户名已被使用"));
        }

        User existEmail = userService.findByEmail(request.getEmail());
        if (existEmail != null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "注册失败，邮箱已被使用"));
        }

        User user = userService.register(request.getUsername(), request.getPassword(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    /**
     * 用户登录。
     *
     * <h3>登录规则</h3>
     * <ol>
     *   <li><b>限流</b>：同一 IP 每 15 分钟最多 5 次尝试（Bucket4j），
     *       超限返回 429。</li>
     *   <li><b>账号匹配</b>：先按用户名查找，未找到再按邮箱查找，
     *       支持用户名或邮箱任一方式登录。</li>
     *   <li><b>密码校验</b>：通过 {@link PasswordEncoder#matches BCrypt} 比对，
     *       失败统一返回「用户名或密码错误」（不区分用户不存在/密码错误，防枚举）。</li>
     *   <li><b>角色查询</b>：从数据库 {@code t_user_role} 关联表查真实角色列表，
     *       若未关联任何角色则记录 warn 日志。</li>
     *   <li><b>JWT 签发</b>：token 包含 username、userId、roles，
     *       过期时间由 {@code JWT_EXPIRATION} 环境变量控制（默认 24 小时）。</li>
     * </ol>
     *
     * @param request  登录请求（username、password），通过 {@code @Valid} 校验
     * @param forwardedFor X-Forwarded-For 头，用于获取真实客户端 IP
     * @param httpRequest 原始 HTTP 请求
     * @return 200 登录成功（返回 token + 用户信息） / 401 用户名或密码错误
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request,
                              @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                              HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest, forwardedFor);
        Bucket bucket = getLoginBucket(ip);
        log.info("[RateLimit] IP={} availableTokens={} redisAvailable={}", ip, bucket.getAvailableTokens(), redisAvailable);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(429, "登录尝试次数过多，请15分钟后再试"));
        }

        if (redisAvailable) {
            saveBucketCount(getRedisLoginKey(ip), bucket, 900);
        } else {
            log.info("[RateLimit] Redis不可用，使用本地内存桶，IP={}", ip);
        }

        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            user = userService.findByEmail(request.getUsername());
        }
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        // 从数据库查询用户的真实角色
        List<String> roles = roleMapper.findCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            log.warn("用户 {} 没有任何角色关联", user.getUsername());
        }
        log.debug("用户 {} 的角色: {}", user.getUsername(), roles);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roles);
        String roleStr = roles.isEmpty() ? user.getRole() : String.join(",", roles);
        List<String> permissions = getPermissions(user.getId(), roles);
        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getNickname(), user.getEmail(), roleStr, user.getAvatar(), permissions);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户退出登录。
     *
     * <h3>退出规则</h3>
     * <ol>
     *   <li><b>Token 黑名单</b>：从 Authorization 头提取 Bearer token，
     *       写入 Redis 黑名单（key = {@code blacklist:<token>}），
     *       TTL 设为 token 剩余有效时长，过期自动清除。</li>
     *   <li><b>Redis 不可用时降级</b>：黑名单写入失败仅记录 warn 日志，
     *       不影响退出响应（仍然返回 200）。</li>
     *   <li><b>无状态退出</b>：服务端不维护 session，
     *       前端负责清除 sessionStorage 中的 token 并跳转登录页。</li>
     * </ol>
     *
     * <p>注意：加入黑名单的 token 在有效期内不能被复用，
     * JWT 本身无状态，黑名单是额外的防御层。</p>
     *
     * @param request HTTP 请求，用于提取 Authorization 头
     * @return 200 退出成功
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (redisAvailable && redisTemplate != null) {
                try {
                    Long expiration = jwtUtil.getExpiration(token);
                    if (expiration != null && expiration > 0) {
                        redisTemplate.opsForValue().set("blacklist:" + token, "1", Duration.ofMillis(expiration));
                    }
                } catch (Exception e) {
                    log.warn("Failed to blacklist token", e);
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success("退出成功"));
    }

    private List<String> getPermissions(Long userId, List<String> roles) {
        if (roles.contains("SUPER_ADMIN")) {
            List<com.aurora.admin.entity.Menu> allMenus = menuMapper.findAll();
            return allMenus.stream()
                    .filter(m -> m.getPermission() != null && !m.getPermission().isBlank())
                    .map(com.aurora.admin.entity.Menu::getPermission)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
        }
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roleMapper.findIdsByCodes(roles);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<com.aurora.admin.entity.Menu> userMenus = menuMapper.findByRoleIds(roleIds);
        return userMenus.stream()
                .filter(m -> m.getPermission() != null && !m.getPermission().isBlank())
                .map(com.aurora.admin.entity.Menu::getPermission)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
}
