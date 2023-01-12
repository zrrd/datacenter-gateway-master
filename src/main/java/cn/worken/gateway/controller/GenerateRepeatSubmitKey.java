package cn.worken.gateway.controller;

import cn.worken.gateway.util.SnowflakeIdWorker;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/actuator/")
public class GenerateRepeatSubmitKey {

    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdWorker idWorker;

    public GenerateRepeatSubmitKey(StringRedisTemplate stringRedisTemplate, SnowflakeIdWorker idWorker){
        this.stringRedisTemplate = stringRedisTemplate;
        this.idWorker = idWorker;
    }

    @RequestMapping("submit-key")
    public String generateRepeatSubmitKey() {
        String key = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + idWorker.nextStrId();
        stringRedisTemplate.opsForValue().set(key, key, 5 * 60, TimeUnit.SECONDS);
        return key;
    }
}
