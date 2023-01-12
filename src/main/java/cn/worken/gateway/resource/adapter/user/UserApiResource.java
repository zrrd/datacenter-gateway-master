package cn.worken.gateway.resource.adapter.user;

import cn.worken.gateway.resource.ApiResource;
import lombok.Data;

/**
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Data
public class UserApiResource implements ApiResource {

    private String apiId;

    private String serviceName;

    private String resourceName;
}
