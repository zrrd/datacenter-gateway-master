package cn.worken.gateway.resource;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;


import cn.worken.gateway.dto.GatewayAuthenticationInfo;
import cn.worken.gateway.util.RouteUtils;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/4 12:39
 */
public interface ResourceAdapter<T extends ApiResource> {

    /**
     * 通过请求上下文获取 apiResource
     *
     * @param exchange 请求上下文
     * @return 资源
     */
    default Mono<T> loadResource(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return null;
        }
        String serviceId = RouteUtils.getLbName(route);
        return this.loadResourceByReqUri(serviceId, exchange.getRequest().getPath().value()).doOnNext(apiResource -> {
            if (apiResource != null) {
                apiResource.setServiceName(serviceId);
            }
        });
    }


    /**
     * 根据资源id加载api资源
     *
     * @param apiId 资源id
     * @return api资源
     */
    Mono<T> loadResource(String apiId);

    /**
     * 根据请求URL加载api资源
     *
     * @param serviceId 服务ID
     * @param reqUri 资源Uri
     * @return api资源
     */
    Mono<T> loadResourceByReqUri(String serviceId, String reqUri);

    /**
     * 验证对该资源的访问权限
     *
     * @param authenticationInfo 用户/客户端标识
     * @param apiResource 访问资源
     * @return 返回访问权限检查状态
     */
    Mono<ResourceAccessStatus> access(GatewayAuthenticationInfo authenticationInfo, Mono<T> apiResource);
}
