package io.github.thoroldvix.api;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class SingleVideoTranscriptRetrievalTest extends TranscriptRetrievalTest {

    private void givenVideoPageHtml(String html) throws Exception {
        when(client.get(YOUTUBE_WATCH_URL + VIDEO_ID, Map.of("Accept-Language", "en-US"))).thenReturn(html);
    }

    private void givenVideoPageHtmlFromFile(String fileName) throws Exception {
        String html = Files.readString(Path.of(RESOURCE_PATH, fileName));
        givenVideoPageHtml(html);
    }

    @Test
    void listTranscripts() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeData(INNERTUBE_DATA, VIDEO_ID);

        TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts(VIDEO_ID);

        assertThat(transcriptList)
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("cs", "hi", "de", "ko", "ja", "en", "es", "zh", "en");
    }

    private void givenInnertubeDataFromFile(String fileName) throws Exception {
        String innertubeData = Files.readString(Path.of(RESOURCE_PATH, fileName));
        givenInnertubeData(innertubeData, VIDEO_ID);
    }

    @Test
    void listTranscripts_shouldThrowException_whenVideoPageContainsConsentPage() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_consent_page.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("age restricted");
    }

    @Test
    void listTranscripts_shouldThrowException_whenVideoPageContainsCaptcha() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_too_many_requests.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("captcha detected");
    }

    @Test
    void listTranscripts_shouldThrowException_whenInnertubeRequestIsBlocked() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/request_blocked.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("blocking requests");
    }

    @Test
    void listTranscripts_shouldThrowException_whenVideoIsUnplayable() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/unplayable.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("Video is unplayable");
    }

    @Test
    void listTranscripts_shouldThrowException_whenTranscriptsAreDisabled() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/transcripts_disabled.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("Transcripts are disabled");

        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/transcripts_disabled2.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("Transcripts are disabled");
    }

    @Test
    void listTranscripts_shouldThrowException_whenVideoIsUnavailable() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/unavailable.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("video is unavailable");
    }

    @Test
    void listTranscripts_shouldThrowException_whenVideoIsAgeRestricted() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeDataFromFile("innertube/age_restricted.json.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("age restricted");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "short", "with spaces", "over11characterslong"})
    void listTranscripts_shouldThrowException_whenGivenInvalidVideoId(String invalidId) {
        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid video ID");
    }

    @Test
    void getTranscript() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);
        givenInnertubeData(INNERTUBE_DATA, VIDEO_ID);
        givenTranscriptContentXml();

        TranscriptContent expected = getTranscriptContent();

        TranscriptContent actual = youtubeTranscriptApi.getTranscript(VIDEO_ID);

        assertThat(actual).isEqualTo(expected);
    }
}