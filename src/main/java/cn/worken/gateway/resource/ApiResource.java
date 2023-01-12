package cn.worken.gateway.resource;

import java.io.Serializable;

/**
 * @author shaoyijong
 * @version 1.0
 * @date 2019/4/4 12:37
 */
public interface ApiResource extends Serializable {

    String getApiId();

    String getServiceName();

    void setServiceName(String serviceName);

    String getResourceName();
}
