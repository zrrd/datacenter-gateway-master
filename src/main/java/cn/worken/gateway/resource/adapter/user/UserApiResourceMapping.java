package cn.worken.gateway.resource.adapter.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * TODO 修改缓存方式
 *
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/24 14:26
 */
@Component
public class UserApiResourceMapping {

    private static final Pattern REST_URI_PATTERN = Pattern.compile("\\{\\S*}");
    private final Map<String, String> serviceVersionCache = new ConcurrentHashMap<>();
    private final Map<String, ServiceResource> serviceResourceMap = new ConcurrentHashMap<>();


    public UserApiResource getUserApiResource(String serviceId, String uri) {
        ServiceResource resource = serviceResourceMap.get(serviceId);
        if (null == resource) {
            return null;
        }
        return resource.getPermissionCode(uri);
    }

    /**
     * 更新服务接口 权限编码 映射关系
     */
    public synchronized void updateServiceApiMapping(String serviceName, String content) {
        // 接口版本号
        String sha1 = DigestUtils.sha1Hex(content);
        String preSha1 = serviceVersionCache.get(serviceName);
        // 判断 hash 是否一直 , 不一致才更新
        if (StringUtils.equals(sha1, preSha1)) {
            return;
        }
        // 更新颁布控制
        serviceVersionCache.put(serviceName, sha1);
        JSONObject apiSecurityCodeMapping = JSON.parseObject(content);
        if (apiSecurityCodeMapping.isEmpty()) {
            serviceResourceMap.remove(serviceName);
            return;
        }
        ServiceResource resource = new ServiceResource(serviceName);
        // key 该服务对应的接口名 code 该服务接口对应的权限编码
        for (String key : apiSecurityCodeMapping.keySet()) {
            String code = apiSecurityCodeMapping.getString(key);
            Matcher matcher = REST_URI_PATTERN.matcher(key);
            if (matcher.find()) {
                String pattern = matcher.replaceAll("*");
                resource.addUriMatcher(pattern, key, code);
            } else {
                resource.addApi(key, code);
            }
        }

        serviceResourceMap.put(serviceName, resource);
    }

    private static class ServiceResource {

        private static final AntPathMatcher MATCHER = new AntPathMatcher();
        private final String name;
        private Map<String, String> apiCodeMapping;
        private Map<String, MutablePair<String, String>> uriPatternCodeMapping;
        private final Map<String, UserApiResource> cache = new ConcurrentHashMap<>();

        public ServiceResource(String name) {
            this.name = name;
        }

        public void addApi(String uri, String code) {
            if (null == apiCodeMapping) {
                apiCodeMapping = new HashMap<>(16);
            }
            apiCodeMapping.put(uri, code);
        }

        public void addUriMatcher(String uriPattern, String resource, String code) {
            if (null == uriPatternCodeMapping) {
                uriPatternCodeMapping = new HashMap<>(16);
            }
            uriPatternCodeMapping.put(uriPattern, MutablePair.of(code, resource));
        }

        private Set<Map.Entry<String, MutablePair<String, String>>> uriPatternEntrySet() {
            return null == uriPatternCodeMapping ? Collections.emptySet() : uriPatternCodeMapping.entrySet();
        }

        public UserApiResource getPermissionCode(String uri) {
            String code = null != apiCodeMapping ? apiCodeMapping.get(uri) : null;
            if (null == code) {
                for (Map.Entry<String, MutablePair<String, String>> entry : uriPatternEntrySet()) {
                    if (MATCHER.match(entry.getKey(), uri)) {
                        return getResource(entry.getValue().getRight(), entry.getValue().getLeft());
                    }
                }
                return null;
            } else {
                return getResource(uri, code);
            }
        }

        private UserApiResource getResource(String res, String code) {
            return cache.computeIfAbsent(res, k -> {
                UserApiResource apiResource = new UserApiResource();
                apiResource.setApiId(code);
                apiResource.setResourceName(res);
                apiResource.setServiceName(name);
                return apiResource;
            });
        }
    }

}
