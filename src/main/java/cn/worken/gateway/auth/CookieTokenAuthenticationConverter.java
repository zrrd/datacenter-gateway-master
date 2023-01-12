package cn.worken.gateway.auth;

import java.util.Optional;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author shaoyijiong
 * @date 2020/7/7
 */
public class CookieTokenAuthenticationConverter implements ServerAuthenticationConverter {

    /**
     * 从 cookie 中获取 token , 内部用户使用该种方式存储 token
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange serverWebExchange) {
        return Mono.justOrEmpty(token(serverWebExchange.getRequest())).map(BearerTokenAuthenticationToken::new);
    }

    private String token(ServerHttpRequest request) {
        if (!request.getCookies().isEmpty()) {
            return Optional.ofNullable(request.getCookies().getFirst("token")).map(HttpCookie::getValue).orElse(null);
        }
        return null;
    }
}
