package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranscriptTest {

    private YoutubeApi youtubeApi;
    private Transcript transcript;
    private static final String VIDEO_ID = "dQw4w9WgXcQ";
    private static final String API_URL = "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ";

    @BeforeEach
    void setUp() {
        youtubeApi = mock(YoutubeApi.class);
        transcript = new Transcript(
                youtubeApi,
                VIDEO_ID,
                API_URL,
                "English",
                "en",
                false,
                Map.of("af", "Afrikaans")
        );
    }

    @Test
    void fetch_shouldFetchTranscriptContent() throws Exception {
        String transcriptXml = Files.readString(Path.of("src/test/resources/transcript.xml"));
        when(youtubeApi.fetchTranscriptContentXml(VIDEO_ID, API_URL)).thenReturn(transcriptXml);

        List<TranscriptContent.Fragment> expected = List.of(new TranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new TranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new TranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239));

        TranscriptContent actual = transcript.fetch();

        assertThat(actual.getContent()).isEqualTo(expected);
    }

    @Test
    void fetch_shouldThrowException_whenYoutubeReturnsInvalidContentXml() throws Exception {
        when(youtubeApi.fetchTranscriptContentXml(VIDEO_ID, API_URL)).thenReturn("invalid xml");

        assertThatThrownBy(() -> transcript.fetch())
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("Failed to parse transcript content XML");
    }

    @Test
    void translate_shouldTranslateTranscript() throws Exception {
        Transcript translatedTranscript = transcript.translate("af");

        assertThat(translatedTranscript.getLanguageCode()).isEqualTo("af");
        assertThat(translatedTranscript.getApiUrl()).contains("&tlang=af");
    }

    @Test
    void translate_shouldThrowException_whenGivenLanguageCodeIsNotAvailable() {
        assertThatThrownBy(() -> transcript.translate("zz"))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("is not available");
    }

    @Test
    void isTranslatable_shouldReturnCorrectResult() {
        Transcript notTranslatableTranscript = new Transcript(
                youtubeApi,
                VIDEO_ID,
                API_URL,
                "English",
                "en",
                false,
                Collections.emptyMap()
        );
        assertThat(transcript.isTranslatable()).isTrue();
        assertThat(notTranslatableTranscript.isTranslatable()).isFalse();
    }

    @Test
    void translate_shouldThrowException_whenTranscriptIsNotTranslatable() {
        Transcript transcript = new Transcript(
                youtubeApi,
                VIDEO_ID,
                API_URL,
                "English",
                "en",
                false,
                Collections.emptyMap()
        );

        assertThat(transcript.isTranslatable()).isFalse();
        assertThatThrownBy(() -> transcript.translate("af"))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("transcript is not translatable");
    }

    @Test
    void toString_shouldBeFormattedCorrectly() {
        Transcript transcript = new Transcript(
                youtubeApi,
                VIDEO_ID,
                API_URL,
                "English",
                "en",
                false,
                Map.of("en", "English", "af", "Afrikaans")
        );

        String expected = """
                Transcript for video with id: %s.
                Language: English
                Language code: en
                API URL for retrieving content: %s
                Available translation languages: [af, en]""".formatted(VIDEO_ID, API_URL);

        assertThat(transcript.toString()).isEqualTo(expected);
    }
}