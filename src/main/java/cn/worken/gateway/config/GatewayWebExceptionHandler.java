package cn.worken.gateway.config;

import cn.worken.gateway.config.constant.GatewayCode;
import cn.worken.gateway.config.exception.GatewayException;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.google.common.collect.ImmutableMap;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局异常处理
 *
 * @author shaoyijiong
 * @date 2020/7/6
 */
@Slf4j
public class GatewayWebExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * MessageReader
     */
    private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

    /**
     * MessageWriter
     */
    private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

    /**
     * ViewResolvers
     */
    private List<ViewResolver> viewResolvers = Collections.emptyList();

    /**
     * 存储处理异常后的信息
     */
    private final ThreadLocal<Map<String, Object>> exceptionHandlerResult = new ThreadLocal<>();

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
        Assert.notNull(messageReaders, "'messageReaders' must not be null");
        this.messageReaders = messageReaders;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setViewResolvers(List<ViewResolver> viewResolvers) {
        this.viewResolvers = viewResolvers;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
        Assert.notNull(messageWriters, "'messageWriters' must not be null");
        this.messageWriters = messageWriters;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 按照异常类型进行处理
        HttpStatus httpStatus;
        String body;
        if (ex instanceof NotFoundException) {
            // 服务未找到
            httpStatus = HttpStatus.NOT_FOUND;
            body = "服务维护中 请稍等...";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            httpStatus = responseStatusException.getStatus();
            body = responseStatusException.getMessage();
            if (httpStatus == HttpStatus.NOT_FOUND) {
                body = GatewayCode.API_NOT_EXIST.getMessage();
            }
        } else if (ex instanceof AuthenticationException) {
            // 鉴权失败
            httpStatus = HttpStatus.UNAUTHORIZED;
            body = "用户未认证!";
        } else if (ex instanceof GatewayException) {
            httpStatus = HttpStatus.valueOf(((GatewayException) ex).getCode());
            body = ex.getMessage();
        } else if (ex instanceof IllegalArgumentException) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            body = ex.getMessage();
        } else if (ex instanceof BlockException) {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS;
            body = "您的请求过快,请稍后再试!";
        } else {
            // 其他异常
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            body = "系统异常!";
        }
        //封装响应体,此body可修改为自己的jsonBody
        Map<String, Object> result = new HashMap<>(2, 1);
        // http响应码
        result.put("httpStatus", HttpStatus.OK);
        Map<String, Object> msg = ImmutableMap.of("code", httpStatus.value(), "message", body);
        // 实际响应内容
        result.put("body", msg);
        //错误记录
        ServerHttpRequest request = exchange.getRequest();
        // ip信息
        String ipAddress = getIpAddress(request);
        log.error("[全局异常处理]异常请求路径:{},记录异常信息:{},请求ip:{}", request.getPath(), ex.getMessage(), ipAddress, ex);
        // 参考AbstractErrorWebExceptionHandler
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        exceptionHandlerResult.set(result);
        ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
            .route(newRequest)
            .switchIfEmpty(Mono.error(ex))
            .flatMap((handler) -> handler.handle(newRequest))
            .flatMap((response) -> write(exchange, response));

    }

    private static String getIpAddress(ServerHttpRequest request) {
        //未知ip
        String unknown = "unknown";
        String ip = request.getHeaders().getFirst("X-Real-IP");
        if (!StringUtils.isBlank(ip) && !unknown.equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (!StringUtils.isBlank(ip) && !unknown.equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        } else {
            return Optional.ofNullable(request.getRemoteAddress()).map(InetSocketAddress::getHostString).orElse("");
        }
    }


    /**
     * 参考DefaultErrorWebExceptionHandler
     */
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> result = exceptionHandlerResult.get();
        exceptionHandlerResult.remove();
        return ServerResponse
            .status((HttpStatus) result.get("httpStatus"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(result.get("body")));
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
        exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
        return response.writeTo(exchange, new ResponseContext());
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private class ResponseContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return GatewayWebExceptionHandler.this.messageWriters;
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return GatewayWebExceptionHandler.this.viewResolvers;
        }

    }
}
