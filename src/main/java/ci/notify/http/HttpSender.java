package ci.notify.http;

import java.io.IOException;
import java.util.Map;

/**
 * Thin abstraction over HTTP so notifiers can be unit-tested without real network calls.
 */
public interface HttpSender {

    /**
     * Executes an HTTP POST with JSON body.
     *
     * @param url absolute URL
     * @param headers request headers
     * @param jsonBody JSON payload
     * @return response data
     * @throws IOException on transport failure
     * @throws InterruptedException if interrupted
     */
    HttpResponseData postJson(String url, Map<String, String> headers, String jsonBody)
            throws IOException, InterruptedException;
}