package io.github.thoroldvix;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptRetrievalExceptionTest {

    @Test
    void exceptionMessageBuiltCorrectly() {
        String videoId = "dQw4w9WgXcQ";
        TranscriptRetrievalException exception = new TranscriptRetrievalException(videoId, "Cause");

        String expected = "Could not retrieve transcript for the video: https://www.youtube.com/watch?v=%s.\nReason: Cause".formatted(videoId);

        String actual = exception.getMessage();

        assertThat(actual).isEqualTo(expected);
    }
}