package ci.notify.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HttpSender implementation using Java 11+ HttpClient.
 */
public final class JavaHttpSender implements HttpSender {

    private final HttpClient client;

    public JavaHttpSender() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public JavaHttpSender(HttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponseData postJson(String url, Map<String, String> headers, String jsonBody)
            throws IOException, InterruptedException {

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));

        if (headers != null) {
            headers.forEach(req::header);
        }

        HttpResponse<String> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
        return new HttpResponseData(resp.statusCode(), resp.body());
    }
}
