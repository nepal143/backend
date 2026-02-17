package com.placementgo.backend.resume.dto;

public class GenerateResumeResponse {

    private String latex;
    private String pdfBase64;

    public GenerateResumeResponse() {}

    public GenerateResumeResponse(String latex, String pdfBase64) {
        this.latex = latex;
        this.pdfBase64 = pdfBase64;
    }

    public String getLatex() {
        return latex;
    }

    public void setLatex(String latex) {
        this.latex = latex;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }
}
