package io.github.thoroldvix.internal;


import io.github.thoroldvix.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link YoutubeTranscriptApi}.
 */
final class DefaultYoutubeTranscriptApi implements YoutubeTranscriptApi {

    private final YoutubeApi youtubeApi;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    DefaultYoutubeTranscriptApi(YoutubeClient client) {
        this.youtubeApi = new YoutubeApi(client);
    }

    private static TranscriptContent transcriptContentSupplier(TranscriptRequest request, String[] languageCodes, TranscriptList transcriptList) {
        try {
            return transcriptList.findTranscript(languageCodes).fetch();
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }

        return null;
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

    @Override
    public TranscriptContent getTranscript(String videoId, String... languageCodes) throws TranscriptRetrievalException {
        return listTranscripts(videoId)
                .findTranscript(languageCodes)
                .fetch();
    }

    @Override
    public TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException {
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
        String videoPageHtml = youtubeApi.getVideoPage(videoId);
        String innertubeApiKey = extractInnertubeApiKey(videoId, videoPageHtml);
        String innertubeData = youtubeApi.getInnertubeData(videoId, innertubeApiKey);
        return TranscriptListExtractor.extract(innertubeData, videoId, youtubeApi);
    }

    private String extractInnertubeApiKey(String videoId, String videoPageHtml) throws TranscriptRetrievalException {
        Pattern pattern = Pattern.compile("\"INNERTUBE_API_KEY\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(videoPageHtml);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new TranscriptRetrievalException(videoId, "INNERTUBE_API_KEY not found in video page HTML.");
        }
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = youtubeApi.getVideoIds(playlistId, request.getApiKey());

        List<CompletableFuture<Void>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() -> transcriptListSupplier(request, videoId), executorService)
                        .thenAccept(transcriptList -> {
                            if (transcriptList != null) {
                                transcriptLists.put(transcriptList.getVideoId(), transcriptList);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcriptLists;
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = listTranscriptsForPlaylist(playlistId, request);
        Map<String, TranscriptContent> transcripts = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = transcriptLists.values().stream()
                .map(transcriptList -> CompletableFuture.supplyAsync(() -> transcriptContentSupplier(request, languageCodes, transcriptList), executorService)
                        .thenAccept(transcriptContent -> {
                            if (transcriptContent != null) {
                                transcripts.put(transcriptList.getVideoId(), transcriptContent);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcripts;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.getChannelPlaylistId(channelName, request.getApiKey());
        return listTranscriptsForPlaylist(channelPlaylistId, request);
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.getChannelPlaylistId(channelName, request.getApiKey());
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
        return null;
    }
}
