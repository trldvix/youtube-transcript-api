package io.github.thoroldvix.api;

import java.util.*;

/**
 * Represents a list of all available transcripts for a YouTube video.
 * <p>
 * This interface provides methods to iterate over all available transcripts for a given YouTube video, and to find either generated or manual transcripts for a specific language.
 * Individual transcripts are represented by {@link Transcript} objects.
 * Instances of {@link TranscriptList} can be obtained through the {@link YoutubeTranscriptApi} class.
 * </p>
 */
public class TranscriptList implements Iterable<Transcript> {

    private final String videoId;
    private final Map<String, Transcript> manualTranscripts;
    private final Map<String, Transcript> generatedTranscripts;
    private final Map<String, String> translationLanguages;

    TranscriptList(String videoId,
                   Map<String, Transcript> manualTranscripts,
                   Map<String, Transcript> generatedTranscripts,
                   Map<String, String> translationLanguages) {
        this.videoId = videoId;
        this.manualTranscripts = manualTranscripts;
        this.generatedTranscripts = generatedTranscripts;
        this.translationLanguages = translationLanguages;
    }

    static TranscriptList empty() {
        return new TranscriptList("", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    boolean isEmpty() {
        return videoId.isBlank()
               && manualTranscripts.isEmpty()
               && generatedTranscripts.isEmpty()
               && translationLanguages.isEmpty();
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

    /**
     * Searches for a transcript using the provided language codes.
     * Manually created transcripts are prioritized, and if none are found, generated transcripts are used.
     * If you only want generated or manually created transcripts, use {@link #findGeneratedTranscript(String...)} or {@link #findManualTranscript(String...)} instead.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
    public Transcript findTranscript(String... languageCodes) throws TranscriptRetrievalException {
        try {
            return findManualTranscript(languageCodes);
        } catch (TranscriptRetrievalException e) {
            return findGeneratedTranscript(languageCodes);
        }
    }

    /**
     * Searches for a manually created transcript using the provided language codes.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
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

    /**
     * Searches for an automatically generated transcript using the provided language codes.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
    public Transcript findGeneratedTranscript(String... languageCodes) throws TranscriptRetrievalException {
        return findTranscript(generatedTranscripts, getDefault(languageCodes));
    }

    /**
     * Retrieves the ID of the video to which transcript was retrieved.
     *
     * @return The video ID.
     */
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
        TranscriptList that = (TranscriptList) o;
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
