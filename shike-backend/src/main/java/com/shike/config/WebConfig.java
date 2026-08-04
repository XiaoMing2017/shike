package com.shike.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = new File("uploads").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path + File.separator);

        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**", "/api/v1/admin/**")
                .excludePathPatterns(
                    "/admin/login", "/api/v1/admin/login",
                    "/admin/check-auth", "/api/v1/admin/check-auth",
                    "/admin/index.html", "/api/v1/admin/index.html",
                    "/admin/*.css", "/admin/*.js", "/admin/*.png", "/admin/*.jpg"
                );
    }
}
