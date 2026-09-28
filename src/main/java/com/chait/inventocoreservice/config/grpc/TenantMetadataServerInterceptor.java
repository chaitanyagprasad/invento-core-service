package com.chait.inventocoreservice.config.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * gRPC server interceptor that reads BFF-forwarded caller identity from
 * incoming metadata and populates {@link GrpcRequestContext}.
 *
 * <p>The BFF is the authentication authority — this service never validates
 * a JWT. It trusts {@code tenant-id} and {@code x-user-id} unconditionally
 * because only the BFF can reach this service's gRPC port (network policy).
 *
 * <p>Absent metadata keys receive sentinel values rather than null so
 * handlers can always read a non-null value from context:
 * <ul>
 *   <li>{@code tenant-id} absent → {@code "none"}</li>
 *   <li>{@code x-user-id} absent → {@code "anonymous"}</li>
 * </ul>
 *
 * <p>Registered globally in {@link GrpcServerConfig}.
 */
@GlobalServerInterceptor
public class TenantMetadataServerInterceptor implements ServerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(TenantMetadataServerInterceptor.class);

    private static final String SENTINEL_TENANT = "none";
    private static final String SENTINEL_USER   = "anonymous";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String tenantId = headers.get(GrpcMetadataKeys.TENANT_ID);
        String userId   = headers.get(GrpcMetadataKeys.USER_ID);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Incoming gRPC call [{}] missing tenant-id metadata.",
                    call.getMethodDescriptor().getFullMethodName());
            tenantId = SENTINEL_TENANT;
        }

        if (userId == null || userId.isBlank()) {
            log.warn("Incoming gRPC call [{}] missing x-user-id metadata.",
                    call.getMethodDescriptor().getFullMethodName());
            userId = SENTINEL_USER;
        }

        log.debug("gRPC call [{}] — tenant={} user={}",
                call.getMethodDescriptor().getFullMethodName(),
                tenantId, userId);

        Context context = Context.current()
                .withValue(GrpcRequestContext.TENANT_ID, tenantId)
                .withValue(GrpcRequestContext.USER_ID,   userId);

        return Contexts.interceptCall(context, call, headers, next);
    }
}
