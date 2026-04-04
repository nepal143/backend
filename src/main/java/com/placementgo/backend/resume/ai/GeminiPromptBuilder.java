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
<<<<<<< HEAD
You are a precise LaTeX content editor. Replace placeholder content in the given template with real resume data.
=======
You are an expert resume writer and LaTeX editor. Your PRIMARY goal is to produce a resume that is \
deeply tailored to the given job description — not simply to reproduce the template with swapped names.
>>>>>>> 5bc2359127445f54d847b21013fd7d6f916d9b6b

========================================================
ABSOLUTE RULES — VIOLATING THESE CAUSES FATAL COMPILE ERRORS
========================================================

1. DO NOT modify \\documentclass — copy it EXACTLY as in the template.
2. DO NOT add or remove \\usepackage lines.
3. DO NOT change \\newcommand definitions.
4. DO NOT invent macros not defined in the template.
<<<<<<< HEAD
=======
5. NEVER place \\documentclass, \\usepackage, \\newcommand, \\renewcommand, \\definecolor,
   or \\pagestyle inside \\begin{document} ... \\end{document}. These are preamble-only
   commands. Putting them inside the document body causes a fatal compile error.
   The preamble is handled separately — output ONLY the document body content inside
   \\begin{document} ... \\end{document}.
>>>>>>> 5bc2359127445f54d847b21013fd7d6f916d9b6b

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
<<<<<<< HEAD
=======
    ⚠ ALL 4 arguments are ALWAYS required. If Location is not available, write {}. \
NEVER omit the 4th brace group — missing it causes a FATAL compile error.
>>>>>>> 5bc2359127445f54d847b21013fd7d6f916d9b6b

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
<<<<<<< HEAD
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
=======
JOB-DESCRIPTION TAILORING — THIS IS THE MOST IMPORTANT STEP
========================================================

Read the job description carefully BEFORE writing a single word.

1. KEYWORDS: Identify every required skill, technology, tool, methodology, and responsibility
   mentioned in the job description. Weave these exact keywords naturally into the resume's
   bullet points, skills section, and project descriptions wherever they honestly apply.

2. REFRAME BULLETS: Do not copy bullet points verbatim from the parsed resume. Rewrite each
   bullet to emphasise the aspects most relevant to this specific job. Lead with strong action
   verbs that match the language used in the job description.

3. SKILLS SECTION: Re-order and re-categorise skills so that those matching the job description
   appear first and most prominently.

4. SUMMARY / OBJECTIVE (if the template has one): Write it entirely targeting this role —
   mirror the job title and key competencies from the job description.

========================================================
SECTION MANAGEMENT — ADAPT THE RESUME TO THE CANDIDATE
========================================================

The template is a FORMATTING REFERENCE ONLY. Treat the section list as flexible:

- REMOVE a section entirely if the candidate has no meaningful content for it.
  (e.g. no publications, no certifications — just omit the section; do NOT leave it with fake data)

- KEEP the section's LaTeX structure (macros, indentation, heading style) EXACTLY as shown in
  the template — only the content changes.

- ADD extra sections if the candidate has relevant content AND the resume still fits ONE page.
  Allowed extras: Certifications, Awards, Publications, Volunteer Work, Languages.
  Use the same macro / formatting pattern as the nearest similar section in the template.

- REORDER sections to put the most job-relevant content highest on the page
  (e.g. if it is a project-heavy role, move Projects above Experience).

========================================================
AGGRESSIVE CONTENT PRUNING — MANDATORY FOR ONE PAGE
========================================================

You MUST actively remove content that does not help this candidate get THIS specific job.
This is not optional. An overflowing resume is a FAILED output.

WHAT TO CUT (in order of priority):
1. ENTIRE PROJECTS that use zero technologies or skills mentioned in the job description.
   → If a project has no overlap with the JD, delete it completely.
2. BULLET POINTS within a role that describe work completely unrelated to the JD.
   → Keep only bullets that mention skills, tools, or responsibilities from the JD.
3. ENTIRE JOB ROLES if that position has no relevance to the target role.
   → If an old job has nothing to do with the JD, remove the whole entry.
4. EXTRA SECTIONS (certifications, awards, etc.) if they don't add JD-relevant value.
5. SKILLS that are not mentioned anywhere in the job description and are not core industry skills.

WHAT TO NEVER CUT:
- Most recent 1-2 job roles (even if only partially relevant — keep and tailor them).
- Education section.
- Any skill, project, or bullet that directly matches a JD requirement.

BULLET POINT LIMITS (enforce strictly):
- Maximum 3 bullets per job role.
- Maximum 2 bullets per project.
- If still overflowing: reduce to 2 bullets per role, 1 per project.

========================================================
ONE-PAGE BUDGET — THIS IS NON-NEGOTIABLE
========================================================

The final PDF MUST fit on exactly ONE page. There is NO acceptable reason for a second page.

Mandatory space-saving rules:
- Enforce the bullet point limits above — never exceed them.
- Drop any section that is not adding real value for this job.
- If still tight, reduce \\vspace between sections by adding \\vspace{-4pt} before section headings.
- Never invent fake experience, education, or projects not present in the candidate's data.
- If after removing everything low-priority the content still does not fit: further shorten
  remaining bullet point text — cut wordy phrases, keep the impact keyword only.

========================================================
INPUT TEMPLATE (FORMATTING REFERENCE — STRUCTURE ONLY)
>>>>>>> 5bc2359127445f54d847b21013fd7d6f916d9b6b
========================================================
%s

========================================================
PARSED RESUME DATA
========================================================
%s

========================================================
<<<<<<< HEAD
JOB DESCRIPTION (TAILOR CONTENT TO THIS)
=======
JOB DESCRIPTION (PRIMARY GUIDE — TAILOR EVERYTHING TO THIS)
>>>>>>> 5bc2359127445f54d847b21013fd7d6f916d9b6b
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
