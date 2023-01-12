package cn.worken.gateway.config.constant;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/9 20:36
 */
public enum GatewayCode {
    //资源不存在
    API_NOT_EXIST(404, "访问资源不存在"),
    //访问受限
    ACCESS_DENY(403, "无权访问当前资源"),
    //认证失败
    AUTHENTICATION_FAILURE(401, "没有有效认证信息"),
    //系统繁忙
    APPLICATION_BUSY(500, "系统繁忙"),
    //表单重复提交
    REPEAT_SUBMIT(500, "表单重复提交");

    int code;
    String message;

    GatewayCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
