package io.github.thoroldvix.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is the main interface for the YouTube Transcript API.
 * <p>
 * It provides functionality for retrieving all available transcripts or retrieving actual transcript content for a single video, playlist, or channel.
 * </p>
 * <p>
 * To instantiate this API, you should use {@link TranscriptApiFactory}.
 * </p>
 */
public class YoutubeTranscriptApi {

    private final YoutubeApi youtubeApi;
    private final ObjectMapper objectMapper;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    YoutubeTranscriptApi(YoutubeClient client) {
        this.objectMapper = new ObjectMapper();
        this.youtubeApi = new YoutubeApi(client, objectMapper);
    }

    private static TranscriptContent transcriptContentSupplier(TranscriptRequest request, String[] languageCodes, TranscriptList transcriptList) {
        try {
            return transcriptList.findTranscript(languageCodes).fetch();
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }

        return TranscriptContent.empty();
    }

    private static void joinFutures(List<CompletableFuture<Void>> futures, String playlistId) throws TranscriptRetrievalException {
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TranscriptRetrievalException) {
                throw (TranscriptRetrievalException) e.getCause();
            } else {
                throw new TranscriptRetrievalException("Failed to retrieve transcripts for playlist: " + playlistId, e);
            }
        }
    }

    /**
     * Retrieves transcript content for a single video.
     * <p>
     * This is a shortcut for calling:
     * </p>
     * <p>
     * {@code listTranscripts(videoId).findTranscript(languageCodes).fetch();}
     * </p>
     *
     * @param videoId       The ID of the video
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return {@link TranscriptContent} The transcript content
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    public TranscriptContent getTranscript(String videoId, String... languageCodes) throws TranscriptRetrievalException {
        return listTranscripts(videoId)
                .findTranscript(languageCodes)
                .fetch();
    }

    /**
     * Retrieves a list of available transcripts for a given video.
     *
     * @param videoId The ID of the video
     * @return {@link TranscriptList} A list of all available transcripts for the given video
     * @throws TranscriptRetrievalException If the retrieval of the transcript list fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    public TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException {
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
        String innertubeData = youtubeApi.fetchInnertubeData(videoId);
        return extractTranscriptList(innertubeData, videoId);
    }

    private TranscriptList extractTranscriptList(String innertubeData, String videoId) throws TranscriptRetrievalException {
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
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript JSON.", e);
        }

        if (innertubeJson == null) {
            throw new TranscriptRetrievalException(videoId, "Failed to find captions track list.");
        }

        if (innertubeJson.has("playabilityStatus")) {
            checkPlayabilityStatus(videoId, innertubeJson.get("playabilityStatus"));
        }

        if (!innertubeJson.has("captions")) {
            throw new TranscriptRetrievalException(videoId, "This video does not have captions.");
        }

        JsonNode captionsJson = innertubeJson.get("captions").get("playerCaptionsTracklistRenderer");
        if (captionsJson == null) {
            throw new TranscriptRetrievalException(videoId, "Transcripts are disabled for this video.");
        }

        return captionsJson;
    }

    private void checkPlayabilityStatus(String videoId, JsonNode playabilityStatusJson) throws TranscriptRetrievalException {
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


    /**
     * Retrieves transcript lists for all videos in the specified playlist.
     *
     * @param playlistId The ID of the playlist
     * @param request    {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = youtubeApi.fetchVideoIdsForPlaylist(playlistId, request.getApiKey());

        List<CompletableFuture<Void>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() -> transcriptListSupplier(request, videoId), executorService)
                        .thenAccept(transcriptList -> {
                            if (transcriptList.isEmpty()) {
                                transcriptLists.put(transcriptList.getVideoId(), transcriptList);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcriptLists;
    }

    /**
     * Retrieves transcript content for all videos in the specified playlist.
     *
     * @param playlistId    The ID of the playlist
     * @param request       {@link TranscriptRequest} request object containing API key and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    public Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = listTranscriptsForPlaylist(playlistId, request);
        Map<String, TranscriptContent> transcripts = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = transcriptLists.values().stream()
                .map(transcriptList -> CompletableFuture.supplyAsync(() -> transcriptContentSupplier(request, languageCodes, transcriptList), executorService)
                        .thenAccept(transcriptContent -> {
                            if (transcriptContent.isEmpty()) {
                                transcripts.put(transcriptList.getVideoId(), transcriptContent);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcripts;
    }

    /**
     * Retrieves transcript lists for all videos for the specified channel.
     *
     * @param channelName The name of the channel
     * @param request     {@link TranscriptRequest} request object containing API key and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.fetchChannelPlaylistId(channelName, request.getApiKey());
        return listTranscriptsForPlaylist(channelPlaylistId, request);
    }

    /**
     * Retrieves transcript content for all videos for the specified channel.
     *
     * @param channelName   The name of the channel
     * @param request       {@link TranscriptRequest} request object containing API key and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    public Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.fetchChannelPlaylistId(channelName, request.getApiKey());
        return getTranscriptsForPlaylist(channelPlaylistId, request, languageCodes);
    }

    private TranscriptList transcriptListSupplier(TranscriptRequest request, String videoId) {
        try {
            return listTranscripts(videoId);
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }

        return TranscriptList.empty();
    }
}
