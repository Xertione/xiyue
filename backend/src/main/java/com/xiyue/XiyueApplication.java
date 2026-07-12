package com.xiyue;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 息悦生活家政平台后端启动类。
 */
@SpringBootApplication
@MapperScan("com.xiyue.**.mapper")
public class XiyueApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiyueApplication.class, args);
    }
}
