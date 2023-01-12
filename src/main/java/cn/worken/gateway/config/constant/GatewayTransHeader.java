package cn.worken.gateway.config.constant;

/**
 * 服务流转添加 header
 *
 * @author shaoyijiong
 * @date 2020/7/7
 */
public interface GatewayTransHeader {

    String X_GATEWAY_AUTHENTICATION_INFO = "X-GatewayAuthenticationInfo";

    /**
     * 请求账户唯一标识 , 如果是普通用户请求使用 username ; 如果是 client 请求使用 client_id
     */
    String X_IDENTIFIES= "X_IDENTIFIES";

    String X_OPENAPI= "X_OPENAPI";
}
