package com.placementgo.backend.resume.dto;

import java.util.List;

public class GenerateResumeResponse {

    private String latex;
    private String pdfBase64;
    private int atsScore;
    private List<String> suggestions;

    public GenerateResumeResponse() {}

    public GenerateResumeResponse(String latex, String pdfBase64) {
        this.latex = latex;
        this.pdfBase64 = pdfBase64;
    }

    public GenerateResumeResponse(String latex, String pdfBase64, int atsScore, List<String> suggestions) {
        this.latex = latex;
        this.pdfBase64 = pdfBase64;
        this.atsScore = atsScore;
        this.suggestions = suggestions;
    }

    public String getLatex() { return latex; }
    public void setLatex(String latex) { this.latex = latex; }

    public String getPdfBase64() { return pdfBase64; }
    public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }

    public int getAtsScore() { return atsScore; }
    public void setAtsScore(int atsScore) { this.atsScore = atsScore; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
