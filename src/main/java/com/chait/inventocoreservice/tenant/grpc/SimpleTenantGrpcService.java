package com.chait.inventocoreservice.tenant.grpc;

import com.chait.inventocore.tenant.grpc.*;
import com.chait.inventocoreservice.config.grpc.GrpcRequestContext;
import com.chait.inventocoreservice.tenant.exception.InvalidTenantStateException;
import com.chait.inventocoreservice.tenant.exception.TenantNotFoundException;
import com.chait.inventocoreservice.tenant.projection.TenantView;
import com.chait.inventocoreservice.tenant.service.TenantCommandService;
import com.chait.inventocoreservice.tenant.service.TenantQueryService;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class SimpleTenantGrpcService extends TenantServiceGrpc.TenantServiceImplBase {

    private final TenantCommandService commandService;
    private final TenantQueryService queryService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public void createTenant(
            CreateTenantRequest request,
            StreamObserver<CreateTenantResponse> responseObserver) {

        String callerUserId = GrpcRequestContext.USER_ID.get();
        log.info("gRPC createTenant — tenantId=[{}] caller=[{}]",
                request.getTenantId(), callerUserId);

        try {
            UUID id = commandService.createTenant(
                    request.getTenantId(),
                    request.getDisplayName());

            responseObserver.onNext(CreateTenantResponse.newBuilder()
                    .setId(id.toString())
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException ex) {
            log.warn("createTenant — tenant already exists: {}", ex.getMessage());
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (Exception ex) {
            log.error("createTenant — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error creating tenant.")
                    .asRuntimeException());
        }
    }

    @Override
    public void activateTenant(
            ActivateTenantRequest request,
            StreamObserver<ActivateTenantResponse> responseObserver) {

        log.info("gRPC activateTenant — id=[{}]", request.getId());

        try {
            commandService.activateTenant(UUID.fromString(request.getId()));

            responseObserver.onNext(ActivateTenantResponse.newBuilder().build());
            responseObserver.onCompleted();

        } catch (TenantNotFoundException ex) {
            log.warn("activateTenant — not found: {}", ex.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (InvalidTenantStateException ex) {
            log.warn("activateTenant — invalid state: {}", ex.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (Exception ex) {
            log.error("activateTenant — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error activating tenant.")
                    .asRuntimeException());
        }
    }

    @Override
    public void setTenantDbName(
            SetTenantDbNameRequest request,
            StreamObserver<SetTenantDbNameResponse> responseObserver) {

        log.info("gRPC setTenantDbName — id=[{}] dbName=[{}]",
                request.getId(), request.getDbName());

        try {
            commandService.setTenantDbName(
                    UUID.fromString(request.getId()),
                    request.getDbName());

            responseObserver.onNext(SetTenantDbNameResponse.newBuilder().build());
            responseObserver.onCompleted();

        } catch (TenantNotFoundException ex) {
            log.warn("setTenantDbName — not found: {}", ex.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (InvalidTenantStateException ex) {
            log.warn("setTenantDbName — invalid state: {}", ex.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (Exception ex) {
            log.error("setTenantDbName — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error setting db name.")
                    .asRuntimeException());
        }
    }

    @Override
    public void disableTenant(
            DisableTenantRequest request,
            StreamObserver<DisableTenantResponse> responseObserver) {

        log.info("gRPC disableTenant — id=[{}]", request.getId());

        try {
            commandService.disableTenant(UUID.fromString(request.getId()));

            responseObserver.onNext(DisableTenantResponse.newBuilder().build());
            responseObserver.onCompleted();

        } catch (TenantNotFoundException ex) {
            log.warn("disableTenant — not found: {}", ex.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (InvalidTenantStateException ex) {
            log.warn("disableTenant — invalid state: {}", ex.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (Exception ex) {
            log.error("disableTenant — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error disabling tenant.")
                    .asRuntimeException());
        }
    }

    @Override
    public void getTenantById(
            GetTenantByIdRequest request,
            StreamObserver<TenantViewProto> responseObserver) {

        log.debug("gRPC getTenantById — id=[{}]", request.getId());

        try {
            TenantView view = queryService.findById(
                    UUID.fromString(request.getId()));

            responseObserver.onNext(toProto(view));
            responseObserver.onCompleted();

        } catch (TenantNotFoundException ex) {
            log.warn("getTenantById — not found: {}", ex.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        } catch (Exception ex) {
            log.error("getTenantById — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error fetching tenant.")
                    .asRuntimeException());
        }
    }

    @Override
    public void getAllTenants(
            GetAllTenantsRequest request,
            StreamObserver<GetAllTenantsResponse> responseObserver) {

        log.debug("gRPC getAllTenants — page=[{}] size=[{}]",
                request.getPage(), request.getSize());

        try {
            int size = request.getSize() > 0
                    ? request.getSize()
                    : DEFAULT_PAGE_SIZE;

            Page<TenantView> page = queryService.findAll(
                    PageRequest.of(request.getPage(), size));

            List<TenantViewProto> protos = page.getContent()
                    .stream()
                    .map(this::toProto)
                    .toList();

            responseObserver.onNext(GetAllTenantsResponse.newBuilder()
                    .addAllTenants(protos)
                    .setTotalElements(page.getTotalElements())
                    .setTotalPages(page.getTotalPages())
                    .setPage(page.getNumber())
                    .setSize(page.getSize())
                    .build());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error("getAllTenants — unexpected error", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error fetching tenants.")
                    .asRuntimeException());
        }
    }

    private TenantViewProto toProto(TenantView view) {
        TenantViewProto.Builder builder = TenantViewProto.newBuilder()
                .setId(view.getId().toString())
                .setTenantId(view.getTenantId())
                .setDisplayName(view.getDisplayName())
                .setStatus(view.getStatus().name())
                .setCreatedAt(toTimestamp(view.getCreatedAt()))
                .setUpdatedAt(toTimestamp(view.getUpdatedAt()));

        if (view.getDbName() != null) {
            builder.setDbName(view.getDbName());
        }

        return builder.build();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
