package ci.notify;

import ci.notify.http.HttpResponseData;
import ci.notify.http.HttpSender;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test helper that records the last POST request.
 */
final class RecordingHttpSender implements HttpSender {

    final AtomicReference<String> url = new AtomicReference<>();
    final AtomicReference<Map<String, String>> headers = new AtomicReference<>();
    final AtomicReference<String> body = new AtomicReference<>();

    private final int statusCode;

    RecordingHttpSender(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public HttpResponseData postJson(String url, Map<String, String> headers, String jsonBody) {
        this.url.set(url);
        this.headers.set(headers);
        this.body.set(jsonBody);
        return new HttpResponseData(statusCode, "{}");
    }
}
