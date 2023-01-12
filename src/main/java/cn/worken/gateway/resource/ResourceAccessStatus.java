package cn.worken.gateway.resource;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/4 12:44
 */
@Getter
@Setter
public class ResourceAccessStatus implements Serializable {

    /**
     * 是否通过
     */
    @JSONField(serialize = false)
    private boolean access;
    /**
     * 拒绝响应码
     */
    private int denyCode;
    /**
     * 拒绝消息
     */
    private String denyMsg;


    public static ResourceAccessStatus accessSuccess() {
        ResourceAccessStatus resourceAccessStatus = new ResourceAccessStatus();
        resourceAccessStatus.setAccess(true);
        return resourceAccessStatus;
    }

    public static ResourceAccessStatus accessFail(int denyCode, String denyMsg) {
        ResourceAccessStatus resourceAccessStatus = new ResourceAccessStatus();
        resourceAccessStatus.setAccess(false);
        resourceAccessStatus.setDenyCode(denyCode);
        resourceAccessStatus.setDenyMsg(denyMsg);
        return resourceAccessStatus;
    }
}
