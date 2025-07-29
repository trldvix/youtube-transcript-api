package io.github.thoroldvix.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class YoutubeApi {

    private final static String YOUTUBE_API_V3_BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    private static final String INNERTUBE_API_URL = "https://www.youtube.com/youtubei/v1/player?key=%s";

    private final YoutubeClient client;
    private final ObjectMapper objectMapper;

    YoutubeApi(YoutubeClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    String fetchChannelPlaylistId(String channelName, String apiKey) throws TranscriptRetrievalException {
        String channelId = fetchChannelId(channelName, apiKey);
        Map<String, String> params = createParams(
                "key", apiKey,
                "part", "contentDetails",
                "id", channelId
        );

        String url = buildUrlWithParams(YOUTUBE_API_V3_BASE_URL + "channels", params);
        String channelJson = client.get(url);

        return extractChannelPlaylistId(channelJson);
    }

    List<String> fetchVideoIdsForPlaylist(String playlistId, String apiKey) throws TranscriptRetrievalException {
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

            videoIds.addAll(extractVideoIds(playlistJson));
            String nextPageToken = extractNextPageToken(playlistJson);

            if (nextPageToken.isBlank()) {
                break;
            }

            params.put("pageToken", nextPageToken);
        }

        return videoIds;
    }

    private String fetchChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams(
                "key", apiKey,
                "q", channelName,
                "part", "snippet",
                "type", "channel"
        );

        String url = buildUrlWithParams(YOUTUBE_API_V3_BASE_URL + "search", params);
        String searchJson = client.get(url);

        return extractChannelId(searchJson, channelName);
    }

    private String fetchVideoPage(String videoId) throws TranscriptRetrievalException {
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

    String fetchInnertubeData(String videoId) throws TranscriptRetrievalException {
        String videoPageHtml = fetchVideoPage(videoId);
        String innertubeApiKey = extractInnertubeApiKey(videoId, videoPageHtml);
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

        if (innertubeData.isBlank()) {
            throw new TranscriptRetrievalException(videoId, "Could not get innertube data from YouTube.");
        }

        return innertubeData;
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

    String fetchTranscriptContentXml(String videoId, String contentUrl) throws TranscriptRetrievalException {
        String transcriptXml;
        try {
            transcriptXml = client.get(contentUrl, Map.of("Accept-Language", "en-US"));
        } catch (TranscriptRetrievalException e) {
            throw new TranscriptRetrievalException(videoId, e.getMessage());
        }

        if (transcriptXml.isBlank()) {
            throw new TranscriptRetrievalException(videoId, "YouTube returned an empty transcript XML.");
        }

        return transcriptXml;
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

    String extractChannelId(String channelJson, String channelName) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(channelJson);
        JsonNode channelId = jsonNode.get("items").get(0).get("snippet").get("channelId");

        if (channelId == null || channelId.isEmpty()) {
            throw new TranscriptRetrievalException("Could not find channel id for the channel with the name: " + channelName);
        }

        return channelId.asText();
    }

    List<String> extractVideoIds(String playlistJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(playlistJson);
        List<String> videoIds = new ArrayList<>();

        jsonNode.get("items").forEach(item -> {
            String videoId = item.get("snippet").get("resourceId").get("videoId").asText();
            videoIds.add(videoId);
        });

        return videoIds;
    }

    String extractNextPageToken(String playlistJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(playlistJson);
        JsonNode nextPageToken = jsonNode.get("nextPageToken");

        if (nextPageToken == null || nextPageToken.isEmpty()) {
            return "";
        }

        return nextPageToken.asText();
    }

    String extractChannelPlaylistId(String channelJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(channelJson);
        return jsonNode.get("items")
                .get(0)
                .get("contentDetails")
                .get("relatedPlaylists")
                .get("uploads")
                .asText();
    }

    private JsonNode parseJson(String json) throws TranscriptRetrievalException {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new TranscriptRetrievalException("Failed to parse YouTube API response JSON.", e);
        }
    }
}