package cn.worken.gateway.config.exception;

import cn.worken.gateway.config.constant.GatewayCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/7 15:03
 */
@Getter
@Setter
public class GatewayException extends RuntimeException {

    private final int code;

    public GatewayException(GatewayCode code) {
        this(code.getCode(), code.getMessage());
    }

    public GatewayException(int code, String message) {
        super(message);
        this.code = code;
    }
}
