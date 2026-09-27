package com.chait.inventocoreservice;

import org.springframework.boot.SpringApplication;

public class TestInventoCoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(InventoCoreServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
