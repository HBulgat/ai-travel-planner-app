package top.bulgat.ai.travel.plan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.nls")
public class AliyunNlsConfig {
    private String appKey;
    private String accessKeyId;
    private String accessKeySecret;
    private String url = "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1";

    public boolean isValid() {
        return appKey != null && !appKey.isEmpty()
            && accessKeyId != null && !accessKeyId.isEmpty()
            && accessKeySecret != null && !accessKeySecret.isEmpty();
    }
}