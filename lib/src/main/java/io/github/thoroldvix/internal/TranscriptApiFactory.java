package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YoutubeTranscriptApi;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for creating instances of {@link YoutubeTranscriptApi}.
 */
public final class TranscriptApiFactory {

    private TranscriptApiFactory() {
    }

    /**
     * Creates a new instance of {@link YoutubeTranscriptApi} using the default YouTube client.
     *
     * @return A new instance of {@link YoutubeTranscriptApi}
     */
    public static YoutubeTranscriptApi createDefault() {
        return createWithClient(new DefaultYoutubeClient());
    }

    /**
     * Creates a new instance of {@link YoutubeTranscriptApi} using the specified {@link YoutubeClient}.
     *
     * @param client The {@link YoutubeClient} to be used for YouTube interactions
     * @return A new instance of {@link YoutubeTranscriptApi}
     */
    public static YoutubeTranscriptApi createWithClient(YoutubeClient client) {
        return new DefaultYoutubeTranscriptApi(client);
    }
}
