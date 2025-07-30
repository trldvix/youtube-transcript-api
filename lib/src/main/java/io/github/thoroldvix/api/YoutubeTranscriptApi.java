package io.github.thoroldvix.api;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    private final TranscriptListExtractor transcriptListExtractor;
    private final ExecutorService executorService;

    YoutubeTranscriptApi(YoutubeClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        executorService = Executors.newFixedThreadPool(10);
        this.youtubeApi = new YoutubeApi(client, objectMapper);
        this.transcriptListExtractor = new TranscriptListExtractor(youtubeApi, objectMapper);
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
        return transcriptListExtractor.extract(videoId, innertubeData);
    }

    /**
     * Retrieves transcript lists for all videos in the specified playlist.
     *
     * @param playlistId The ID of the playlist
     * @param request    {@link BulkTranscriptRequest} request object containing API key and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, BulkTranscriptRequest request) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = youtubeApi.fetchVideoIdsForPlaylist(playlistId, request.getApiKey());

        List<CompletableFuture<Void>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() -> transcriptListSupplier(request, videoId), executorService)
                        .thenAccept(transcriptList -> {
                            if (!transcriptList.isEmpty()) {
                                transcriptLists.put(transcriptList.getVideoId(), transcriptList);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcriptLists;
    }

    private TranscriptList transcriptListSupplier(BulkTranscriptRequest request, String videoId) {
        try {
            return listTranscripts(videoId);
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }

        return TranscriptList.empty();
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
     * Retrieves transcript content for all videos in the specified playlist.
     *
     * @param playlistId    The ID of the playlist
     * @param request       {@link BulkTranscriptRequest} request object containing API key and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    public Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId, BulkTranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = listTranscriptsForPlaylist(playlistId, request);
        Map<String, TranscriptContent> transcripts = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = transcriptLists.values().stream()
                .map(transcriptList -> CompletableFuture.supplyAsync(() -> transcriptContentSupplier(request, languageCodes, transcriptList), executorService)
                        .thenAccept(transcriptContent -> {
                            if (!transcriptContent.isEmpty()) {
                                transcripts.put(transcriptList.getVideoId(), transcriptContent);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcripts;
    }

    private static TranscriptContent transcriptContentSupplier(BulkTranscriptRequest request, String[] languageCodes, TranscriptList transcriptList) {
        try {
            return transcriptList.findTranscript(languageCodes).fetch();
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }

        return TranscriptContent.empty();
    }

    /**
     * Retrieves transcript lists for all videos for the specified channel.
     *
     * @param channelName The name of the channel
     * @param request     {@link BulkTranscriptRequest} request object containing API key and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, BulkTranscriptRequest request) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.fetchChannelPlaylistId(channelName, request.getApiKey());
        return listTranscriptsForPlaylist(channelPlaylistId, request);
    }

    /**
     * Retrieves transcript content for all videos for the specified channel.
     *
     * @param channelName   The name of the channel
     * @param request       {@link BulkTranscriptRequest} request object containing API key and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    public Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, BulkTranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.fetchChannelPlaylistId(channelName, request.getApiKey());
        return getTranscriptsForPlaylist(channelPlaylistId, request, languageCodes);
    }
}
