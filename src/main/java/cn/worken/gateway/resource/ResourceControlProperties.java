package cn.worken.gateway.resource;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2020/1/7 16:23
 */
@Slf4j
@Data
@Component
@RefreshScope
@ConfigurationProperties("resource")
public class ResourceControlProperties {

    private Integer version;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 不作为用户访问接口访问的服务
     */
    private Set<String> excludeResourceServiceList = new HashSet<>();

    /**
     * 白名单列表
     */
    private Set<String> whiteApiList = new HashSet<>();

    /**
     * 用户黑名单列表
     */
    private Set<String> blockApiList = new HashSet<>();


    @PostConstruct
    private void init() {
        // 从数据库中读出白名单列表
        whiteApiList = Sets.newHashSet(jdbcTemplate.queryForList("select api_uri from open_white_api", String.class));
        log.info("网关uri控制列表------>\nexcludeResourceServiceList:[{}]\nwhiteApiList:[{}]\nblockApiList:[{}]",
            excludeResourceServiceList, whiteApiList, blockApiList);
    }
}
