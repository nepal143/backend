package com.placementgo.backend.jd_intel.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class ScrapingService {

    public String scrape(String url) {
        log.info("Scraping URL: {}", url);
        try {
            // Fetch the document
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // Remove unwanted elements
            doc.select("script, style, nav, footer, iframe, header, ads, .ads, .advertisement").remove();

            // Extract text from the body or main content area
            String text = doc.body().text();

            // Basic cleaning
            return text.replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            log.error("Failed to scrape {}: {}", url, e.getMessage());
            return "";
        }
    }
}
