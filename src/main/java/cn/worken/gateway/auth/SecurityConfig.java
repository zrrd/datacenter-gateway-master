package cn.worken.gateway.auth;

import cn.worken.gateway.config.constant.ReqContextConstant;
import cn.worken.gateway.config.constant.UserConstants;
import cn.worken.gateway.resource.manage.WhiteListServerWebExchangeMatcher;
import cn.worken.gateway.util.RSAUtils;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 鉴权判断
 *
 * @author shaoyijiong
 * @date 2020/7/4
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher;

    public SecurityConfig(WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher) {
        this.whiteListServerWebExchangeMatcher = whiteListServerWebExchangeMatcher;
    }

    /**
     * rsa 加密 key
     */
    @SneakyThrows
    @Bean
    public NimbusReactiveJwtDecoder jwtDecoder() {
        RSAPublicKey publicKey = RSAUtils.getPublicKey(PubKey.VALUE);
        return new NimbusReactiveJwtDecoder(publicKey);
    }


    /**
     * 鉴权配置
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // 允许端点监控
        return http.csrf().disable()
            // 白名单不做权限控制
            .authorizeExchange().matchers(whiteListServerWebExchangeMatcher).permitAll()
            // 其他所有接口需要鉴权
            .and().authorizeExchange().anyExchange().access((authentication, object) -> {
                // 获取token 先从cookie中获取 再从header中获取
                return new CookieTokenAuthenticationConverter().convert(object.getExchange())
                    .switchIfEmpty(new ServerBearerTokenAuthenticationConverter().convert(object.getExchange()))
                    .switchIfEmpty(Mono.error(new AccessTokenRequiredException("未携带有效token", null)))
                    .map(auth -> (BearerTokenAuthenticationToken) auth)
                    // 校验 jwt token
                    .flatMap(authToken -> jwtDecoder().decode(authToken.getToken()))
                    // 校验成功 , 在 attributes 中放入校验后的信息 jwt
                    .doOnSuccess(authJwt -> object.getExchange().getAttributes()
                        .put(ReqContextConstant.SECURITY_INFO_IN_REQ, authJwt))
                    // 判断是否是平台用户还是 client 请求
                    .doOnSuccess(authJwt -> {
                        Map<String, Object> attributes = object.getExchange().getAttributes();
                        boolean isUser = authJwt.getClaims().get(UserConstants.USER_NAME) != null;
                        // 请求是否为平台用户 or client
                        attributes.put(ReqContextConstant.SECURITY_IS_USER, isUser);
                        // 请求用户唯一标识 如果是用户的话使用 username ; client 的话使用 client_id
                        Object identifies = isUser ? authJwt.getClaims().get(UserConstants.USER_NAME)
                            : authJwt.getClaims().get(UserConstants.CLIENT_ID);
                        attributes.put(ReqContextConstant.X_IDENTIFIES, identifies);
                    })
                    .map(jwt -> new AuthorizationDecision(true))
                    // 校验失败 抛出异常 交给全局异常处理
                    .onErrorReturn((throwable) -> {
                        log.error("", throwable);
                        return true;
                    }, new AuthorizationDecision(false));
            })
            // 鉴权校验失败 直接抛出异常交给全局异常处理
            .and().exceptionHandling().authenticationEntryPoint((exchange, e) -> Mono.error(e))
            // 资源校验失败 直接抛出异常交给全局异常处理
            .and().exceptionHandling().accessDeniedHandler((exchange, e) -> Mono.error(e))
            .and().build();
    }

    public interface PubKey {

        String VALUE =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCUHHjftKwmmTvhu1nheSfti5w5edO6SROcdBwHsqEFHdEwc-H4xauApJoe9AubUpDmCFBiuojsL9oS33LIfvjvddXzzIXYA3qJorLcSNu2Rid3Wev1wgXp3RC8qkoln26hXuw9ktyO7Rbmlvq9sIJWDHXDNzF_OePa1fTf3ErBKQIDAQAB";
    }

}
