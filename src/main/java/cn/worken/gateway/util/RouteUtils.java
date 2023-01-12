package cn.worken.gateway.util;

import java.net.URI;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.route.Route;

/**
 * 获取路由的服务名
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
public class RouteUtils {

    private RouteUtils() {
    }

    /**
     * 根据 route 获取服务名
     */
    public static String getLbName(Route route) {
        URI uri = route.getUri();
        if (StringUtils.equalsIgnoreCase(uri.getScheme(), "lb") && StringUtils.isNotBlank(uri.getHost())) {
            return uri.getHost();
        } else {
            return StringUtils.substringAfter(uri.getSchemeSpecificPart(), "//");
        }
    }
}
