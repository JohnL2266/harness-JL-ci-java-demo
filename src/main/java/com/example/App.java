package com.example;

// ------------------------------------------------------------
// Imports
// ------------------------------------------------------------

// Lightweight HTTP server included in the JDK (no Spring/Tomcat)
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

// Core Java imports
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class App {

    // ------------------------------------------------------------
    // Runtime / build metadata (typically injected by CI/CD)
    // ------------------------------------------------------------

    private final String serviceName =
            System.getenv().getOrDefault("SERVICE_NAME", "harness-ci-lab");

    private final String version =
            System.getenv().getOrDefault("APP_VERSION", "dev");

    private final String gitSha =
            System.getenv().getOrDefault("GIT_SHA", "unknown");

    // Pod name (in K8s, HOSTNAME = pod name; otherwise "local")
    private final String podName =
            System.getenv().getOrDefault("HOSTNAME", "local");

    // Unique ID per running instance (useful in K8s / multi-replica scenarios)
    private final String instanceId = UUID.randomUUID().toString();

    // Start time (used for uptime)
    private final Instant startedAt = Instant.now();

    // ------------------------------------------------------------
    // Feature-flag–like runtime mode (optional global default)
    // ------------------------------------------------------------

    // Volatile ensures visibility across threads
    private volatile String mode = "normal";

    // ------------------------------------------------------------
    // In-memory metrics and counters
    // ------------------------------------------------------------

    // Total HTTP requests handled
    private final AtomicLong requestsTotal = new AtomicLong(0);

    // Visitor counter specifically for /greet (per running instance)
    private final AtomicLong greetVisitors = new AtomicLong(0);

    // Per-endpoint request counters
    private final Map<String, AtomicLong> requestsByPath =
            new ConcurrentHashMap<>();

    // ------------------------------------------------------------
    // Application entry point
    // ------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        new App().start();
    }

    // ------------------------------------------------------------
    // HTTP server startup and route wiring
    // ------------------------------------------------------------

    public void start() throws Exception {

        // Use PORT env var if present (nice for containers); default to 8080
        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        // Create server bound to port
        HttpServer server =
                HttpServer.create(new InetSocketAddress(port), 0);

        // --------------------------------------------------------
        // Routes
        // --------------------------------------------------------

        // Root: interactive dashboard
        server.createContext("/", ex ->
                trackAndRespond(ex, 200, "text/html; charset=utf-8", dashboardHtml())
        );

        // Liveness probe (machine endpoint)
        server.createContext("/healthz", ex ->
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", "ok\n")
        );

        // Readiness probe (machine endpoint)
        server.createContext("/readyz", ex ->
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", "ready\n")
        );

        // Runtime/build info (used by dashboard + useful in deployments)
        server.createContext("/version", ex ->
                trackAndRespond(ex, 200, "application/json; charset=utf-8", versionJson())
        );

        // Prometheus-ish metrics (used by dashboard)
        server.createContext("/metrics", ex ->
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", metricsText())
        );

        // Greeting endpoint: contextual + increments visitor counter
        server.createContext("/greet", ex -> {
            // Name from query string, URL-decoded (handles spaces)
            String name = queryParamDecoded(ex, "name", "World");

            // Requested greeting mode per request; fall back to current global mode
            String requestedMode = queryParam(ex, "mode", mode);

            // Increment per-instance visitor counter
            long visitorNumber = greetVisitors.incrementAndGet();

            // Build contextual greeting response
            String body = buildGreeting(name, visitorNumber, requestedMode);

            trackAndRespond(ex, 200, "text/plain; charset=utf-8", body);
        });

        // Chaos endpoint: simulate failures for testing K8s self-healing
        server.createContext("/chaos", ex -> {
            String action = queryParam(ex, "action", "");

            if ("enable".equals(action)) {
                // Enable chaos mode (app continues running but in degraded state)
                mode = "chaos";
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", "Chaos mode enabled\n");

            } else if ("disable".equals(action)) {
                // Disable chaos mode (return to normal)
                mode = "normal";
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", "Chaos mode disabled\n");

            } else if ("crash".equals(action)) {
                // Simulate a crash (K8s will restart the pod)
                trackAndRespond(ex, 200, "text/plain; charset=utf-8", "Crashing in 2 seconds...\n");
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception ignored) {}
                    System.exit(1);
                }).start();

            } else {
                // Default: return 503 Service Unavailable (triggers health check failures)
                trackAndRespond(ex, 503, "text/plain; charset=utf-8", "Service degraded (chaos mode)\n");
            }
        });

        // Default executor is fine for a demo
        server.setExecutor(null);

        // Start accepting requests
        server.start();

        System.out.println("Server started on port " + port);
    }

    // ------------------------------------------------------------
    // Core greeting logic
    // ------------------------------------------------------------

    // Builds a contextual greeting message
    private String buildGreeting(String name, long visitorNumber, String requestedMode) {
        long uptimeSeconds = Duration.between(startedAt, Instant.now()).getSeconds();

        boolean pirate = "pirate".equalsIgnoreCase(requestedMode);

        String opening = pirate
                ? "Ahoy, " + name + "! 🏴‍☠️"
                : "Hello, " + name + "! 👋";

        // Multi-line, readable "verification-style" output for demos
        return opening + "\n"
                + "You are visitor #" + visitorNumber + "\n"
                + "Service: " + serviceName + "\n"
                + "Greeting Mode: " + requestedMode + "\n"
                + "App Mode: " + mode + "\n"
                + "Version: " + version + "\n"
                + "Git SHA: " + gitSha + "\n"
                + "Pod: " + podName + "\n"
                + "Instance: " + instanceId.substring(0, 8) + "\n"
                + "Uptime: " + uptimeSeconds + "s\n";
    }

    // ------------------------------------------------------------
    // Public helper for unit tests (Option 2)
    // ------------------------------------------------------------

    // Allows tests to validate greeting behavior without HTTP wiring
    public String getGreetingForTest(String name, String requestedMode) {
        // Visitor number is not important for unit tests, keep deterministic
        return buildGreeting(name, 1, requestedMode);
    }

    // ------------------------------------------------------------
    // Version endpoint payload
    // ------------------------------------------------------------

    private String versionJson() {
        long uptime = Duration.between(startedAt, Instant.now()).getSeconds();

        return String.format(
                "{\"service\":\"%s\",\"version\":\"%s\",\"gitSha\":\"%s\",\"podName\":\"%s\",\"instanceId\":\"%s\",\"uptimeSeconds\":%d,\"mode\":\"%s\"}\n",
                serviceName, version, gitSha, podName, instanceId, uptime, mode
        );
    }

    // ------------------------------------------------------------
    // Metrics output
    // ------------------------------------------------------------

    private String metricsText() {
        long uptime = Duration.between(startedAt, Instant.now()).getSeconds();

        StringBuilder sb = new StringBuilder();
        sb.append("service_uptime_seconds ").append(uptime).append("\n");
        sb.append("requests_total ").append(requestsTotal.get()).append("\n");
        sb.append("greet_visitors_total ").append(greetVisitors.get()).append("\n");

        for (Map.Entry<String, AtomicLong> e : requestsByPath.entrySet()) {
            // Skip the browser favicon noise to keep output clean
            if ("/favicon.ico".equals(e.getKey())) continue;

            sb.append("requests_by_path{path=\"")
              .append(e.getKey())
              .append("\"} ")
              .append(e.getValue().get())
              .append("\n");
        }

        return sb.toString();
    }

    // ------------------------------------------------------------
    // HTML dashboard (Java 11 compatible, interactive, animated badge)
    // ------------------------------------------------------------

    private String dashboardHtml() {
        return "<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "  <meta charset=\"utf-8\"/>\n"
            + "  <title>Harness CI Lab</title>\n"
            + "  <style>\n"
            + "    body { font-family: Arial, sans-serif; margin: 24px; }\n"
            + "    .card { padding: 16px; border: 1px solid #ddd; border-radius: 10px; margin-bottom: 12px; }\n"
            + "    code { background: #f6f6f6; padding: 2px 6px; border-radius: 6px; }\n"
            + "    input, select { padding: 8px; border-radius: 8px; border: 1px solid #ccc; margin-right: 8px; }\n"
            + "    button { padding: 8px 12px; border-radius: 8px; border: 1px solid #4f46e5; background: #4f46e5; color: #fff; cursor: pointer; }\n"
            + "    button.danger { background: #dc2626; border-color: #dc2626; }\n"
            + "    pre { background:#fafafa; border:1px solid #eee; border-radius:8px; padding:10px; overflow-x:auto; }\n"
            + "    .row { margin: 6px 0; }\n"
            + "    .label { display:inline-block; width: 120px; color:#444; }\n"
            + "    .value { font-weight: 600; }\n"
            + "    .badge {\n"
            + "      position: fixed; right: 20px; bottom: 20px;\n"
            + "      background: #22c55e; color: #fff;\n"
            + "      padding: 10px 14px; border-radius: 999px;\n"
            + "      font-weight: bold;\n"
            + "      box-shadow: 0 4px 10px rgba(0,0,0,0.2);\n"
            + "      animation: bounce 1.8s infinite;\n"
            + "    }\n"
            + "    @keyframes bounce {\n"
            + "      0%,100% { transform: translateY(0); }\n"
            + "      50% { transform: translateY(-10px); }\n"
            + "    }\n"
            + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <h2>🚀 Harness CI Lab</h2>\n"
            + "\n"
            + "  <div class=\"card\">\n"
            + "    <div><b>Endpoints:</b>\n"
            + "      <code>/healthz</code>\n"
            + "      <code>/readyz</code>\n"
            + "      <code>/version</code>\n"
            + "      <code>/metrics</code>\n"
            + "      <code>/greet?name=John&mode=pirate</code>\n"
            + "      <code>/chaos?action=crash</code>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "\n"
            + "  <div class=\"card\">\n"
            + "    <b>Try it</b>\n"
            + "    <div style=\"margin-top:10px;\">\n"
            + "      <input id=\"name\" type=\"text\" placeholder=\"Your name\" value=\"John\" />\n"
            + "      <select id=\"mode\">\n"
            + "        <option value=\"normal\">Normal</option>\n"
            + "        <option value=\"pirate\">Pirate 🏴‍☠️</option>\n"
            + "      </select>\n"
            + "      <button onclick=\"greet()\">Greet me</button>\n"
            + "    </div>\n"
            + "    <pre id=\"greeting\" style=\"margin-top:12px;\">Click \"Greet me\" to generate a greeting...</pre>\n"
            + "  </div>\n"
            + "\n"
            + "  <div class=\"card\">\n"
            + "    <b>Build / Runtime</b>\n"
            + "    <div id=\"version\" style=\"margin-top:10px;\">Loading...</div>\n"
            + "  </div>\n"
            + "\n"
            + "  <div class=\"card\">\n"
            + "    <b>Metrics</b>\n"
            + "    <pre id=\"metrics\">Loading...</pre>\n"
            + "  </div>\n"
            + "\n"
            + "  <div class=\"card\">\n"
            + "    <b>Chaos Engineering (K8s Demo)</b>\n"
            + "    <div style=\"margin-top:10px;\">\n"
            + "      <button class=\"danger\" onclick=\"chaos('crash')\">💥 Crash Pod</button>\n"
            + "      <button class=\"danger\" onclick=\"chaos('enable')\">⚠️ Enable Chaos</button>\n"
            + "      <button onclick=\"chaos('disable')\">✅ Disable Chaos</button>\n"
            + "    </div>\n"
            + "    <pre id=\"chaos\" style=\"margin-top:12px;\">Chaos controls allow testing K8s self-healing...</pre>\n"
            + "  </div>\n"
            + "\n"
            + "  <div class=\"badge\" id=\"badge\">Visitor #–</div>\n"
            + "\n"
            + "<script>\n"
            + "async function refresh() {\n"
            + "  const v = JSON.parse(await (await fetch('/version')).text());\n"
            + "  document.getElementById('version').innerHTML =\n"
            + "    '<div class=\"row\"><span class=\"label\">Service:</span> <span class=\"value\">' + v.service + '</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">Version:</span> <span class=\"value\">' + v.version + '</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">Git SHA:</span> <span class=\"value\">' + v.gitSha + '</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">Pod Name:</span> <span class=\"value\">' + v.podName + '</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">Instance:</span> <span class=\"value\">' + v.instanceId + '</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">Uptime:</span> <span class=\"value\">' + v.uptimeSeconds + 's</span></div>' +\n"
            + "    '<div class=\"row\"><span class=\"label\">App Mode:</span> <span class=\"value\">' + v.mode + '</span></div>';\n"
            + "\n"
            + "  document.getElementById('metrics').textContent = await (await fetch('/metrics')).text();\n"
            + "}\n"
            + "\n"
            + "async function greet() {\n"
            + "  const name = encodeURIComponent(document.getElementById('name').value || 'World');\n"
            + "  const mode = document.getElementById('mode').value;\n"
            + "  const text = await (await fetch('/greet?name=' + name + '&mode=' + mode)).text();\n"
            + "  document.getElementById('greeting').textContent = text;\n"
            + "  const match = text.match(/visitor #(\\d+)/);\n"
            + "  if (match) document.getElementById('badge').textContent = 'Visitor #' + match[1];\n"
            + "  await refresh();\n"
            + "}\n"
            + "\n"
            + "async function chaos(action) {\n"
            + "  const text = await (await fetch('/chaos?action=' + action)).text();\n"
            + "  document.getElementById('chaos').textContent = text;\n"
            + "  await refresh();\n"
            + "}\n"
            + "\n"
            + "setInterval(refresh, 2000);\n"
            + "refresh();\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";
    }

    // ------------------------------------------------------------
    // Shared HTTP response helper (writes response + tracks metrics)
    // ------------------------------------------------------------

    private void trackAndRespond(HttpExchange ex, int status, String contentType, String body)
            throws IOException {

        // Total request count
        requestsTotal.incrementAndGet();

        // Per-path request count
        String path = ex.getRequestURI().getPath();
        requestsByPath
                .computeIfAbsent(path, k -> new AtomicLong(0))
                .incrementAndGet();

        // Write HTTP response
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ------------------------------------------------------------
    // Query string helpers
    // ------------------------------------------------------------

    // Basic query param (no decoding)
    private static String queryParam(HttpExchange ex, String key, String defaultVal) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return defaultVal;

        for (String part : q.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return defaultVal;
    }

    // URL-decoded query param (supports spaces and special characters)
    private static String queryParamDecoded(HttpExchange ex, String key, String defaultVal) {
        try {
            // Decode with UTF-8 (Java 11 safe)
            return URLDecoder.decode(queryParam(ex, key, defaultVal), "UTF-8");
        } catch (Exception e) {
            return defaultVal;
        }
    }
}