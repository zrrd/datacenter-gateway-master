package cn.worken.gateway.resource;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * 黑白名单控制数据,使用配置中心配置  添加刷新配置接口
 *
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/24 15:13
 * <p>
 * RefreshScope need be managed as spring bean type
 */
@Slf4j
@Component
public class ResourceControl {

    private final ResourceControlProperties properties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public ResourceControl(ResourceControlProperties properties) {
        this.properties = properties;
    }

    /**
     * 不需要资源校验的服务
     */
    public boolean isExcludeResourceService(String service) {
        boolean excludeService =
            StringUtils.isBlank(service) || properties.getExcludeResourceServiceList().contains(service);
        log.debug("网关uri控制[excludeService]--->{}   {}", service, excludeService);
        return excludeService;
    }

    /**
     * 访问白名单 无需登陆
     */
    public boolean isWhiteApi(String api) {
        boolean whiteApi;
        if (StringUtils.isBlank(api)) {
            whiteApi = true;
        } else {
            whiteApi = anyMatch(api, properties.getWhiteApiList());
        }
        log.debug("网关uri控制[whiteApi]--->{}   {}", api, whiteApi);
        return whiteApi;
    }

    /**
     * 用户黑名单 用户无法访问
     */
    public boolean isBlockApiList(String api) {
        boolean blockApiList;
        if (StringUtils.isBlank(api)) {
            blockApiList = false;
        } else {
            blockApiList = anyMatch(api, properties.getBlockApiList());
        }
        log.debug("网关uri控制[UserBlockApiList]--->{}   {}", api, blockApiList);
        return blockApiList;
    }

    private boolean anyMatch(String api, Set<String> apiPatternList) {
        if (apiPatternList.contains(api)) {
            return true;
        }

        for (String pattern : apiPatternList) {
            if (antPathMatcher.match(pattern, api)) {
                return true;
            }
        }
        return false;
    }
}
