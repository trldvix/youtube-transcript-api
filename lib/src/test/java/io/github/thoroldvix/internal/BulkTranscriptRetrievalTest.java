package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


class BulkTranscriptRetrievalTest extends TranscriptRetrievalTest {
    private final String VIDEO_ID_1 = "8idr1WZ1A7Q";
    private final String VIDEO_ID_2 = "ZA4JkHKZM50";
    private static final String PLAYLIST_ID = "playlistId";
    private static String YOUTUBE_HTML;
    private static String PLAYLIST_SINGLE_PAGE;
    private static String CHANNEL_SEARCH_RESPONSE;
    private static String CHANNEL_RESPONSE;
    private static String API_RESPONSES_PATH;
    private static TranscriptRequest REQUEST;

    @BeforeAll
    static void beforeAll() throws IOException {
        API_RESPONSES_PATH = RESOURCE_PATH + "api_v3_responses";
        YOUTUBE_HTML = Files.readString(Path.of(RESOURCE_PATH, "pages/youtube.html.static"));
        PLAYLIST_SINGLE_PAGE = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_single_page.json"));
        CHANNEL_SEARCH_RESPONSE = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_search_response.json"));
        CHANNEL_RESPONSE = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_response.json"));
        REQUEST = new TranscriptRequest("apiKey", true);
    }


    @Test
    void listTranscriptsForPlaylist() throws Exception {
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenReturn(YOUTUBE_HTML);

        Map<String, TranscriptList> actual = youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("zh", "de", "hi", "ja", "ko", "es", "cs", "en");
        assertThat(actual.get(VIDEO_ID_2))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("zh", "de", "hi", "ja", "ko", "es", "cs", "en");
    }

    @Test
    void listTranscriptsForPlaylistContinueOnError() throws TranscriptRetrievalException {
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap()))
                .thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));


        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
        assertThatNoException()
                .isThrownBy(() -> youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID,
                        new TranscriptRequest("apiKey", false)));
    }

    @Test
    void listTranscriptsForPlaylistGetsNextPage(@Captor ArgumentCaptor<Map<String, String>> paramsCaptor) throws Exception {
        String firstPageResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_page_one.json"));
        String secondPageResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_page_two.json"));

        when(client.get(eq(PLAYLIST_ITEMS), paramsCaptor.capture()))
                .thenReturn(firstPageResponse)
                .thenReturn(secondPageResponse);
        when(client.get(anyString(), anyMap())).thenReturn(YOUTUBE_HTML);


        youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(paramsCaptor.getValue()).containsEntry("pageToken",
                "EAAajgFQVDpDQUVpRURJNE9VWTBRVFEyUkVZd1FUTXdSRElvQVVpN3BfbVAwY3FHQTFBQldrVWlRMmx" +
                "LVVZSR2NFbFZWVGxwVkRGa1ZWVlZVbEJoYlRGMlRURnJNbEZWVW5STlJrNXFWak" +
                "JHYzFKV2FHMU1WMXAzUldkM1NYRkxlVTl6ZDFsUkxVbFRkWGgzU1NJ");
    }

    @Test
    void listTranscriptsForPlaylistThrowsExceptionIfCannotParsePlaylistJson() throws TranscriptRetrievalException {
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelWithCookies(@Captor ArgumentCaptor<Map<String, String>> paramsCaptor) throws Exception {
        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), paramsCaptor.capture())).thenReturn(CHANNEL_RESPONSE);
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenReturn(YOUTUBE_HTML);

        Map<String, TranscriptList> actual = youtubeTranscriptApi.listTranscriptsForChannel("3Blue1Brown", REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(paramsCaptor.getValue()).containsEntry("id", "UCYO_jab_esuFRV4b17AJtAw");
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionWhenChannelNotFound() throws TranscriptRetrievalException, IOException {
        String searchNoMatchResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_search_no_match.json"));
        when(client.get(eq(SEARCH), anyMap())).thenReturn(searchNoMatchResponse);

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionIfCannotParseChannelSearchJson() throws Exception {
        when(client.get(eq(SEARCH), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionIfCannotParseChannelJson() throws Exception {
        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptsForPlaylist() throws Exception {
        String transcriptContentXml = Files.readString(Paths.get(RESOURCE_PATH, "transcript.xml"));
        DefaultTranscriptContent expected = new DefaultTranscriptContent(
                List.of(
                        new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0, 1.54),
                        new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                        new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)
                )

        );

        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap()))
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(transcriptContentXml);

        Map<String, TranscriptContent> actual = youtubeTranscriptApi.getTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }

    @Test
    void getTranscriptsForPlaylistStopOnError() throws TranscriptRetrievalException {
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));


        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);

        assertThatNoException()
                .isThrownBy(() -> youtubeTranscriptApi.getTranscriptsForPlaylist(PLAYLIST_ID,
                        new TranscriptRequest("apiKey", false)));
    }

    @Test
    void getTranscriptsForChannel() throws TranscriptRetrievalException, IOException {
        String transcriptContentXml = Files.readString(Paths.get("src/test/resources/", "transcript.xml"));

        DefaultTranscriptContent expected = new DefaultTranscriptContent(
                List.of(
                        new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0, 1.54),
                        new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                        new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)
                )

        );

        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), anyMap())).thenReturn(CHANNEL_RESPONSE);
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);

        when(client.get(anyString(), anyMap()))
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(transcriptContentXml);


        Map<String, TranscriptContent> actual = youtubeTranscriptApi.getTranscriptsForChannel("3Blue1Brown", REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }

}