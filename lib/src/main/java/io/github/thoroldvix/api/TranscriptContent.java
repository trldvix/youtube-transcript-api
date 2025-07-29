package io.github.thoroldvix.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
public class TranscriptContent {

    private final List<TranscriptContent.Fragment> content;

    public TranscriptContent(List<TranscriptContent.Fragment> content) {
        this.content = content;
    }

    /**
     * Retrieves a list of {@link TranscriptContent.Fragment} objects that represent the content of the transcript.
     *
     * @return A {@link List} of {@link TranscriptContent.Fragment} objects.
     */
    public List<TranscriptContent.Fragment> getContent() {
        return Collections.unmodifiableList(content);
    }

    static TranscriptContent empty() {
        return new TranscriptContent(Collections.emptyList());
    }

    boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranscriptContent that = (TranscriptContent) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public String toString() {
        return "content=[" + content.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")) + "]";
    }

    /**
     * Represents a single fragment of the transcript content.
     */
    @JacksonXmlRootElement(localName = "transcript")
    public static class Fragment {
        @JacksonXmlText
        @JsonProperty("text")
        private String text;
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty("start")
        private double start;
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty("dur")
        private double dur;

        public Fragment(String text, double start, double dur) {
            this.text = text;
            this.start = start;
            this.dur = dur;
        }

        Fragment() {
        }

        /**
         * Retrieves the text of the fragment.
         *
         * @return The text of the fragment as a {@link String}.
         */
        public String getText() {
            return text;
        }

        /**
         * Retrieves the start time of the fragment in seconds.
         *
         * @return The start time of the fragment as a {@link Double}.
         */
        public double getStart() {
            return start;
        }

        /**
         * Retrieves the duration of the fragment in seconds.
         *
         * @return The duration of the fragment as a {@link Double}.
         */
        public double getDur() {
            return dur;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fragment fragment = (Fragment) o;
            return Double.compare(start, fragment.start) == 0 && Double.compare(dur, fragment.dur) == 0 && Objects.equals(text, fragment.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, start, dur);
        }

        @Override
        public String toString() {
            return "{" +
                   "text='" + text + '\'' +
                   ", start=" + start +
                   ", dur=" + dur +
                   '}';
        }
    }
}

