package com.xiyue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置：映射 /mock-uploads/** 到本地 uploads 目录，
 * 使模拟上传的图片可通过 HTTP 访问。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = System.getProperty("user.dir").replace("\\", "/") + "/uploads/";
        registry.addResourceHandler("/mock-uploads/**")
                .addResourceLocations("file:" + uploadsPath);
    }
}
