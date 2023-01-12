package cn.worken.gateway.controller;

import cn.worken.gateway.resource.adapter.client.ClientResourceJdbcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RefreshCache {

    @Autowired
    private ClientResourceJdbcAdapter clientResourceJdbcAdapter;

    @RequestMapping("refresh-client-cache")
    public Mono<String> refreshClientCache(String key) {
        clientResourceJdbcAdapter.clearCache(key);
        return Mono.just("success");
    }
}
