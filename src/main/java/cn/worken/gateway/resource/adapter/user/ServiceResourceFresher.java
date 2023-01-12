package cn.worken.gateway.resource.adapter.user;

import cn.worken.gateway.resource.ResourceControl;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 刷新客户端
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Slf4j
@Component
public class ServiceResourceFresher implements ApplicationListener<HeartbeatEvent> {


    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final Cache<String, List<ServiceInstance>> serviceInstanceCache;
    private final Cache<String, URI> successLoadServiceCache;
    private final UserApiResourceMapping updateServiceApiMapping;
    private final ResourceControl resourceControl;

    public ServiceResourceFresher(RestTemplate restTemplate, DiscoveryClient discoveryClient,
        UserApiResourceMapping updateServiceApiMapping, ResourceControl resourceControl) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.updateServiceApiMapping = updateServiceApiMapping;
        this.resourceControl = resourceControl;
        this.serviceInstanceCache = CacheBuilder.newBuilder().build();
        this.successLoadServiceCache = CacheBuilder.newBuilder().build();
    }


    /**
     * 监听服务注册广播
     */
    @Override
    public void onApplicationEvent(HeartbeatEvent heartbeatEvent) {
        discoveryClient.getServices().stream()
            // 不是注册的资源
            .filter(s -> !resourceControl.isExcludeResourceService(s))
            // 获取所有服务的实例
            .map(s -> new ImmutablePair<>(s, discoveryClient.getInstances(s)))
            .filter(p -> !p.getRight().isEmpty())
            // 讲实例放入缓存
            .forEach(pair -> serviceInstanceCache.put(pair.getLeft(), pair.getRight()));
    }

    @Scheduled(fixedDelay = 10000)
    public void loadApi() {
        if (serviceInstanceCache.size() == 0) {
            return;
        }
        HashSet<String> serviceSet = new HashSet<>(serviceInstanceCache.asMap().keySet());
        for (String service : serviceSet) {
            List<ServiceInstance> instances = serviceInstanceCache.getIfPresent(service);
            serviceInstanceCache.invalidate(service);
            if (instances != null && !instances.isEmpty()) {
                updateApiInfo(instances.get(0));
            }
        }
    }

    /**
     * 更新服务接口权限编码
     */
    private void updateApiInfo(ServiceInstance service) {
        try {
            URI uri = successLoadServiceCache.getIfPresent(service.getServiceId());
            if (service.getUri().equals(uri)) {
                return;
            }
            String content =
                restTemplate.getForObject("http://" + service.getServiceId() + "/api/export", String.class);
            log.info("服务[{}]加载到接口信息 {}", service.getServiceId(), content);
            // 更新服务接口信息
            updateServiceApiMapping.updateServiceApiMapping(service.getServiceId(), content);
            successLoadServiceCache.put(service.getServiceId(), service.getUri());
        } catch (Exception e) {
            log.info("服务[{}]接口信息加载失败   {}", service.getServiceId(), e.getMessage());
        }
    }
}
