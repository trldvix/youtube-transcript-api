package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptList;
import io.github.thoroldvix.api.TranscriptRetrievalException;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Extracts transcript list from video page HTML.
 */
final class TranscriptListExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TranscriptListExtractor() {
    }

    static TranscriptList extract(String innertubeData, String videoId, YoutubeApi youtubeApi) throws TranscriptRetrievalException {
        JsonNode captionsJson = extractCaptionsJson(innertubeData, videoId);
        Map<String, Transcript> manualTranscripts = extractManualTranscripts(youtubeApi, captionsJson, videoId);
        Map<String, Transcript> generatedTranscripts = extractGeneratedTranscripts(youtubeApi, captionsJson, videoId);
        Map<String, String> translationLanguages = extractTranslationLanguages(captionsJson);
        return new DefaultTranscriptList(videoId, manualTranscripts, generatedTranscripts, translationLanguages);
    }

    private static JsonNode extractCaptionsJson(String innertubeData, String videoId) throws TranscriptRetrievalException {
        JsonNode innertubeJson;
        try {
            innertubeJson = OBJECT_MAPPER.readTree(innertubeData);
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript JSON.", e);
        }

        if (innertubeJson == null) {
            throw new TranscriptRetrievalException(videoId, "Failed to find captions track list.");
        }

        checkPlayabilityStatus(videoId, innertubeJson.get("playabilityStatus"));

        if (!innertubeJson.has("captions")) {
            throw new TranscriptRetrievalException(videoId, "This video does not have captions.");
        }

        JsonNode captionsJson = innertubeJson.get("captions").get("playerCaptionsTracklistRenderer");
        if (captionsJson == null) {
            throw new TranscriptRetrievalException(videoId, "Transcripts are disabled for this video.");
        }

        return captionsJson;
    }

    private static void checkPlayabilityStatus(String videoId, JsonNode playabilityStatusJson) throws TranscriptRetrievalException {
        String status = playabilityStatusJson.get("status").asText();

        if (status != null && !status.isBlank() && !status.equals("OK")) {
            String reason = playabilityStatusJson.get("reason").asText();
            if (status.equals("LOGIN_REQUIRED")) {
                if (reason.equals("BOT_DETECTED")) {
                    throw new TranscriptRetrievalException(videoId, "YouTube is blocking requests from your ip because it thinks you are a bot");
                }
                if (reason.equals("AGE_RESTRICTED")) {
                    throw new TranscriptRetrievalException(videoId, "Video is age restricted");
                }
            }

            if (status.equals("ERROR") && reason.equals("VIDEO_UNAVAILABLE")) {
                throw new TranscriptRetrievalException(videoId, "This video is not available");
            }

            JsonNode runs = playabilityStatusJson
                    .path("errorScreen")
                    .path("playerErrorMessageRenderer")
                    .path("subreason")
                    .path("runs");

            String detailedReason = StreamSupport.stream(runs.spliterator(), false)
                    .map(run -> run.path("text").asText())
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.joining(", "));

            throw new TranscriptRetrievalException(videoId, "Video is unplayable." + (detailedReason.isBlank() ? "" : " Additional details: " + detailedReason));
        }
    }

    private static Map<String, String> extractTranslationLanguages(JsonNode json) {
        if (!json.has("translationLanguages")) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(json.get("translationLanguages").spliterator(), false)
                .collect(Collectors.toMap(
                        jsonNode -> jsonNode.get("languageCode").asText(),
                        jsonNode -> jsonNode.get("languageName").get("runs").get(0).get("text").asText()
                ));
    }

    private static Map<String, Transcript> extractManualTranscripts(YoutubeApi youtubeApi, JsonNode json, String videoId) {
        return extractTranscript(youtubeApi, json, jsonNode -> !jsonNode.has("kind"), videoId);
    }

    private static Map<String, Transcript> extractGeneratedTranscripts(YoutubeApi youtubeApi, JsonNode json, String videoId) {
        return extractTranscript(youtubeApi, json, jsonNode -> jsonNode.has("kind"), videoId);
    }

    private static Map<String, Transcript> extractTranscript(YoutubeApi youtubeApi, JsonNode json, Predicate<JsonNode> filter, String videoId) {
        Map<String, String> translationLanguages = extractTranslationLanguages(json);
        return StreamSupport.stream(json.get("captionTracks").spliterator(), false)
                .filter(filter)
                .map(jsonNode -> extractTranscript(youtubeApi, jsonNode, translationLanguages, videoId))
                .collect(Collectors.toMap(
                        Transcript::getLanguageCode,
                        transcript -> transcript,
                        (existing, replacement) -> existing)
                );
    }

    private static Transcript extractTranscript(YoutubeApi youtubeApi, JsonNode jsonNode, Map<String, String> translationLanguages, String videoId) {
        return new DefaultTranscript(
                youtubeApi,
                videoId,
                jsonNode.get("baseUrl").asText().replace("&fmt=srv3", ""),
                jsonNode.get("name").get("runs").get(0).get("text").asText(),
                jsonNode.get("languageCode").asText(),
                jsonNode.has("kind"),
                translationLanguages
        );
    }
}
