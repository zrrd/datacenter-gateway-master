package cn.worken.gateway.resource;

import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.exception.GatewayException;
import cn.worken.gateway.dto.GatewayAuthenticationInfo;
import cn.worken.gateway.resource.adapter.client.ClientResourceJdbcAdapter;
import cn.worken.gateway.resource.adapter.user.UserResourceAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author shaoyijiong
 * @date 2020/7/7
 */
@Component
public class ResourceAccessFactory {

    private final ClientResourceJdbcAdapter clientResourceAdapter;
    private final UserResourceAdapter userResourceAdapter;

    public ResourceAccessFactory(ClientResourceJdbcAdapter clientResourceAdapter, UserResourceAdapter userResourceAdapter) {
        this.clientResourceAdapter = clientResourceAdapter;
        this.userResourceAdapter = userResourceAdapter;
    }

    public Mono<ResourceAccessStatus> access(boolean isUser, ServerWebExchange exchange,
        GatewayAuthenticationInfo authenticationInfo) {
        // 判断是内部用户还是client , 得到不同的 adapter
        ResourceAdapter resourceAdapter = isUser ? userResourceAdapter : clientResourceAdapter;
        Mono apiResource = resourceAdapter.loadResource(exchange)
            .switchIfEmpty(Mono.error(new GatewayException(GatewayCode.API_NOT_EXIST)));
        return resourceAdapter.access(authenticationInfo, apiResource);
    }
}
