package com.xushu.webtable.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人网盘接口文档")
                        .version("1.0")
                        .description("Spring Boot 项目 API 说明")
                        .contact(new Contact()
                                .name("易宸钰")
                                .email("3388364100@qq.com")));
    }
}
