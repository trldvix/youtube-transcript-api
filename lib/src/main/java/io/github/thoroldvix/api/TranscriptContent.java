package io.github.thoroldvix.api;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Represents the content of a transcript for a single video.
 * <p>
 * When the transcript content is fetched from YouTube, it is provided in the form of XML containing multiple transcript fragments.
 * <p>
 * For example:
 * </p>
 * <pre>{@code
 *    <transcript>
 *         <text>Text</text>
 *         <start>0.0</start>
 *         <dur>1.54</dur>
 *    </transcript>
 *    <transcript>
 *         <text>Another text</text>
 *         <start>1.54</start>
 *         <dur>4.16</dur>
 *    </transcript>
 * }</pre>
 * This interface encapsulates the transcript content as a {@code List<Fragment>}.
 */
public interface TranscriptContent {

    /**
     * Retrieves a list of {@link Fragment} objects that represent the content of the transcript.
     *
     * @return A {@link List} of {@link Fragment} objects.
     */
    @Nullable
    List<Fragment> getContent();

    /**
     * Represents a single fragment of the transcript content.
     */
    interface Fragment {
        /**
         * Retrieves the text of the fragment.
         *
         * @return The text of the fragment as a {@link String}.
         */
        String getText();

        /**
         * Retrieves the start time of the fragment in seconds.
         *
         * @return The start time of the fragment as a {@link Double}.
         */
        double getStart();

        /**
         * Retrieves the duration of the fragment in seconds.
         *
         * @return The duration of the fragment as a {@link Double}.
         */
        double getDur();
    }
}


