package cn.worken.gateway.resource;

import cn.worken.gateway.util.GatewayUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenAPIListServerWebExchangeMatcher {

    private static final List<String> OPEN_API_LIST = Arrays.asList("/business-center-backend/open-api/sms/idea/msg-sending-list");

    public static boolean testOpenAPIList(ServerWebExchange exchange) {
        return OPEN_API_LIST.contains(GatewayUtils.getRawPath(exchange));
    }

}
