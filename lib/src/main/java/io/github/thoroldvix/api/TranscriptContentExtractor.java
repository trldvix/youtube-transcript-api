package io.github.thoroldvix.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.thoroldvix.api.TranscriptContent.Fragment;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Responsible for extracting transcript content from xml.
 */
final class TranscriptContentExtractor {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private TranscriptContentExtractor() {
    }

    private static List<Fragment> formatFragments(List<Fragment> fragments) {
        return fragments.stream()
                .map(TranscriptContentExtractor::removeHtmlTags)
                .map(TranscriptContentExtractor::unescapeXmlTags)
                .collect(Collectors.toList());
    }

    static TranscriptContent extract(String videoId, String xml) throws TranscriptRetrievalException {
        List<Fragment> fragments = parseFragments(videoId, xml).stream()
                .filter(TranscriptContentExtractor::isValidTranscriptFragment)
                .collect(Collectors.toList());
        List<Fragment> content = formatFragments(fragments);
        return new TranscriptContent(content);
    }

    private static Fragment unescapeXmlTags(Fragment fragment) {
        String formattedValue = StringEscapeUtils.unescapeXml(fragment.getText());
        return new Fragment(formattedValue, fragment.getStart(), fragment.getDur());
    }

    private static Fragment removeHtmlTags(Fragment fragment) {
        Pattern pattern = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
        String text = pattern.matcher(fragment.getText()).replaceAll("");
        return new Fragment(text, fragment.getStart(), fragment.getDur());
    }

    @SuppressWarnings("ConstantConditions")
    private static boolean isValidTranscriptFragment(Fragment fragment) {
        String text = fragment.getText();
        return text != null && !text.isBlank();
    }

    private static List<Fragment> parseFragments(String videoId, String xml) throws TranscriptRetrievalException {
        try {
            return XML_MAPPER.readValue(xml, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript content XML.", e);
        }
    }
}
