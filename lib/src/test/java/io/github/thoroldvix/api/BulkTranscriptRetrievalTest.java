package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class BulkTranscriptRetrievalTest extends TranscriptRetrievalTest {
    private final String VIDEO_ID_1 = "8idr1WZ1A7Q";
    private final String VIDEO_ID_2 = "ZA4JkHKZM50";
    private static final String NEXT_PAGE_TOKEN = "nextPageToken";
    private final static String YOUTUBE_API_V3_BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String PLAYLIST_ID = "playlistId";
    private static final String API_KEY = "apiKey";
    private static final String PLAYLIST_SINGLE_PAGE;
    private static final String CHANNEL_SEARCH_RESPONSE;
    private static final String CHANNEL_RESPONSE;
    private static final String API_RESPONSES_PATH = RESOURCE_PATH + "api_v3_responses";
    ;
    private static final BulkTranscriptRequest REQUEST = new BulkTranscriptRequest(API_KEY, true);

    @BeforeEach
    void setUp() throws Exception {
        when(client.get(matches("playlistItems.*"))).thenReturn(PLAYLIST_SINGLE_PAGE);
        givenVideoPageHtml(VIDEO_ID_1);
        givenVideoPageHtml(VIDEO_ID_2);
        givenInnertubeData(INNERTUBE_DATA, VIDEO_ID_1);
        givenInnertubeData(Files.readString(Path.of(RESOURCE_PATH, "innertube/response2.json.static")), VIDEO_ID_2);
    }

    static {
        try {
            PLAYLIST_SINGLE_PAGE = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_single_page.json"));
            CHANNEL_SEARCH_RESPONSE = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_search_response.json"));
            CHANNEL_RESPONSE = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_response.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void givenVideoPageHtml(String videoId) throws Exception {
        when(client.get(YOUTUBE_WATCH_URL + videoId, Map.of("Accept-Language", "en-US"))).thenReturn(YOUTUBE_HTML);
    }

    @Test
    void listTranscriptsForPlaylist_shouldReturnTranscriptsForVideosInPlaylist_whenThereIsSinglePageWithVideoIds() throws Exception {
        Map<String, TranscriptList> actual = youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("cs", "hi", "de", "ko", "ja", "en", "es", "zh", "en");
        assertThat(actual.get(VIDEO_ID_2))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("zz", "hi", "de", "ko", "ja", "en", "es", "zh", "en");
    }

    @Test
    void listTranscriptsForPlaylist_shouldThrowException_whenStopOnErrorIsTrueAndTranscriptRetrievalFails() throws Exception {
        when(client.get(anyString(), anyMap())).thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));
        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForPlaylist_shouldIgnoreFailedTranscripts_whenStopOnErrorIsFalseAndTranscriptRetrievalFails() throws Exception {
        //stub only for the first video id, so retrieval for the second id fails
        Mockito.reset(client);
        when(client.get(matches("playlistItems.*playlistId=%s.*".formatted(PLAYLIST_ID)))).thenReturn(PLAYLIST_SINGLE_PAGE);
        givenVideoPageHtml(VIDEO_ID_1);
        givenInnertubeData(INNERTUBE_DATA, VIDEO_ID_1);

        Map<String, TranscriptList> result = youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, new BulkTranscriptRequest(API_KEY, false));
        assertThat(result.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1);
    }

    @Test
    void listTranscriptsForPlaylist_shouldReturnTranscriptsForVideosInPlaylist_whenThereIsMultipleVideoIdPages() throws Exception {
        String firstPageResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_page_one.json"));
        String secondPageResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "playlist_page_two.json"));

        when(client.get(matches("playlistItems.*"))).thenReturn(firstPageResponse);
        when(client.get(matches("playlistItems.*pageToken=%s.*".formatted(NEXT_PAGE_TOKEN)))).thenReturn(firstPageResponse)
                .thenReturn(secondPageResponse);

        Map<String, TranscriptList> actual = youtubeTranscriptApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("cs", "hi", "de", "ko", "ja", "en", "es", "zh", "en");
        assertThat(actual.get(VIDEO_ID_2))
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("zz", "hi", "de", "ko", "ja", "en", "es", "zh", "en");
    }

    @Test
    void listTranscriptsForChannel() throws Exception {
        String channelName = "3Blue1Brown";
        String channelId = "UCYO_jab_esuFRV4b17AJtAw";

        when(client.get(matches("search.*q=%s.*".formatted(channelName)))).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(matches("channels.*"))).thenReturn(CHANNEL_RESPONSE);
        when(client.get(matches("playlistItems.*playlistId=%s.*".formatted(channelId)))).thenReturn(PLAYLIST_SINGLE_PAGE);

        Map<String, TranscriptList> actual = youtubeTranscriptApi.listTranscriptsForChannel(channelName, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
    }

    private void givenChannelSearchResponse(String channelName, String channelSearchResponse) throws TranscriptRetrievalException {
        String channelIdSearchUrl = (YOUTUBE_API_V3_BASE_URL + "search?q=%s&type=channel&key=%s&part=snippet").formatted(channelName, API_KEY);
        when(client.get(channelIdSearchUrl)).thenReturn(channelSearchResponse);
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionWhenChannelNotFound() throws TranscriptRetrievalException, IOException {
        String searchNoMatchResponse = Files.readString(Paths.get(API_RESPONSES_PATH, "channel_search_no_match.json"));
        String channelName = "3Blue1Brown";
        givenChannelSearchResponse(channelName, searchNoMatchResponse);

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("Could not find channel with the name: " + channelName);
    }

    @Test
    void getTranscriptsForPlaylist() throws Exception {
        TranscriptContent expected = getTranscriptContent();
        givenTranscriptContentXml();

        Map<String, TranscriptContent> actual = youtubeTranscriptApi.getTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }

    @Test
    void getTranscriptsForChannel() throws Exception {
        TranscriptContent expected = getTranscriptContent();

        when(client.get(matches("search.*"))).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(matches("channels.*"))).thenReturn(CHANNEL_RESPONSE);
        when(client.get(matches("playlistItems.*"))).thenReturn(PLAYLIST_SINGLE_PAGE);
        givenTranscriptContentXml();

        Map<String, TranscriptContent> actual = youtubeTranscriptApi.getTranscriptsForChannel("3Blue1Brown", REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }
}