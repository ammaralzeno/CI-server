package ci.notify.http;

/**
 * Minimal HTTP response container for notifier operations.
 */
public final class HttpResponseData {
    private final int statusCode;
    private final String body;

    public HttpResponseData(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }
}