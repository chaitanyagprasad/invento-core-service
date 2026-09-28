package com.chait.inventocoreservice.config.grpc;

import io.grpc.Metadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared gRPC metadata key constants.
 *
 * <p>Keys must match exactly what {@code TenantForwardingClientInterceptor}
 * in the BFF writes. Mismatched key names silently produce empty values —
 * keeping them in one place prevents that class of bug.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrpcMetadataKeys {

    public static final Metadata.Key<String> TENANT_ID =
            Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> USER_ID =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
}
