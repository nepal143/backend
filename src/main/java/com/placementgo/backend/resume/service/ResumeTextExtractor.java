package com.placementgo.backend.resume.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class ResumeTextExtractor {

    private final Tika tika = new Tika();

    public String extractText(File file) throws IOException, TikaException {
        return tika.parseToString(file);
    }
}
