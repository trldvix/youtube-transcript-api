package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
abstract class TranscriptRetrievalTest {

    protected static final String RESOURCE_PATH = "src/test/resources/";
    protected static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    protected static final String INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    protected static final String INNERTUBE_API_URL = "https://www.youtube.com/youtubei/v1/player";
    protected static final String YOUTUBE_HTML;
    protected static final String INNERTUBE_DATA;
    protected static final String VIDEO_ID = "dQw4w9WgXcQ";
    protected static final String TRANSCRIPT_XML;

    protected YoutubeClient client;
    protected YoutubeTranscriptApi youtubeTranscriptApi;

    @BeforeEach
    void setUp() {
        client = mock(YoutubeClient.class);
        youtubeTranscriptApi = new YoutubeTranscriptApi(client);
    }

    static {
        try {
            YOUTUBE_HTML = Files.readString(Path.of(RESOURCE_PATH, "pages/youtube.html.static"));
            INNERTUBE_DATA = Files.readString(Path.of(RESOURCE_PATH, "innertube/response.json.static"));
            TRANSCRIPT_XML = Files.readString(Path.of(RESOURCE_PATH, "transcript.xml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
