package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TranscriptListTest {

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
        transcriptList = new TranscriptList("dQw4w9WgXcQ", manualTranscripts, generatedTranscripts, Map.of("af", "Afrikaans"));
    }

    private Transcript createTranscript(String language, String languageCode, boolean isGenerated) {
        YoutubeApi youtubeApi = mock(YoutubeApi.class);
        return new Transcript(
                youtubeApi,
                "dQw4w9WgXcQ",
                "test",
                language,
                languageCode,
                isGenerated,
                Collections.emptyMap());
    }

    @Test
    void findTranscriptNoCodeUsesEnglish() throws Exception {
        Transcript transcript = transcriptList.findTranscript();

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
    }

    @Test
    void findTranscriptSingleLanguageCode() throws Exception {
        Transcript transcript = transcriptList.findTranscript("de");

        assertThat(transcript.getLanguageCode()).isEqualTo("de");
    }

    @Test
    void findTranscriptMultipleLanguageCodesFirstCodeIsUsed() throws Exception {
        Transcript transcript = transcriptList.findTranscript("de", "en");

        assertThat(transcript.getLanguageCode()).isEqualTo("de");
    }

    @Test
    void findTranscriptMultipleGetLanguageCodesSecondCodeIsUsedIfFirstCodeNotAvailable() throws Exception {
        Transcript transcript = transcriptList.findTranscript("zz", "en");

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
    }

    @Test
    void findTranscriptFindsManuallyCreated() throws Exception {
        Transcript manuallyCreatedTranscript = transcriptList.findTranscript("cs");

        assertThat(manuallyCreatedTranscript.getLanguageCode()).isEqualTo("cs");
        assertThat(manuallyCreatedTranscript.isGenerated()).isFalse();
    }

    @Test
    void findTranscriptFindsGenerated() throws Exception {
        Transcript generatedTranscript = transcriptList.findTranscript("en");

        assertThat(generatedTranscript.getLanguageCode()).isEqualTo("en");
        assertThat(generatedTranscript.isGenerated()).isTrue();
    }

    @Test
    void findTranscriptThrowsExceptionWhenLanguageNotAvailable() {
        assertThatThrownBy(() -> transcriptList.findTranscript("zz"))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findTranscriptWhenInvalidGetLanguageCodesThrowsException(String languageCodes) {
        assertThatThrownBy(() -> transcriptList.findTranscript(languageCodes))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void findManuallyCreated() throws Exception {
        Transcript transcript = transcriptList.findManualTranscript("cs");

        assertThat(transcript.getLanguageCode()).isEqualTo("cs");
        assertThat(transcript.isGenerated()).isFalse();
    }

    @Test
    void findManuallyCreatedNoCodeUsesEnglish() throws Exception {
        TranscriptList transcriptList = new TranscriptList("dQw4w9WgXcQ",
                Map.of("en", createTranscript("English", "en", false),
                        "de", createTranscript("Deutsch", "de", false)),
                Map.of(),
                Map.of());

        Transcript transcript = transcriptList.findManualTranscript();

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
        assertThat(transcript.isGenerated()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findManuallyCreatedWhenInvalidGetLanguageCodesThrowsException(String languageCodes) {
        assertThatThrownBy(() -> transcriptList.findManualTranscript(languageCodes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findGenerated() throws Exception {
        assertThatThrownBy(() -> transcriptList.findGeneratedTranscript("cs"))
                .isInstanceOf(TranscriptRetrievalException.class);

        Transcript transcript = transcriptList.findGeneratedTranscript("en");

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
        assertThat(transcript.isGenerated()).isTrue();
    }

    @Test
    void findGeneratedNoCodeUsesEnglish() throws Exception {
        TranscriptList transcriptList = new TranscriptList("dQw4w9WgXcQ",
                Map.of(),
                Map.of("en", createTranscript("English", "en", true),
                        "de", createTranscript("Deutsch", "de", true)),
                Map.of());
        Transcript transcript = transcriptList.findGeneratedTranscript();

        assertThat(transcript.getLanguageCode()).isEqualTo("en");
        assertThat(transcript.isGenerated()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findGeneratedWithInvalidGetLanguageCodesThrowsException(String languageCodes) {
        assertThatThrownBy(() -> transcriptList.findGeneratedTranscript(languageCodes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringFormattedCorrectly() {
        Map<String, Transcript> manualTranscripts = Map.of(
                "en", createTranscript("English", "en", false),
                "de", createTranscript("Deutsch", "de", false));
        Map<String, Transcript> generatedTranscripts = Map.of(
                "af", createTranscript("Afrikaans", "af", true),
                "cs", createTranscript("Czech", "cs", true));
        Map<String, String> translationLanguages = Map.of("en", "English", "de", "Deutsch");
        TranscriptList transcriptList = new TranscriptList(
                "dQw4w9WgXcQ",
                manualTranscripts,
                generatedTranscripts,
                translationLanguages);

        String expected = """
                For video with ID (dQw4w9WgXcQ) transcripts are available in the following languages:
                Manually created: [de, en]
                Automatically generated: [af, cs]
                Available translation languages: [de, en]""";

        assertThat(transcriptList.toString()).isEqualToNormalizingNewlines(expected);
    }
}