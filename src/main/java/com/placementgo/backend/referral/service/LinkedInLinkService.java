package com.placementgo.backend.referral.service;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class LinkedInLinkService {

    public String generate(String jd) {
        return "https://www.linkedin.com/search/results/people/?keywords="
                + URLEncoder.encode(jd, StandardCharsets.UTF_8);
    }
}
