package io.github.thoroldvix.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptFormattersTest {

    private static final TranscriptContent EMPTY_CONTENT = new TranscriptContent(List.of());
    private TranscriptContent content;

    @BeforeEach
    void setUp() {
        List<TranscriptContent.Fragment> fragments = List.of(new TranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new TranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new TranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239));
        content = new TranscriptContent(fragments);
    }

    @Test
    void jsonFormatter() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.jsonFormatter();
        String expected = """
                {"content":[{"text":"Hey, this is just a test","start":0.0,"dur":1.54},\
                {"text":"this is not the original transcript","start":1.54,"dur":4.16},\
                {"text":"test & test, like this \\"test\\" he's testing","start":5.7,"dur":3.239}]}\
                """;

        String actual = transcriptFormatter.format(content);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void jsonFormatterNoContent() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.jsonFormatter();

        String expected = "{\"content\":[]}";

        String actual = transcriptFormatter.format(EMPTY_CONTENT);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void jsonPrettyFormatter() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.prettyJsonFormatter();

        String expected = """
                {
                  "content" : [ {
                    "text" : "Hey, this is just a test",
                    "start" : 0.0,
                    "dur" : 1.54
                  }, {
                    "text" : "this is not the original transcript",
                    "start" : 1.54,
                    "dur" : 4.16
                  }, {
                    "text" : "test & test, like this \\"test\\" he's testing",
                    "start" : 5.7,
                    "dur" : 3.239
                  } ]
                }""";

        String actual = transcriptFormatter.format(content);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void jsonPrettyFormatterNoContent() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.prettyJsonFormatter();

        String expected = "{\n  \"content\" : [ ]\n}";

        String actual = transcriptFormatter.format(EMPTY_CONTENT);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void textFormatter() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.textFormatter();

        String expected = "Hey, this is just a test\nthis is not the original transcript\ntest & test, like this \"test\" he's testing";

        String actual = transcriptFormatter.format(content);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void webVTTFormatter() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.webVTTFormatter();

        String expected = """
                WEBVTT
                
                00:00:00.000 --> 00:00:01.540
                Hey, this is just a test

                00:00:01.540 --> 00:00:05.700
                this is not the original transcript

                00:00:05.700 --> 00:00:08.939
                test & test, like this "test" he's testing""";

        String actual = transcriptFormatter.format(content);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void webVTTFormatterNoContent() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.webVTTFormatter();

        String expected = "WEBVTT\n\n";

        String actual = transcriptFormatter.format(EMPTY_CONTENT);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void srtFormatter() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.srtFormatter();

        String expected = """
                1
                00:00:00.000 --> 00:00:01.540
                Hey, this is just a test

                2
                00:00:01.540 --> 00:00:05.700
                this is not the original transcript

                3
                00:00:05.700 --> 00:00:08.939
                test & test, like this "test" he's testing""";

        String actual = transcriptFormatter.format(content);

        assertThat(actual).isEqualToNormalizingNewlines(expected);
    }

    @Test
    void srtFormatterNoContent() {
        TranscriptFormatter transcriptFormatter = TranscriptFormatters.srtFormatter();

        String expected = "";

        String actual = transcriptFormatter.format(EMPTY_CONTENT);

        assertThat(actual).isEqualTo(expected);
    }
}
