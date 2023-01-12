package cn.worken.gateway.filter;

import cn.worken.gateway.config.constant.ClientConstants;
import cn.worken.gateway.config.constant.GatewayTransHeader;
import cn.worken.gateway.config.constant.ReqContextConstant;
import cn.worken.gateway.config.constant.UserConstants;
import cn.worken.gateway.dto.GatewayAuthenticationInfo;
import cn.worken.gateway.resource.OpenAPIListServerWebExchangeMatcher;
import cn.worken.gateway.resource.manage.WhiteListServerWebExchangeMatcher;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户认证信息解析
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Component
@Slf4j
public class AuthenticationRetrieveFilter implements GlobalFilter, Ordered {

    private final WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher;

    public AuthenticationRetrieveFilter(WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher) {
        this.whiteListServerWebExchangeMatcher = whiteListServerWebExchangeMatcher;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (whiteListServerWebExchangeMatcher.isWhiteApi(exchange)) {
            return chain.filter(exchange);
        }
        Boolean isUser = exchange.getAttribute(ReqContextConstant.SECURITY_IS_USER);
        Jwt jwt = exchange.getAttribute(ReqContextConstant.SECURITY_INFO_IN_REQ);
        if (jwt == null || isUser == null) {
            return chain.filter(exchange);
        }
        GatewayAuthenticationInfo authenticationInfo;
        if (isUser) {
            // 封装用户信息
            authenticationInfo = GatewayAuthenticationInfo.builder()
                .userId(String.valueOf(jwt.<Integer>getClaim(UserConstants.USER_ID)))
                .clientId(jwt.getClaim(UserConstants.CLIENT_ID))
                .comId(jwt.getClaim(UserConstants.COM_ID))
                .username(jwt.getClaim(UserConstants.USER_NAME))
                // 防止中文乱码
                .name(Optional.ofNullable(jwt.getClaimAsString(UserConstants.NAME))
                    .map(n -> Base64Utils.encodeToString(n.getBytes())).orElse(null))
                .userType(jwt.getClaim(UserConstants.USER_TYPE))
                .server(jwt.getClaim(UserConstants.SERVER))
                .productId(jwt.getClaim(UserConstants.PRODUCT_ID))
                .build();
        } else {
            // client 请求封装 只有 client_id 和 com_id
            authenticationInfo = GatewayAuthenticationInfo.builder()
                .clientId(jwt.getClaim(ClientConstants.CLIENT_ID))
                .comId(jwt.getClaim(ClientConstants.COM_ID))
                .confProductCode(jwt.getClaim(ClientConstants.PRODUCT_CODE))
                .build();
        }
        // attribute 存入用户信息
        exchange.getAttributes().put(ReqContextConstant.GATEWAY_AUTHENTICATION_INFO, authenticationInfo);
        // 请求头存入用户信息 供后续服务访问
        exchange.getRequest().mutate()
            .header(GatewayTransHeader.X_GATEWAY_AUTHENTICATION_INFO, JSON.toJSONString(authenticationInfo));

        // 存入用户唯一标识 用于做用户识别(限流)
        if(OpenAPIListServerWebExchangeMatcher.testOpenAPIList(exchange)) {
            // 针对开放API接口，引导其他限流规则
            exchange.getAttributes().remove(GatewayTransHeader.X_IDENTIFIES);
            exchange.getRequest().mutate()
                    .header(GatewayTransHeader.X_OPENAPI, UUID.randomUUID().toString());
        } else {
            exchange.getRequest().mutate()
                    .header(GatewayTransHeader.X_IDENTIFIES, exchange.getAttributeOrDefault(ReqContextConstant.X_IDENTIFIES,
                            UUID.randomUUID().toString()));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrders.AUTHENTICATION.getOrder();
    }
}
