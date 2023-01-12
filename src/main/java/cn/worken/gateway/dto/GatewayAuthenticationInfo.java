package cn.worken.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登陆人用户信息
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Data
@Builder
public class GatewayAuthenticationInfo {

    /**
     * 用户id
     */
    private String userId;
    /**
     * 用户登陆名
     */
    private String username;
    /**
     * 组织id
     */
    private String comId;
    /**
     * 客户端id
     */
    private String clientId;
    /**
     * 用户类型
     */
    private String userType;
    /**
     * 用户名称
     */
    private String name;
    /**
     * 登陆所属服务
     */
    private String server;

    private String loginType;

    private Long productId;

    private String confProductCode;
}
