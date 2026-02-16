package com.placementgo.backend.resume.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class GeminiPromptBuilder {

    private String loadFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template file: " + path, e);
        }
    }

    public String buildLatexPrompt(String parsedResumeJson, String jobDescription) {

        String mainTex = loadFile("tempDir/main.tex");
        String classFile = loadFile("tempDir/tccv.cls");

        return """
You are an elite resume strategist and LaTeX engineer.

Your task:
Generate a HIGH-IMPACT, ATS-OPTIMIZED, STRICTLY ONE-PAGE resume that compiles cleanly without layout issues.

========================================================
CRITICAL PAGE & LAYOUT SAFETY RULES (MANDATORY)
========================================================

- The resume MUST fit on EXACTLY ONE PAGE.
- It MUST NOT overflow to a second page.
- It MUST NOT cause text overlapping.
- It MUST NOT overflow margins.
- It MUST NOT break layout alignment.
- It MUST NOT modify font sizes.
- It MUST NOT insert \\vspace, \\hspace, or spacing hacks.
- It MUST NOT alter margins or geometry.
- It MUST NOT modify documentclass.
- It MUST NOT modify layout structure.
- It MUST use only safe standard section content replacement.
- It MUST compile cleanly with pdflatex without warnings that cause layout distortion.

If content exceeds one page:
- Aggressively compress content.
- Reduce bullet length.
- Remove low-value achievements.
- Keep only the most job-relevant experience.
- Limit each job to maximum 3 concise bullets.
- Limit projects to most relevant 2–3.
- Summary must not exceed 3 lines.

========================================================
CONTENT INTELLIGENCE RULES
========================================================

- You have freedom to decide section headings.
- You may reorder sections strategically.
- You may rename sections to better match the job.
- Prioritize content that aligns strongly with the job description.
- Remove irrelevant information.
- Use strong action verbs.
- Include measurable impact where possible.
- Naturally integrate important job keywords.

========================================================
OUTPUT RESTRICTIONS
========================================================

- Modify ONLY content areas inside the template.
- DO NOT add new packages.
- DO NOT create new environments.
- DO NOT include comments.
- DO NOT include explanations.
- DO NOT wrap in markdown.
- Output must start with \\documentclass and end with \\end{document}.
- Output must compile to a SINGLE PAGE PDF with no layout corruption.

========================================================
CLASS FILE (tccv.cls)
========================================================
%s

========================================================
TEMPLATE FILE (main.tex)
========================================================
%s

========================================================
PARSED RESUME JSON
========================================================
%s

========================================================
JOB DESCRIPTION
========================================================
%s

Return ONLY valid, safe, single-page LaTeX code.
"""
                .formatted(classFile, mainTex, parsedResumeJson, jobDescription);
    }

}
