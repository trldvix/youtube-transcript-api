package io.github.thoroldvix.api;

/**
 * Responsible for creating instances of {@link YoutubeTranscriptApi}.
 */
public class TranscriptApiFactory {

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
        return new YoutubeTranscriptApi(client);
    }
}
