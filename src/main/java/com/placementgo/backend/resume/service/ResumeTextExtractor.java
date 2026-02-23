package com.placementgo.backend.resume.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ResumeTextExtractor {

    private final Tika tika = new Tika();
    public String extractText(InputStream inputStream)
            throws IOException, TikaException {

        return tika.parseToString(inputStream);
    }
}