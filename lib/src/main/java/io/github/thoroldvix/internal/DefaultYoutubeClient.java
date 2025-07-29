package io.github.thoroldvix.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

/**
 * Default implementation of {@link YoutubeClient}.
 */
final class DefaultYoutubeClient implements YoutubeClient {

    private final HttpClient httpClient;
    private static final String DEFAULT_ERROR_MESSAGE = "Request to YouTube failed.";

    DefaultYoutubeClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    DefaultYoutubeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(String url, Map<String, String> headers) throws TranscriptRetrievalException {
        String[] headersArray = createHeaders(headers);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        if (headersArray.length > 0) {
            requestBuilder.headers(headersArray);
        }

        return send(requestBuilder.build());
    }

    private String send(HttpRequest request) throws TranscriptRetrievalException {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TranscriptRetrievalException(DEFAULT_ERROR_MESSAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(DEFAULT_ERROR_MESSAGE, e);
        }

        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(DEFAULT_ERROR_MESSAGE + " Status code: " + response.statusCode());
        }

        return response.body();
    }

    @Override
    public String post(String url, String body) throws TranscriptRetrievalException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(url))
                .build();
        return send(request);
    }

    private String[] createHeaders(Map<String, String> headers) {
        String[] headersArray = new String[headers.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersArray[i++] = entry.getKey();
            headersArray[i++] = entry.getValue();
        }
        return headersArray;
    }
}
