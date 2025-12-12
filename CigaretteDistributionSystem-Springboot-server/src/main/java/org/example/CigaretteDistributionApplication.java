package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@org.mybatis.spring.annotation.MapperScan({ "org.example.mapper"})
public class CigaretteDistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CigaretteDistributionApplication.class, args);
    }
}
