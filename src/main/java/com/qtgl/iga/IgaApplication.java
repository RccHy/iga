package com.qtgl.iga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IgaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgaApplication.class, args);
    }

}
