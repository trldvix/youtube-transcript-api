package io.github.thoroldvix.api;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

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

    @Mock(strictness = Mock.Strictness.LENIENT)
    protected YoutubeClient client;
    @InjectMocks
    protected YoutubeTranscriptApi youtubeTranscriptApi;

    protected void givenTranscriptContentXml() throws TranscriptRetrievalException, IOException {
        when(client.get(matches("https://www\\.youtube\\.com/api/timedtext\\?v=.*"), anyMap()))
                .thenReturn(Files.readString(Path.of(RESOURCE_PATH, "transcript.xml")));
    }


    protected static TranscriptContent getTranscriptContent() {
        return new TranscriptContent(List.of(new TranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new TranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new TranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)));
    }

    protected void givenInnertubeData(String innertubeData, String videoId) throws TranscriptRetrievalException {
        String expectedInnertubeRequest = """
                {
                "context":{
                    "client": {
                    "clientName": "ANDROID",
                    "clientVersion": "20.10.38"
                    }
                },
                    "videoId": "%s"
                }""".formatted(videoId);
        when(client.post(INNERTUBE_API_URL + "?key=" + INNERTUBE_API_KEY, expectedInnertubeRequest))
                .thenReturn(innertubeData);
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
