package cn.worken.gateway;

import cn.worken.gateway.util.RSAUtils;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.junit.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.util.ResourceUtils;
import reactor.core.publisher.Mono;

/**
 * @author shaoyijiong
 * @date 2020/7/4
 */
public class GoTest {

    //@Test
    public void abc() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        File pubFile = ResourceUtils.getFile("classpath:pub.key");
        String pubString = CharStreams.toString(new InputStreamReader(new FileInputStream(pubFile)));
        NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder =
            new NimbusReactiveJwtDecoder(RSAUtils.getPublicKey(pubString));
        Mono<Jwt> decode = nimbusReactiveJwtDecoder.decode(
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX3R5cGUiOm51bGwsInVzZXJfaWQiOjEsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJjb21faWQiOiIxIiwibmFtZSI6IuS9muWQjSIsImV4cCI6MTU5Mzg1MzE4OCwianRpIjoiNzE2MDU4MTAtNGE4MC00MmZjLWFmZjUtZTQwMmNiZDhhMjhhIiwiY2xpZW50X2lkIjoiY2xpZW50In0.Z8E1Rqm0GIwGwv7QjPa9kY1tQNlxu5N6NknRNnmRRU_F6OkfPXpDQWh22kDiU_SJ6NlYzirsGi9NpOmPO-mFrxFBEVgtBaYFRTWtDkSwgT4OUFYNd4n80hFUwkkxAvSR4FsBhEmew6JYvM-jBUOMNXuFkYO2kEfZXVyliWh1UP0");
        System.out.println(decode.block());
    }
}
