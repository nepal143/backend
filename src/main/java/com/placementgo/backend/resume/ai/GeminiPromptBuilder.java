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

    public String buildLatexPrompt(String parsedResumeJson, String jobDescription, String templateId) {

        String templateFile = switch (templateId == null ? "classic" : templateId.toLowerCase()) {
            case "modern"   -> "tempDir/modern.tex";
            case "compact"  -> "tempDir/compact.tex";
            case "elegant"  -> "tempDir/elegant.tex";
            case "sharp"    -> "tempDir/sharp.tex";
            default         -> "tempDir/main.tex";
        };
        String mainTex = loadFile(templateFile);

        // Only include the document body in the prompt — the preamble is applied separately
        // by buildFinalLatex(), so there's no need to send ~3KB of package/macro definitions.
        int bodyStart = mainTex.indexOf("\\begin{document}");
        String templateForPrompt = (bodyStart >= 0) ? mainTex.substring(bodyStart) : mainTex;

        return """
You are a precise LaTeX content editor. Replace placeholder content in the given template with real resume data.

========================================================
ABSOLUTE RULES — VIOLATING THESE CAUSES FATAL COMPILE ERRORS
========================================================

1. DO NOT modify \\documentclass — copy it EXACTLY as in the template.
2. DO NOT add or remove \\usepackage lines.
3. DO NOT change \\newcommand definitions.
4. DO NOT invent macros not defined in the template.

========================================================
MANDATORY: USE CUSTOM MACROS (DO NOT EXPAND THEM)
========================================================

The template defines these macros. USE THEM EXACTLY — never write their raw \\begin/\\end expansion:

  For EXPERIENCE / EDUCATION / PROJECTS section wrappers:
    \\resumeSubHeadingListStart   ← use this, NOT \\begin{itemize}[leftmargin=0in,label={}]
    \\resumeSubHeadingListEnd     ← use this, NOT \\end{itemize}

  For bullet point lists INSIDE a role/project:
    \\resumeItemListStart         ← use this, NOT \\begin{itemize}
    \\resumeItemListEnd           ← use this, NOT \\end{itemize}

  For individual bullets:
    \\resumeItem{your text here}  ← use this, NOT \\item

  For a job or education entry heading:
    \\resumeSubheading{Company}{Date}{Role}{Location}

  For a project heading:
    \\resumeProjectHeading{\\textbf{Project Name}}{Date}

  EXCEPTION: The SKILLS section uses a direct \\begin{itemize}[leftmargin=0in,label={}] block.
  Copy that structure from the template EXACTLY as shown.

========================================================
BRACE AND BRACKET SAFETY (FATAL IF BROKEN)
========================================================

- Every \\{ must be closed with \\}.
- Every \\begin{X} must be closed with \\end{X}.
- In optional arguments [...], NEVER put an unmatched } before the closing ].
  CORRECT:   [leftmargin=0in,label={}]    (closing ] is OUTSIDE the {})
  WRONG:     [leftmargin=0in,label={]}    (closing ] is INSIDE the {} — FATAL)
- \\textbf{text} — the } must come after the full text.
- \\href{url}{display} — two separate brace groups, both must close.

========================================================
TEXT ESCAPING — REQUIRED FOR SPECIAL CHARACTERS
========================================================

In ALL regular text content, escape these characters:

  &   →  \\&
  %%   →  \\%%
  $   →  \\$
  #   →  \\#
  _   →  \\_   (underscores in text — e.g. variable_name → variable\\_name)
  ~   →  \\textasciitilde{}
  ^   →  \\textasciicircum{}

URLs inside \\href{} are the only place raw & and %% are allowed.

========================================================
CONTENT RULES
========================================================

- Keep the resume to ONE page.
- Maximum 3 bullet points per job/project.
- Be concise and ATS-optimized.
- Tailor bullet points to match keywords from the job description.
- Do NOT add sections not present in the template.
- Do NOT remove sections present in the template.
- Use real dates, names, and content from the parsed resume data.

========================================================
INPUT TEMPLATE (COPY STRUCTURE — REPLACE CONTENT ONLY)
========================================================
%s

========================================================
PARSED RESUME DATA
========================================================
%s

========================================================
JOB DESCRIPTION (TAILOR CONTENT TO THIS)
========================================================
%s

========================================================
OUTPUT INSTRUCTIONS — READ CAREFULLY
========================================================

- Output ONLY the raw LaTeX document.
- Start your output with: \\documentclass
- End your output with: \\end{document}
- Do NOT output any markdown, code fences, backticks, or explanation text.
- Do NOT output ``` or ```latex before or after.
- No commentary. No "Here is the resume:". Just raw LaTeX.
"""
                .formatted(templateForPrompt, parsedResumeJson, jobDescription);
    }
}
