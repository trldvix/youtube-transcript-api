package io.github.thoroldvix.api;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a transcript cannot be retrieved for a specified video.
 * <p>
 * This exception encapsulates the details of the error encountered during the retrieval of a YouTube video transcript.
 * </p>
 */
public class TranscriptRetrievalException extends Exception {

    private static final String ERROR_MESSAGE = "Could not retrieve transcript for the video: %s.\nReason: %s";
    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    @Nullable
    private String videoId;

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param videoId The ID of the video for which the transcript retrieval failed.
     * @param message The detail message explaining the reason for the failure.
     * @param cause   The cause of the failure (which is saved for later retrieval by the {@link Throwable#getCause()} method).
     */
    public TranscriptRetrievalException(String videoId, String message, Throwable cause) {
        super(buildErrorMessage(videoId, message), cause);
        this.videoId = videoId;
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param videoId The ID of the video for which the transcript retrieval failed.
     * @param message The detail message explaining the reason for the failure.
     */
    public TranscriptRetrievalException(String videoId, String message) {
        super(buildErrorMessage(videoId, message));
        this.videoId = videoId;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message explaining the reason for the failure.
     * @param cause   The cause of the failure (which is saved for later retrieval by the {@link Throwable#getCause()} method).
     */
    public TranscriptRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message explaining the reason for the failure.
     */
    public TranscriptRetrievalException(String message) {
        super(message);
    }

    /**
     * Builds the error message to include the video URL and the specific cause of the error.
     *
     * @param videoId The ID of the video for which the transcript retrieval failed.
     * @param message The detail message explaining the reason for the failure.
     * @return The formatted error message.
     */
    private static String buildErrorMessage(String videoId, String message) {
        String videoUrl = YOUTUBE_WATCH_URL + videoId;
        return String.format(ERROR_MESSAGE, videoUrl, message);
    }

    /**
     * @return The ID of the video for which the transcript retrieval failed.
     */
    @Nullable
    public String getVideoId() {
        return videoId;
    }
}

