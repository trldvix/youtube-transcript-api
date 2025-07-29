package io.github.thoroldvix.api;

/**
 * Request object for retrieving transcripts.
 * <p>
 * Contains API key required for the YouTube V3 API,
 * and optionally a file path to the text file containing the authentication cookies. If cookies are not provided, the API will not be able to access age restricted videos.
 * Also contains a flag to stop on error, or continue on error. Defaults to false if not provided.
 * </p>
 * </p>
 */
public class TranscriptRequest {
    private final String apiKey;
    private final boolean stopOnError;

    /**
     * Creates a new instance of {@link TranscriptRequest}
     *
     * @param apiKey      API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     * @param stopOnError Whether to stop if transcript retrieval fails for a video. If false, all transcripts that could not be retrieved will be skipped,
     *                    *                        otherwise an exception will be thrown on first error.
     */
    public TranscriptRequest(String apiKey, boolean stopOnError) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        this.apiKey = apiKey;
        this.stopOnError = stopOnError;
    }

    public TranscriptRequest(String apiKey) {
        this(apiKey, true);
    }

    /**
     * @return API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * @return Whether to stop if transcript retrieval fails for a video. If false, all transcripts that could not be retrieved will be skipped,
     * *                        otherwise an exception will be thrown on first error.
     */
    public boolean isStopOnError() {
        return stopOnError;
    }
}
