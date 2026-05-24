package com.voiceinput.pro.service;

import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.support.TextSupport;
import org.springframework.stereotype.Service;

@Service
public class SceneOutputFormatter {

    public FormattedOutput format(SceneType sceneType, String optimizedText, String markdown) {
        String normalizedOptimized = TextSupport.normalizeWhitespace(optimizedText);
        String normalizedMarkdown = TextSupport.normalizeWhitespace(markdown);
        return new FormattedOutput(normalizedOptimized, normalizedMarkdown);
    }

    public record FormattedOutput(String optimizedText, String markdown) {
    }
}
