package cn.worken.gateway.util;

import java.net.URI;
import java.util.LinkedHashSet;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 获取路径
 *
 * @author shaoyijiong
 * @date 2020/7/7
 */
public class GatewayUtils {

    private GatewayUtils() {
    }

    /**
     * 获取请求路径
     */
    public static String getRawPath(ServerWebExchange exchange) {
        String path;
        LinkedHashSet<URI> uris = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        if (null == uris || uris.isEmpty()) {
            path = exchange.getRequest().getURI().getRawPath();
        } else {
            path = uris.iterator().next().getRawPath();
        }
        return path;
    }
}
