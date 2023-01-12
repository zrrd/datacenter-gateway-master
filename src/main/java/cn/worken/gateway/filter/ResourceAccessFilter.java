package cn.worken.gateway.filter;

import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.constant.ReqContextConstant;
import cn.worken.gateway.config.exception.GatewayException;
import cn.worken.gateway.dto.GatewayAuthenticationInfo;
import cn.worken.gateway.resource.ResourceAccessFactory;
import cn.worken.gateway.resource.ResourceAccessStatus;
import cn.worken.gateway.resource.manage.WhiteListServerWebExchangeMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 资源拦截
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Component
@Slf4j
public class ResourceAccessFilter implements GlobalFilter, Ordered {

    private final WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher;
    private final ResourceAccessFactory resourceAccessFactory;

    public ResourceAccessFilter(WhiteListServerWebExchangeMatcher whiteListServerWebExchangeMatcher,
        ResourceAccessFactory resourceAccessFactory) {
        this.whiteListServerWebExchangeMatcher = whiteListServerWebExchangeMatcher;
        this.resourceAccessFactory = resourceAccessFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (whiteListServerWebExchangeMatcher.isWhiteApi(exchange)) {
            return chain.filter(exchange);
        }
        boolean isUser = exchange.getAttributeOrDefault(ReqContextConstant.SECURITY_IS_USER, false);
        GatewayAuthenticationInfo authenticationInfo =
            exchange.getAttribute(ReqContextConstant.GATEWAY_AUTHENTICATION_INFO);
        if (authenticationInfo == null) {
            throw new GatewayException(GatewayCode.AUTHENTICATION_FAILURE);
        }
        // 将请求上下文交给 factory 处理 resource 处理
        Mono<ResourceAccessStatus> accessResult = resourceAccessFactory.access(isUser, exchange, authenticationInfo);
        return accessResult.doOnNext(access -> {
            if (!access.isAccess()) {
                throw new GatewayException(access.getDenyCode(), access.getDenyMsg());
            }
        }).then(chain.filter(exchange));

    }

    @Override
    public int getOrder() {
        return GlobalFilterOrders.RESOURCE_ACCESS.getOrder();
    }
}
