package com.chait.inventocoreservice.config.grpc;

import io.grpc.Context;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * gRPC {@link Context} keys for caller identity forwarded by the BFF.
 *
 * <p>Populated by {@link TenantMetadataServerInterceptor} at the start
 * of each RPC call. Service handlers read from here rather than from
 * raw metadata so the metadata parsing logic stays in one place.
 *
 * <p>Values are never null inside a handler — the interceptor sets
 * sentinel values ({@code "anonymous"} / {@code "none"}) when a key
 * is absent from the metadata, matching the BFF's MDC sentinel pattern.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrpcRequestContext {

    public static final Context.Key<String> TENANT_ID =
            Context.key("tenant-id");

    public static final Context.Key<String> USER_ID =
            Context.key("x-user-id");
}
