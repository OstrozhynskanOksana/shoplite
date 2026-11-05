package com.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of ShopLite, a simple online store.
 * Architecture: layered monolith (controller -> service -> repository), see docs/architecture.md.
 */
@SpringBootApplication
public class ShopLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopLiteApplication.class, args);
    }
}
