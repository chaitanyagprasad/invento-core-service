package com.chait.inventocoreservice.config.grpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * Registers gRPC server interceptors globally so they apply to every
 * incoming RPC without per-service wiring.
 */
@Configuration
public class GrpcServerConfig {

//    @Bean
//    @Order(0)
//
//    public TenantMetadataServerInterceptor tenantMetadataServerInterceptor() {
//        return new TenantMetadataServerInterceptor();
//    }
}