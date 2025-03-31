package com.rishika.backend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class PipelineServerApplication {
    public static void main(String[] args) {

            SpringApplication.run(PipelineServerApplication.class, args);
        System.out.println("Pipeline server is running...");
    }
}