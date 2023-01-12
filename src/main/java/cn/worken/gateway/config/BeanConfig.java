package cn.worken.gateway.config;

import java.time.Duration;

import cn.worken.gateway.util.SnowflakeIdWorker;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Configuration
public class BeanConfig {


    private final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return restTemplateBuilder.setReadTimeout(Duration.ofSeconds(5)).setConnectTimeout(Duration.ofSeconds(3))
            // 针对http client做超时时间
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory();
                clientFactory.setConnectionRequestTimeout(3_000);
                clientFactory.setConnectTimeout(3_000);
                clientFactory.setReadTimeout(5_000);
                return clientFactory;
            }).build();
    }

    @Bean
    public SnowflakeIdWorker idWorker() {
        return new SnowflakeIdWorker(1, 1);
    }
}
