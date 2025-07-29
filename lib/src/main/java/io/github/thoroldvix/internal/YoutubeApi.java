package io.github.thoroldvix.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class YoutubeApi {
    private final static String YOUTUBE_API_V3_BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    private static final String INNERTUBE_API_URL = "https://www.youtube.com/youtubei/v1/player?key=%s";
    private final YoutubeClient client;

    YoutubeApi(YoutubeClient client) {
        this.client = client;
    }

    String getChannelPlaylistId(String channelName, String apiKey) throws TranscriptRetrievalException {
        String channelId = getChannelId(channelName, apiKey);
        Map<String, String> params = createParams(
                "key", apiKey,
                "part", "contentDetails",
                "id", channelId
        );

        String url = buildUrlWithParams(YOUTUBE_API_V3_BASE_URL + "channels", params);
        String channelJson = client.get(url);

        return YoutubeApiResponseParser.getChannelPlaylistId(channelJson);
    }

    List<String> getVideoIds(String playlistId, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams(
                "key", apiKey,
                "playlistId", playlistId,
                "part", "snippet",
                "maxResults", "50"
        );
        List<String> videoIds = new ArrayList<>();

        while (true) {
            String url = buildUrlWithParams(YOUTUBE_API_V3_BASE_URL + "playlistItems", params);
            String playlistJson = client.get(url);

            videoIds.addAll(YoutubeApiResponseParser.getVideoIds(playlistJson));
            String nextPageToken = YoutubeApiResponseParser.getNextPageToken(playlistJson);

            if (nextPageToken == null) {
                break;
            }

            params.put("pageToken", nextPageToken);
        }

        return videoIds;
    }

    private String getChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams(
                "key", apiKey,
                "q", channelName,
                "part", "snippet",
                "type", "channel"
        );

        String url = buildUrlWithParams(YOUTUBE_API_V3_BASE_URL + "search", params);
        String searchJson = client.get(url);

        return YoutubeApiResponseParser.getChannelId(searchJson, channelName);
    }

    String getVideoPage(String videoId) throws TranscriptRetrievalException {
        String videoPageHtml;
        try {
            videoPageHtml = client.get(YOUTUBE_WATCH_URL + videoId, Map.of("Accept-Language", "en-US"));
        } catch (TranscriptRetrievalException e) {
            throw new TranscriptRetrievalException(videoId, e.getMessage());
        }

        String consentPagePattern = "action=\"https://consent.youtube.com/s\"";
        if (videoPageHtml.contains(consentPagePattern)) {
            throw new TranscriptRetrievalException("Video is age restricted");
        }

        if (videoPageHtml.contains("class=\"g-recaptcha\"")) {
            throw new TranscriptRetrievalException(videoId, "YouTube is receiving too many requests from this IP and now requires solving a captcha to continue.");
        }

        return videoPageHtml;
    }

    String getTranscriptContentXml(String videoId, String contentUrl) throws TranscriptRetrievalException {
        String transcriptXml;
        try {
            transcriptXml = client.get(contentUrl, Map.of("Accept-Language", "en-US"));
        } catch (TranscriptRetrievalException e) {
            throw new TranscriptRetrievalException(videoId, e.getMessage());
        }

        if (transcriptXml == null || transcriptXml.isBlank()) {
            throw new TranscriptRetrievalException(videoId, "YouTube returned an empty transcript XML.");
        }

        return transcriptXml;
    }

    String getInnertubeData(String videoId, String innertubeApiKey) throws TranscriptRetrievalException {
        String body = String.format("{\n" +
                                    "\"context\":{\n" +
                                    "    \"client\": {\n" +
                                    "    \"clientName\": \"ANDROID\",\n" +
                                    "    \"clientVersion\": \"20.10.38\"\n" +
                                    "    }\n" +
                                    "},\n" +
                                    "    \"videoId\": \"%s\"\n" +
                                    "}", videoId);

        String innertubeData = client.post(String.format(INNERTUBE_API_URL, innertubeApiKey), body);
        if (innertubeData == null || innertubeData.isBlank()) {
            throw new TranscriptRetrievalException(videoId, "Could not get innertube data from YouTube.");
        }

        return innertubeData;
    }

    private Map<String, String> createParams(String... params) {
        Map<String, String> map = new HashMap<>(params.length / 2);
        for (int i = 0; i < params.length; i += 2) {
            map.put(params[i], params[i + 1]);
        }
        return map;
    }

    private String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        if (!params.isEmpty()) {
            sb.append(baseUrl.contains("?") ? "&" : "?");
            params.forEach((key, value) -> {
                sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                sb.append("&");
            });

            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}