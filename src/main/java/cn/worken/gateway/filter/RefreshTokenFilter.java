package cn.worken.gateway.filter;

import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.constant.ReqContextConstant;
import cn.worken.gateway.config.exception.GatewayException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 用户刷新token , 用于用户登陆自动延期
 *
 * @author shaoyijiong
 * @date 2020/8/3
 */
@Slf4j
@Component
public class RefreshTokenFilter implements GlobalFilter, Ordered {

    @Value("${oauth.client_id}")
    private String clientId;
    @Value("${oauth.client_secret}")
    private String clientSecret;
    private final RestTemplate restTemplate;
    /**
     * 剩下多少时间的时候进行续签
     */
    private static final int EXPIRES_DIVIDE = 3;

    public RefreshTokenFilter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Boolean isUser = exchange.getAttribute(ReqContextConstant.SECURITY_IS_USER);
        Jwt jwt = exchange.getAttribute(ReqContextConstant.SECURITY_INFO_IN_REQ);
        if (jwt == null || isUser == null) {
            return chain.filter(exchange);
        }
        // 从 cookie 中获取 refresh_token
        String refreshToken = Optional.ofNullable(exchange.getRequest().getCookies().getFirst("refresh_token"))
            .map(HttpCookie::getValue).orElse(null);
        if (jwt.getExpiresAt() != null && refreshToken != null) {
            // 到期时间
            LocalDateTime expire = LocalDateTime.ofInstant(jwt.getExpiresAt(), ZoneId.of("Asia/Shanghai"));
            Long expiresIn = jwt.getClaim("expires_in");
            if (expiresIn == null || expiresIn < 0) {
                return chain.filter(exchange);
            }
            // 判断到期时间是否剩余 1/3
            if (expire.isBefore(LocalDateTime.now().plusSeconds(expiresIn / EXPIRES_DIVIDE))) {
                JSONObject refreshResp = getRefreshToken(refreshToken);
                // 错误情况
                if (refreshResp.getInteger("code") != null) {
                    // 清空cookie
                    setCookie(exchange, "token", "", 0);
                    setCookie(exchange, "refresh_token", "", 0);
                    throw new GatewayException(GatewayCode.AUTHENTICATION_FAILURE);
                }
                // 将新的token 和 refresh_token 重新放入 cookie 中去
                setCookie(exchange, "token", refreshResp.getString("access_token"), refreshResp.getLong("expires_in"));
                setCookie(exchange, "refresh_token", refreshResp.getString("refresh_token"),
                    refreshResp.getLong("expires_in"));
            }
        }
        return chain.filter(exchange);
    }

    /**
     * 请求鉴权服务refresh token
     */
    private JSONObject getRefreshToken(String refreshToken) {
        // 请求鉴权服务刷新 token
        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("grant_type", "refresh_token");
        param.add("client_id", clientId);
        param.add("client_secret", clientSecret);
        param.add("refresh_token", refreshToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(param, new HttpHeaders());
        // 正确返回
        // {
        //    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX3R5cGUiOm51bGwsInVzZXJfaWQiOjEwMDEsImxvZ2luX3R5cGUiOiJETUsiLCJ1c2VyX25hbWUiOiJ1c2VyXzEiLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwiY29tX2lkIjoiMCIsIm5hbWUiOiLkvZrlkI0xIiwiZXhwIjoxNTk2NzA2OTc0LCJleHBpcmVzX2luIjoyNTkxOTksImp0aSI6IjdjNWIyNjQyLTI1MmEtNDU3MS1hOTE0LWE3OWMzMTdjOTkxOSIsImNsaWVudF9pZCI6ImNvbV9jbGllbnQifQ.K1J1sxAcAsx5Zad1csxmpTj0pCuaWYT6t7MG5pZ91Ev__owqlZh9eRiswN3qiQOkGW4LsQLlWMZu7I1yOJqFthLjYyhT_jzEGzX58oR55Skfo4Xf5Np-vQASH9iNco0ubYT6czqnw9YEErNUSZrxA9HNCsbAv35cy3xUxTLAomw",
        //    "token_type": "bearer",
        //    "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX3R5cGUiOm51bGwsInVzZXJfaWQiOjEwMDEsImxvZ2luX3R5cGUiOiJETUsiLCJ1c2VyX25hbWUiOiJ1c2VyXzEiLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwiYXRpIjoiN2M1YjI2NDItMjUyYS00NTcxLWE5MTQtYTc5YzMxN2M5OTE5IiwiY29tX2lkIjoiMCIsIm5hbWUiOiLkvZrlkI0xIiwiZXhwIjoxNTk3MDQ2NTUyLCJleHBpcmVzX2luIjo1OTg3NzYsImp0aSI6IjIxNmEwM2Y4LTE3NzMtNDA1MS1hYjYxLWYxNTkzODQxYzYzZCIsImNsaWVudF9pZCI6ImNvbV9jbGllbnQifQ.AHt5X1TQNHesEdnFnZ1GqMiz4bAq07H5tvXGZMT3v1xh0BB4341rTt94nB6qyyuyBVKGoXvC-O_p1wodmIT8kEI5mInppOkP3UABGHlVaUnSaJf6FWhQ53cS6d-cT4Cnv_ezW8tRf_AydfCzMD8mJx2h069X3QBunTQJEpKB08U",
        //    "expires_in": 259198,
        //    "scope": "read write",
        //    "jti": "7c5b2642-252a-4571-a914-a79c317c9919"
        // }
        // 错误返回
        // {"message":"认证失败","code":401}
        // {"message":"系统异常","code":500}
        String resp = restTemplate.postForObject("http://auth-server/oauth/token", request, String.class);
        log.info("刷新token返回[{}]", resp);
        return JSON.parseObject(resp);
    }

    /**
     * 实际塞cookie
     */
    private void setCookie(ServerWebExchange exchange, String key, String value, long maxAge) {
        exchange.getResponse()
            .addCookie(ResponseCookie.from(key, value).maxAge(maxAge).httpOnly(false).path("/").build());
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrders.REFRESH_TOKEN.getOrder();
    }
}
