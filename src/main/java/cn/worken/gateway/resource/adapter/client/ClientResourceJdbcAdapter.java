package cn.worken.gateway.resource.adapter.client;

import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.exception.GatewayException;
import cn.worken.gateway.dto.GatewayAuthenticationInfo;
import cn.worken.gateway.resource.ResourceAccessStatus;
import cn.worken.gateway.resource.ResourceAdapter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * client 权限控制
 *
 * @author shaoyijiong
 * @date 2020/7/7
 */
@Slf4j
@RefreshScope
@Component
public class ClientResourceJdbcAdapter implements ResourceAdapter<ClientApiResource> {

    @Value("${resource.version}")
    private Integer version;
    private final JdbcTemplate jdbcTemplate;
    /**
     * 路径对应的开放接口
     */
    private final Cache<String, ClientApiResource> apiResourceCache;
    /**
     * 开放接口对应拥有的资源
     */
    private final Cache<String, List<String>> appkeyResourceCache;

    public ClientResourceJdbcAdapter(JdbcTemplate jdbcTemplate) {
        log.info("重置缓存,版本信息[{}]", version);
        this.jdbcTemplate = jdbcTemplate;
        this.apiResourceCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).maximumSize(1000).build();
        this.appkeyResourceCache =
                CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).maximumSize(1000).build();
    }

    public void clearCache(String key) {
        if (key == null || key.isEmpty()) {
            apiResourceCache.invalidateAll();
            appkeyResourceCache.invalidateAll();
        } else {
            apiResourceCache.invalidate(key);
            appkeyResourceCache.invalidate(key);
        }

    }

    @Override
    public Mono<ClientApiResource> loadResource(String apiId) {
        ClientApiResource resource = Optional.ofNullable(apiResourceCache.getIfPresent(apiId)).orElseGet(() -> {
            String sql = "select id ,api_uri  from open_api where id = ? and status = 1";
            ClientApiResource result;
            try {
                result = jdbcTemplate.queryForObject(sql, new Object[]{apiId}, (resultSet, i) -> {
                    ClientApiResource clientApiResource = new ClientApiResource();
                    clientApiResource.setApiId(resultSet.getString("id"));
                    clientApiResource.setResourceName(resultSet.getString("api_uri"));
                    return clientApiResource;
                });
            } catch (Exception e) {
                log.info(e.getMessage());
                throw new GatewayException(GatewayCode.API_NOT_EXIST);
            }
            apiResourceCache.put(apiId, result);
            return result;
        });
        apiResourceCache.put(apiId, resource);
        return Mono.just(resource);
    }

    /**
     * 查询请求url对应的资源id
     *
     * @param serviceId 服务ID
     * @param reqUri    资源Uri
     */
    @Override
    public Mono<ClientApiResource> loadResourceByReqUri(String serviceId, String reqUri) {
        // 从数据库中查找该接口对应的 open_api
        String absoluteUrl = "/" + StringUtils.lowerCase(serviceId) + reqUri;
        ClientApiResource resource = Optional.ofNullable(apiResourceCache.getIfPresent(absoluteUrl)).orElseGet(() -> {
            String sql = "select id,api_uri from open_api where api_uri = ? and status = 1";
            ClientApiResource result;
            try {
                result = jdbcTemplate.queryForObject(sql, new Object[]{absoluteUrl}, (resultSet, i) -> {
                    ClientApiResource clientApiResource = new ClientApiResource();
                    clientApiResource.setApiId(resultSet.getString("id"));
                    clientApiResource.setResourceName(resultSet.getString("api_uri"));
                    return clientApiResource;
                });
            } catch (Exception e) {
                log.info(e.getMessage());
                throw new GatewayException(GatewayCode.API_NOT_EXIST);
            }
            apiResourceCache.put(absoluteUrl, result);
            return result;
        });
        return Mono.just(resource);
    }

    @Override
    public Mono<ResourceAccessStatus> access(GatewayAuthenticationInfo authenticationInfo,
                                             Mono<ClientApiResource> apiResource) {
        // 判断该 client 拥有的资源id 是否匹配d
        return loadClientApiId(authenticationInfo.getClientId())
                // 判断匹配
                .flatMap(apiId -> apiResource.map(resource -> resource.getApiId().equals(apiId)))
                // 存在匹配上的
                .any(Boolean::booleanValue)
                .map(has -> {
                    if (has) {
                        return ResourceAccessStatus.accessSuccess();
                    } else {
                        return ResourceAccessStatus
                                .accessFail(GatewayCode.ACCESS_DENY.getCode(), GatewayCode.ACCESS_DENY.getMessage());
                    }
                });

    }

    /**
     * 查询客户端拥有的api
     *
     * @param appKey 客户端 client id
     */
    public Flux<String> loadClientApiId(String appKey) {
        List<String> apiList = Optional.ofNullable(appkeyResourceCache.getIfPresent(appKey)).orElseGet(() -> {
            String sql = "select api_id from open_api_grant_rel where app_key= ?";
            List<String> result = jdbcTemplate.queryForList(sql, new Object[]{appKey}, String.class);
            if (result.isEmpty()) {
                throw new GatewayException(GatewayCode.API_NOT_EXIST);
            }
            appkeyResourceCache.put(appKey, result);
            return result;
        });
        return Flux.fromIterable(apiList);
    }
}
