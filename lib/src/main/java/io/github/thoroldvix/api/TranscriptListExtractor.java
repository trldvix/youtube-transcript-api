package io.github.thoroldvix.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class TranscriptListExtractor {
    private final YoutubeApi youtubeApi;
    private final ObjectMapper objectMapper;

    TranscriptListExtractor(YoutubeApi youtubeApi, ObjectMapper objectMapper) {
        this.youtubeApi = youtubeApi;
        this.objectMapper = objectMapper;
    }

    TranscriptList extract(String videoId, String innertubeData) throws TranscriptRetrievalException {
        JsonNode captionsJson = extractCaptionsJson(innertubeData, videoId);
        Map<String, Transcript> manualTranscripts = extractManualTranscripts(captionsJson, videoId);
        Map<String, Transcript> generatedTranscripts = extractGeneratedTranscripts(captionsJson, videoId);
        Map<String, String> translationLanguages = extractTranslationLanguages(captionsJson);
        return new TranscriptList(videoId, manualTranscripts, generatedTranscripts, translationLanguages);
    }

    private JsonNode extractCaptionsJson(String innertubeData, String videoId) throws TranscriptRetrievalException {
        JsonNode innertubeJson;
        try {
            innertubeJson = objectMapper.readTree(innertubeData);
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse captions JSON.", e);
        }

        if (innertubeJson == null) {
            throw new TranscriptRetrievalException(videoId, "Failed to find captions track list.");
        }

        if (innertubeJson.has("playabilityStatus")) {
            verifyPlayabilityStatus(videoId, innertubeJson.get("playabilityStatus"));
        }

        if (!innertubeJson.has("captions")) {
            throw new TranscriptRetrievalException(videoId, "Transcripts are disabled for this video.");
        }

        return innertubeJson.get("captions").get("playerCaptionsTracklistRenderer");
    }

    private void verifyPlayabilityStatus(String videoId, JsonNode playabilityStatusJson) throws TranscriptRetrievalException {
        String status = playabilityStatusJson.get("status").asText();

        if (status != null && !status.isBlank() && !status.equals("OK")) {

            String reason = playabilityStatusJson.get("reason").asText();
            if (status.equals("LOGIN_REQUIRED")) {
                if (reason.equals("Sign in to confirm you’re not a bot")) {
                    throw new TranscriptRetrievalException(videoId, "YouTube is blocking requests from your ip because it thinks you are a bot");
                }
                if (reason.equals("This video may be inappropriate for some users.")) {
                    throw new TranscriptRetrievalException(videoId, "Video is age restricted");
                }
            }

            if (status.equals("ERROR") && reason.equals("This video is unavailable")) {
                throw new TranscriptRetrievalException(videoId, reason);
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

    private Map<String, Transcript> extractManualTranscripts(JsonNode json, String videoId) {
        return extractTranscript(json, jsonNode -> !jsonNode.has("kind"), videoId);
    }

    private Map<String, Transcript> extractGeneratedTranscripts(JsonNode json, String videoId) {
        return extractTranscript(json, jsonNode -> jsonNode.has("kind"), videoId);
    }

    private Map<String, Transcript> extractTranscript(JsonNode json, Predicate<JsonNode> filter, String videoId) {
        Map<String, String> translationLanguages = extractTranslationLanguages(json);
        return StreamSupport.stream(json.get("captionTracks").spliterator(), false)
                .filter(filter)
                .map(jsonNode -> new Transcript(
                        youtubeApi,
                        videoId,
                        jsonNode.get("baseUrl").asText().replace("&fmt=srv3", ""),
                        jsonNode.get("name").get("runs").get(0).get("text").asText(),
                        jsonNode.get("languageCode").asText(),
                        jsonNode.has("kind"),
                        translationLanguages
                ))
                .collect(Collectors.toMap(
                        Transcript::getLanguageCode,
                        transcript -> transcript,
                        (existing, replacement) -> existing)
                );
    }

    private Map<String, String> extractTranslationLanguages(JsonNode json) {
        if (!json.has("translationLanguages")) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(json.get("translationLanguages").spliterator(), false)
                .collect(Collectors.toMap(
                        jsonNode -> jsonNode.get("languageCode").asText(),
                        jsonNode -> jsonNode.get("languageName").get("runs").get(0).get("text").asText()
                ));
    }
}
