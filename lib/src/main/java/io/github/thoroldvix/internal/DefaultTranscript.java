package io.github.thoroldvix.internal;


import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.TranscriptRetrievalException;

import java.util.*;

/**
 * Default implementation of {@link Transcript}.
 */
final class DefaultTranscript implements Transcript {

    private final YoutubeApi youtubeApi;
    private final String videoId;
    private final String apiUrl;
    private final String language;
    private final String languageCode;
    private final boolean isGenerated;
    private final Map<String, String> translationLanguages;
    private final boolean isTranslatable;

    DefaultTranscript(YoutubeApi youtubeApi,
                      String videoId,
                      String apiUrl,
                      String language,
                      String languageCode,
                      boolean isGenerated,
                      Map<String, String> translationLanguages) {
        this.youtubeApi = youtubeApi;
        this.videoId = videoId;
        this.apiUrl = apiUrl;
        this.language = language;
        this.languageCode = languageCode;
        this.isGenerated = isGenerated;
        this.translationLanguages = translationLanguages;
        this.isTranslatable = !translationLanguages.isEmpty();
    }

    @Override
    public TranscriptContent fetch() throws TranscriptRetrievalException {
        String transcriptXml = youtubeApi.fetchTranscriptContentXml(videoId, apiUrl);
        return TranscriptContentExtractor.extract(videoId, transcriptXml);
    }

    @Override
    public Transcript translate(String languageCode) throws TranscriptRetrievalException {
        checkIfPossibleToTranslate(languageCode);
        return new DefaultTranscript(
                youtubeApi,
                videoId,
                createTranslationApiUrl(languageCode),
                translationLanguages.get(languageCode),
                languageCode,
                isGenerated,
                translationLanguages
        );
    }

    private void checkIfPossibleToTranslate(String languageCode) throws TranscriptRetrievalException {
        if (!isTranslatable) {
            throw new TranscriptRetrievalException(videoId, "This transcript is not translatable");
        }
        if (!translationLanguages.containsKey(languageCode)) {
            throw new TranscriptRetrievalException(videoId, String.format("Translation language '%s' is not available", languageCode));
        }
    }

    private String createTranslationApiUrl(String languageCode) {
        return String.format("%s&tlang=%s", apiUrl, languageCode);
    }

    @Override
    public String getVideoId() {
        return videoId;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public Set<String> getTranslationLanguages() {
        return Collections.unmodifiableSet(translationLanguages.keySet());
    }

    @Override
    public boolean isTranslatable() {
        return isTranslatable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultTranscript that = (DefaultTranscript) o;
        return isGenerated == that.isGenerated && isTranslatable == that.isTranslatable &&
               Objects.equals(youtubeApi, that.youtubeApi) && Objects.equals(videoId, that.videoId) &&
               Objects.equals(apiUrl, that.apiUrl) && Objects.equals(language, that.language) &&
               Objects.equals(languageCode, that.languageCode) &&
               Objects.equals(translationLanguages, that.translationLanguages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(youtubeApi, videoId, apiUrl, language, languageCode, isGenerated, translationLanguages, isTranslatable);
    }

    @Override
    public String toString() {
        String template = "Transcript for video with id: %s.\n" +
                          "Language: %s\n" +
                          "Language code: %s\n" +
                          "API URL for retrieving content: %s\n" +
                          "Available translation languages: %s";

        return String.format(template,
                videoId,
                language,
                languageCode,
                apiUrl,
                new TreeSet<>(translationLanguages.keySet()));
    }
}
