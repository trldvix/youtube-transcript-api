package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YoutubeTranscriptApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
abstract class TranscriptRetrievalTest {

    protected static final String RESOURCE_PATH = "src/test/resources/";
    protected static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    protected YoutubeClient client;
    protected YoutubeTranscriptApi youtubeTranscriptApi;

    @BeforeEach
    void setUp() {
        client = mock(YoutubeClient.class);
        youtubeTranscriptApi = new DefaultYoutubeTranscriptApi(client);
    }
}
