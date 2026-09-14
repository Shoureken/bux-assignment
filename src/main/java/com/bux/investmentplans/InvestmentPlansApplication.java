package com.bux.investmentplans;

import com.bux.investmentplans.config.InvestmentPlansProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InvestmentPlansProperties.class)
public class InvestmentPlansApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentPlansApplication.class, args);
    }
}
