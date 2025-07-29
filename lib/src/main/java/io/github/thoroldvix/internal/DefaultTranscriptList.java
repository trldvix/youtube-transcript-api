package io.github.thoroldvix.internal;
import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptList;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Default implementation of {@link TranscriptList}
 */
final class DefaultTranscriptList implements TranscriptList {

    private final String videoId;
    private final Map<String, Transcript> manualTranscripts;
    private final Map<String, Transcript> generatedTranscripts;
    private final Map<String, String> translationLanguages;

    DefaultTranscriptList(String videoId,
                          Map<String, Transcript> manualTranscripts,
                          Map<String, Transcript> generatedTranscripts,
                          Map<String, String> translationLanguages) {
        this.videoId = videoId;
        this.manualTranscripts = manualTranscripts;
        this.generatedTranscripts = generatedTranscripts;
        this.translationLanguages = translationLanguages;
    }

    private static String[] getDefault(String[] languageCodes) {
        return languageCodes.length == 0 ? new String[]{"en"} : languageCodes;
    }

    @SuppressWarnings("ConstantConditions")
    private static void validateLanguageCodes(String... languageCodes) {
        for (String languageCode : languageCodes) {
            if (languageCode == null) {
                throw new IllegalArgumentException("Language codes cannot be null");
            }
            if (languageCode.isBlank()) {
                throw new IllegalArgumentException("Language codes cannot be blank");
            }
        }
    }

    @Override
    public Transcript findTranscript(String... languageCodes) throws TranscriptRetrievalException {
        try {
            return findManualTranscript(languageCodes);
        } catch (TranscriptRetrievalException e) {
            return findGeneratedTranscript(languageCodes);
        }
    }

    @Override
    public Transcript findManualTranscript(String... languageCodes) throws TranscriptRetrievalException {
        return findTranscript(manualTranscripts, getDefault(languageCodes));
    }

    private Transcript findTranscript(Map<String, Transcript> transcripts, String... languageCodes) throws TranscriptRetrievalException {
        validateLanguageCodes(languageCodes);
        for (String languageCode : languageCodes) {
            if (transcripts.containsKey(languageCode)) {
                return transcripts.get(languageCode);
            }
        }
        throw new TranscriptRetrievalException(videoId, String.format("No transcripts were found for any of the requested language codes: %s. %s.", Arrays.toString(languageCodes), this));
    }

    @Override
    public Transcript findGeneratedTranscript(String... languageCodes) throws TranscriptRetrievalException {
        return findTranscript(generatedTranscripts, getDefault(languageCodes));
    }

    @Override
    public String getVideoId() {
        return videoId;
    }

    @Override
    public Iterator<Transcript> iterator() {
        return new Iterator<>() {
            private final Iterator<Transcript> manualIterator = manualTranscripts.values().iterator();
            private final Iterator<Transcript> generatedIterator = generatedTranscripts.values().iterator();

            @Override
            public boolean hasNext() {
                return manualIterator.hasNext() || generatedIterator.hasNext();
            }

            @Override
            public Transcript next() {
                if (manualIterator.hasNext()) {
                    return manualIterator.next();
                }
                return generatedIterator.next();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultTranscriptList that = (DefaultTranscriptList) o;
        return Objects.equals(videoId, that.videoId) && Objects.equals(manualTranscripts, that.manualTranscripts) && Objects.equals(generatedTranscripts, that.generatedTranscripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoId, manualTranscripts, generatedTranscripts);
    }

    @Override
    public String toString() {
        String template = "For video with ID (%s) transcripts are available in the following languages:\n" +
                          "Manually created: " +
                          "%s\n" +
                          "Automatically generated: " +
                          "%s\n" +
                          "Available translation languages: " +
                          "%s";

        return String.format(template,
                videoId,
                new TreeSet<>(manualTranscripts.keySet()),
                new TreeSet<>(generatedTranscripts.keySet()),
                new TreeSet<>(translationLanguages.keySet()));
    }
}
