package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TranscriptListTest {

    private static final String VIDEO_ID = "dQw4w9WgXcQ";
    private TranscriptList transcriptList;

    @BeforeEach
    void setUp() {
        Map<String, Transcript> manualTranscripts = Map.of(
                "de", createTranscript("Deutsch", "de", false),
                "cs", createTranscript("cs", "cs", false)
        );
        Map<String, Transcript> generatedTranscripts = Map.of(
                "en", createTranscript("English", "en", true)
        );
        transcriptList = new TranscriptList(VIDEO_ID, manualTranscripts, generatedTranscripts, Map.of("af", "Afrikaans"));
    }

    private Transcript createTranscript(String language, String languageCode, boolean isGenerated) {
        YoutubeApi youtubeApi = mock(YoutubeApi.class);
        return new Transcript(
                youtubeApi,
                VIDEO_ID,
                "test",
                language,
                languageCode,
                isGenerated,
                Collections.emptyMap());
    }

    @Test
    void findTranscript_shouldUseEnglish_whenNoCodesGiven() throws Exception {
        Transcript transcript = transcriptList.findTranscript();

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
    }

    @Test
    void findTranscript_shouldFindTranscript_whenGivenSingleCode() throws Exception {
        Transcript transcript = transcriptList.findTranscript("de");

        assertThat(transcript.getLanguageCode()).isEqualTo("de");
    }

    @Test
    void findTranscript_shouldUseFirstCode_whenGivenMultipleLanguageCodes() throws Exception {
        Transcript transcript = transcriptList.findTranscript("de", "en");

        assertThat(transcript.getLanguageCode()).isEqualTo("de");
    }

    @Test
    void findTranscript_shouldUseSecondCode_whenGivenMultipleLanguageCodesAndFirstCodeIsNotAvailable() throws Exception {
        Transcript transcript = transcriptList.findTranscript("zz", "en");

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
    }

    @Test
    void findTranscript_shouldFindManuallyCreatedTranscript() throws Exception {
        Transcript manuallyCreatedTranscript = transcriptList.findTranscript("cs");

        assertThat(manuallyCreatedTranscript.getLanguageCode()).isEqualTo("cs");
        assertThat(manuallyCreatedTranscript.isGenerated()).isFalse();
    }

    @Test
    void findTranscript_shouldFindAutomaticallyGeneratedTranscript() throws Exception {
        Transcript generatedTranscript = transcriptList.findTranscript("en");

        assertThat(generatedTranscript.getLanguageCode()).isEqualTo("en");
        assertThat(generatedTranscript.isGenerated()).isTrue();
    }

    @Test
    void findTranscript_shouldThrowException_whenLanguageNotAvailable() {
        assertThatThrownBy(() -> transcriptList.findTranscript("zz"))
                .isInstanceOf(TranscriptRetrievalException.class)
                .hasMessageContaining("No transcripts were found");;
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findTranscript_shouldThrowException_whenGivenInvalidLanguageCodes(String languageCodes) {
        assertThatThrownBy(() -> transcriptList.findTranscript(languageCodes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findManualTranscript() throws Exception {
        Transcript transcript = transcriptList.findManualTranscript("cs");

        assertThat(transcript.getLanguageCode()).isEqualTo("cs");
        assertThat(transcript.isGenerated()).isFalse();
    }

    @Test
    void findGeneratedTranscript() throws Exception {
        assertThatThrownBy(() -> transcriptList.findGeneratedTranscript("cs"))
                .isInstanceOf(TranscriptRetrievalException.class);

        Transcript transcript = transcriptList.findGeneratedTranscript("en");

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
        assertThat(transcript.isGenerated()).isTrue();
    }

    @Test
    void toString_shouldBeFormattedCorrectly() {
        Map<String, Transcript> manualTranscripts = Map.of(
                "en", createTranscript("English", "en", false),
                "de", createTranscript("Deutsch", "de", false));
        Map<String, Transcript> generatedTranscripts = Map.of(
                "af", createTranscript("Afrikaans", "af", true),
                "cs", createTranscript("Czech", "cs", true));
        Map<String, String> translationLanguages = Map.of("en", "English", "de", "Deutsch");
        TranscriptList transcriptList = new TranscriptList(
                VIDEO_ID,
                manualTranscripts,
                generatedTranscripts,
                translationLanguages);

        String expected = """
                For video with ID (%s) transcripts are available in the following languages:
                Manually created: [de, en]
                Automatically generated: [af, cs]
                Available translation languages: [de, en]""".formatted(VIDEO_ID);

        assertThat(transcriptList.toString()).isEqualToNormalizingNewlines(expected);
    }
}