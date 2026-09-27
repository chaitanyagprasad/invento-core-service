# Local OpenTelemetry

The `local` Spring profile exports application traces and Micrometer metrics to
the Grafana LGTM stack using OTLP over HTTP. Logs are not exported by this
configuration.

## Start the observability backend

From the project root, start Docker Desktop (or another Docker daemon), then run:

```sh
docker compose up -d otel-lgtm
```

The stack exposes Grafana at <http://localhost:3000> and OTLP receivers on
`localhost:4317` (gRPC) and `localhost:4318` (HTTP). Grafana's default local
credentials are `admin` / `admin`.

## Run the application

Keep the LGTM container running and start the service with the `local` profile:

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

The local profile samples every trace and sends trace and metric data to
`localhost:4318`. Metric export is configured for a 10-second interval.
Other profiles do not use these local OTLP settings.

## View telemetry

In Grafana, open **Explore** and select **Tempo** to search for traces or
**Prometheus** to query metrics. Generate a few requests to the service and
allow at least one metric export interval for data to appear. The service
resource name is `invento-core-service`.

To stop the backend:

```sh
docker compose down
```

The named `lgtm-data` volume retains backend data between restarts. To remove
that telemetry data as well, run `docker compose down --volumes`.
