package io.github.thoroldvix.api;

import io.github.thoroldvix.internal.TranscriptApiFactory;

import java.util.Map;

/**
 * This is the main interface for the YouTube Transcript API.
 * <p>
 * It provides functionality for retrieving all available transcripts or retrieving actual transcript content for a single video, playlist, or channel.
 * </p>
 * <p>
 * To instantiate this API, you should use {@link TranscriptApiFactory}.
 * </p>
 */
public interface YoutubeTranscriptApi {

    /**
     * Retrieves a list of available transcripts for a given video.
     *
     * @param videoId The ID of the video
     * @return {@link TranscriptList} A list of all available transcripts for the given video
     * @throws TranscriptRetrievalException If the retrieval of the transcript list fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException;

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
    TranscriptContent getTranscript(String videoId, String... languageCodes) throws TranscriptRetrievalException;

    /**
     * Retrieves transcript lists for all videos in the specified playlist.
     *
     * @param playlistId The ID of the playlist
     * @param request    {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos for the specified channel.
     *
     * @param channelName The name of the channel
     * @param request     {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript content for all videos in the specified playlist.
     *
     * @param playlistId    The ID of the playlist
     * @param request       {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId,
                                                             TranscriptRequest request,
                                                             String... languageCodes) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript content for all videos for the specified channel.
     *
     * @param channelName   The name of the channel
     * @param request       {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException;
}
