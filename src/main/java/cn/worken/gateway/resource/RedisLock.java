package cn.worken.gateway.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RedisLock {

    private static final ThreadLocal<SurvivalClamProcessor> threadLocal = new ThreadLocal();

    private final RedisTemplate redisTemplate;

    public RedisLock(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     *   获取分布式锁
     * * 原因是 redis是单线程的 一但一个线程获取锁后 其它线程就不能获取的锁
     * * @param lockKey 锁名称
     * * @param value   值
     * * @param expire  过期时间
     * * @param timeout 等待超时时间
     * * @return boolean 是否获取锁成功标识
     */
    public boolean getLock(String lockKey, String value, long expire, long timeout) {
        //获取当前毫米数
        long startTime = System.currentTimeMillis();
        try {
            while (true) {
                String script = "if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then  return redis.call('expire',KEYS[1],ARGV[2])  else return 0 end";
                RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
                List<String> keys = new ArrayList<>();
                keys.add(lockKey);
                Object execute = redisTemplate.execute(redisScript, keys, value, expire);
                if (1l == ((Long)execute).longValue()) {
                    startSurvivalClamProcessor(lockKey, value, expire);
                    return true;
                } else {
                    long endTime = System.currentTimeMillis();
                    if ((endTime - startTime) > timeout) {
                        log.info("已超过自旋上限时间，放弃自旋！");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.info("获取锁失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param lockKey 键
     * @param value 值
     * @return
     */
    public boolean releaseLock(String lockKey, String value) {
        releaseSurvivalClamProcessor();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<String> keys = new ArrayList<>();
        keys.add(lockKey);
        Long execute = (Long) redisTemplate.execute(redisScript, keys, value);
        return 1l == execute.longValue();
    }

    // 释放续期子线程  持有锁的线程已经处理完业务了，那么守护线程也应该被销毁
    public void releaseSurvivalClamProcessor() {
        if (threadLocal.get() != null) {
            threadLocal.get().stopThread();
            threadLocal.get().interrupt();
        }
        threadLocal.remove();
    }

    // 获取锁成功，并启用一个守护线程随时续期
    public void startSurvivalClamProcessor(String key, String value, long expire) {
        threadLocal.set(new SurvivalClamProcessor(key, value, expire));
        threadLocal.get().setDaemon(true);
        threadLocal.get().start();
    }

    class SurvivalClamProcessor extends Thread {

        // 续期成功标识
        private static final long REDIS_EXPIRE_SUCCESS = 1;

        // 键
        private String key;

        // 值
        private String value;

        // 续期时间
        private long expire;

        //线程关闭的标记
        private volatile Boolean signal;

        SurvivalClamProcessor(String key, String value, long expire) {
            this.key = key;
            this.value = value;
            this.expire = expire;
            this.signal = Boolean.TRUE;
        }

        void stopThread() {
            this.signal = Boolean.FALSE;
        }

        @Override
        public void run() {
            // 守护线程要在合理的时间再去重新设置锁的LockTime，否则会造成资源的浪费
            long waitTime = expire * 1000 * 2 / 3;
            while (signal) {
                try {
                    Thread.sleep(waitTime);
                    if (expandLockTime(key, value, expire) == REDIS_EXPIRE_SUCCESS) {
                        log.info("续期成功！，续期时间为{},键为{}，值为{}", expire, key, value);
                    } else {
                        // 有可能已经主动删除key,不需要在续期
                        this.stopThread();
                    }
                } catch (Exception e) {
                    log.info("续期线程执行失败:{}",e.getMessage());
                }
            }
        }

        /**
         *
         * @param key redis 键
         * @param value 值
         * @param expire 续期时间
         * @return
         */
        private long expandLockTime(String key, String value, long expire) {
            // 和释放锁的情况一致，我们需要先判断锁的对象是否没有变。否则会造成无论谁持有锁，守护线程都会去重新设置锁的LockTime
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1],ARGV[2]) else return '0' end";
            RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            List<String> keys = new ArrayList<>();
            keys.add(key);
            return (Long) redisTemplate.execute(redisScript, keys, value, expire);
        }
    }
}
