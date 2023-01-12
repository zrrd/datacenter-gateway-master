package cn.worken.gateway.filter;


import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.exception.GatewayException;
import cn.worken.gateway.resource.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关验证表单重读提交
 *
 * @author jianghua
 * @date 2021/05/07
 */
@Component
@Slf4j
public class ValidateSubmitKeyFilter implements GlobalFilter, Ordered {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisLock redisLock;

    public ValidateSubmitKeyFilter(StringRedisTemplate stringRedisTemplate, RedisLock redisLock){
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisLock = redisLock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String repeatSubmitKey = request.getQueryParams().getFirst("repeatSubmitKey");
        log.info("获取到的repeatSubmitKey请求参数是{}", repeatSubmitKey);
        // 没有传参数key  则直接放行
        if (StringUtils.isEmpty(repeatSubmitKey)) {
            return chain.filter(exchange);
        }
        boolean flag = redisLock.getLock("LOCK" + repeatSubmitKey, repeatSubmitKey, 4, 5000);
        if (flag) {
            log.info("获取锁成功！");
            try {
                String value = stringRedisTemplate.opsForValue().get(repeatSubmitKey);
                // 第一次提交会删除掉key  如果重复提交  redis没有key  则不通过校验
                if (repeatSubmitKey.equals(value)) {
                    stringRedisTemplate.delete(repeatSubmitKey);
                    return chain.filter(exchange);
                }
            } catch (Exception e) {
                log.info("执行失败！");
            } finally {
                boolean releaseFlag = redisLock.releaseLock("LOCK" + repeatSubmitKey, repeatSubmitKey);
                if (releaseFlag) {
                    log.info("释放锁成功！");
                } else {
                    log.info("释放锁失败！");
                }
            }
        } else {
            throw new GatewayException(GatewayCode.APPLICATION_BUSY);
        }
        throw new GatewayException(GatewayCode.REPEAT_SUBMIT);
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrders.RESOURCE_ACCESS.getOrder();
    }
}
