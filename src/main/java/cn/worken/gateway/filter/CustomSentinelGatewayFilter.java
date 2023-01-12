package cn.worken.gateway.filter;

import cn.worken.gateway.config.constant.GatewayTransHeader;
import cn.worken.gateway.config.constant.ReqContextConstant;
import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.google.common.collect.Sets;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * 限流
 *
 * @author shaoyijiong
 * @date 2020/8/4
 */
@Configuration
public class CustomSentinelGatewayFilter {

    @Primary
    @Bean
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter(GlobalFilterOrders.SENTINEL.getOrder());
    }

    /**
     * 注册限流规则  通过手动配置  gateway的限流 dashboard暂时不支持
     */
    @PostConstruct
    public void doInit() {
        //自定义api限流规则
        initCustomizedApi();
        //网关限流的规则
        initGatewayRules();
    }

    /**
     * 定义api 拦截资源
     */
    private void initCustomizedApi() {
        //拦截所有api  兜底操作 如果要自定义配置在上面单独创建一个item
        ApiPathPredicateItem baseApiItem = new ApiPathPredicateItem().setPattern("/**")
                .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_PREFIX);
        //定义资源名
        ApiDefinition baseApi = new ApiDefinition("baseApi")
                .setPredicateItems(Sets.newHashSet(baseApiItem));

        Set<ApiDefinition> definitions = new HashSet<>();
        definitions.add(baseApi);
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /**
     * 设置资源拦截规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        //对应上面的api组 资源名称，可以是网关中的 route 名称或者用户自定义的 API 分组名称
        //通用接口 根据登陆信息限流
        rules.add(new GatewayFlowRule("baseApi")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                //3秒窗口期 最多20次访问
                .setCount(20).setIntervalSec(3)
                .setParamItem(new GatewayParamFlowItem()
                        //根据请求头限流 -> token 解析的用户信息 根据登陆信息限流
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                        //解析token后塞入的用户名
                        .setFieldName(ReqContextConstant.X_IDENTIFIES)
                )
        );

        rules.add(new GatewayFlowRule("baseApi")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                //3秒窗口期 最多20次访问
                .setCount(1000).setIntervalSec(3)
                .setParamItem(new GatewayParamFlowItem()
                        //根据请求头限流 -> token 解析的用户信息 根据登陆信息限流
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                        //解析token后塞入的用户名
                        .setFieldName(GatewayTransHeader.X_OPENAPI)
                )
        );

        //登陆接口 根据IP 限流
        rules.add(new GatewayFlowRule("com-login")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                //相同IP 每5秒最多1次访问
                .setCount(1).setIntervalSec(5).setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                        //根据IP限流  防止多个nginx跳转 使用 X-Forwarded-For 获得IP
                        .setFieldName("X-Forwarded-For")));
        GatewayRuleManager.loadRules(rules);
    }

}
