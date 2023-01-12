package cn.worken.gateway.resource.adapter.client;

import cn.worken.gateway.resource.ApiResource;
import lombok.Data;

/**
 * @author shaoyijiong
 * @date 2020/7/7
 */
@Data
public class ClientApiResource implements ApiResource {
    
    private String apiId;
    private String serviceName;
    private String resourceName;

}
