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

        return """
You are a STRICT LaTeX content editor.

Your job is NOT to redesign the template.
Your job is ONLY to replace content inside the provided LaTeX template.

========================================================
CRITICAL: DO NOT MODIFY STRUCTURE
========================================================

- DO NOT change \\documentclass
- DO NOT add or remove packages
- DO NOT add new commands
- DO NOT define new environments
- DO NOT modify spacing
- DO NOT modify margins
- DO NOT modify fonts
- DO NOT modify layout
- DO NOT remove existing environments
- DO NOT introduce new custom macros

You may ONLY replace placeholder content with improved content.

If the template contains custom commands, use them exactly as-is.
Do NOT invent commands like:
\\resumeItem
\\resumeSubheading
\\resumeSection
or any other undefined command.

========================================================
STRICT LATEX SAFETY RULES
========================================================

You MUST escape these characters in normal text:

&  → \\&
%% → \\%%
$  → \\$
#  → \\#
_  → \\_
{  → \\{
}  → \\}
~  → \\textasciitilde{}
^  → \\textasciicircum{}

- All braces must be balanced.
- No runaway arguments.
- No raw URLs (use \\href if already used in template).
- No unclosed environments.
- No nested commands inside macro arguments unless already present in template.

========================================================
PAGE RULES
========================================================

- Resume must remain ONE PAGE.
- Maximum 3 bullet points per role.
- Maximum 3 lines summary.
- Keep content concise and ATS optimized.

If content is too long, compress intelligently.

========================================================
INPUT TEMPLATE (DO NOT MODIFY STRUCTURE)
========================================================
%s

========================================================
PARSED RESUME DATA (SOURCE CONTENT)
========================================================
%s

========================================================
JOB DESCRIPTION (OPTIMIZATION TARGET)
========================================================
%s

========================================================
FINAL INSTRUCTION
========================================================

Return the FULL LaTeX document.
Start with \\documentclass.
End with \\end{document}.
Return ONLY valid LaTeX.
No markdown.
No explanation.
No comments.
"""
                .formatted(mainTex, parsedResumeJson, jobDescription);
    }
}
