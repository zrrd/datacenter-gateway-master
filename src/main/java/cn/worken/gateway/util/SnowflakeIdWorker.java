package cn.worken.gateway.util;

import org.apache.commons.lang3.StringUtils;

public class SnowflakeIdWorker {
    // ==============================Fields===========================================
    /**
     * 开始时间截 (2015-01-01)
     */
    public static final long twepoch = 1420041600000L;

    /**
     * 机器id所占的位数
     */
    public static final long workerIdBits = 5L;

    /**
     * 数据标识id所占的位数
     */
    public static final long datacenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    public static final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据标识id，结果是31
     */
    public static final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 序列在id中占的位数
     */
    public static final long sequenceBits = 12L;

    /**
     * 机器ID向左移12位
     */
    public static final long workerIdShift = sequenceBits;

    /**
     * 数据标识id向左移17位(12+5)
     */
    public static final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间截向左移22位(5+5+12)
     */
    public static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    public static final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    protected long workerId = -1;

    /**
     * 数据中心ID(0~31)
     */
    protected long dataCenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    /**
     * 时间戳偏移量
     */
    private long timestampOffset = 0L;

    //==============================Constructors=====================================


    public SnowflakeIdWorker(long dataCenterId) {
        if (dataCenterId > maxDatacenterId || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.dataCenterId = dataCenterId;
    }

    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public SnowflakeIdWorker(long workerId, long dataCenterId) {
        this(dataCenterId);
        this.updateWorkerId(workerId);
    }

    /**
     * @param workerId      工作ID (0~31)
     * @param dataCenterId  数据中心ID (0~31)
     * @param baseTimestamp 基准时间(用于计算偏移量)
     */
    public SnowflakeIdWorker(long workerId, long dataCenterId, long baseTimestamp) {
        this(workerId, dataCenterId);
        this.updateTimestampOffset(baseTimestamp);
    }

    protected void updateTimestampOffset(long baseTimestamp) {
        if (baseTimestamp < twepoch) {
            throw new IllegalArgumentException("base timestamp is limited minimal value is " + twepoch);
        }
        this.timestampOffset = System.currentTimeMillis() - baseTimestamp;
    }

    protected void updateWorkerId(long workerId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }

        this.workerId = workerId;
    }

    // ==============================Methods==========================================

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized Long nextId() {

        if (workerId < 0) {
            throw new RuntimeException("Illegal workerId [" + workerId + "] please check");
        }

        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - timestampOffset - twepoch) << timestampLeftShift)
                //数据中心参数位移量
                | (dataCenterId << datacenterIdShift)
                //工作节点位移量
                | (workerId << workerIdShift)
                //有序序列
                | sequence;
    }

    public String nextStrId() {
        String nextId = Long.toString(nextId());
        if (nextId.length() < 19) {
            return StringUtils.leftPad(nextId, 19, "0");
        }
        return nextId;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    //==============================Test=============================================

    public static class SnowflakeIdWorkerException extends RuntimeException {
        public SnowflakeIdWorkerException(String message) {
            super(message);
        }
    }
}
