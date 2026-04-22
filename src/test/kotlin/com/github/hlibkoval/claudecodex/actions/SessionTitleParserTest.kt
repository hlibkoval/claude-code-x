package com.github.hlibkoval.claudecodex.actions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SessionTitleParserTest {

    companion object {
        private val fixtures: List<SessionTitleFixture> by lazy { FixtureLoader.loadSessionTitleFixtures() }

        private val expected: Map<String, String> = mapOf(
            "plain-obsidian-vault" to "This is an obsidian vault for my work-related stuff. Move relevant stuff from my personal vault at @../Everything/CLAUDE.md to @CLAUDE.md here",
            "plain-short-sentence" to "The open questions block should be laid out with space to put the actual answers in",
            "plain-with-path-and-newline" to "Links starting at home aren't being detected to make navigable. Example: ~/Work/consumer/docs-resources/research-documents/2026/MCOM-1527/investigation.md",
            "plain-with-url-newline" to "Clone a local copy of intellij idea for reference. Gitignore it. https://github.com/JetBrains/intellij-community.git",
            "plain-multiline-multi-paragraph" to "Scrap and then re-design from ground up the sessions name parsing for the session resume picker. Think of a robust implementation. Add a testing framework. It has to strip both full-blown XML xml tag wraps, and partial too. Use a sub-agent to remove the current implementation so you don't see the current implementation, which may influence your thinking. Then launch a plan mode to design a new implementation.",
            "plain-simple-directive" to "Remove the \"dangerously-skip-permissions\" flag in the claude code launch button",
            "plain-separate-panel" to "I want our plugin to live in a completely separate panel, not in the \"Terminal\". Duplicating and stripping down the terminal logic comes to mind. Explore our options.",
            "bracketed-interrupt" to "[Request interrupted by user]",
            "command-name-only-review" to "/review MSUB-2152",
            "command-name-deploy-promote" to "/deploy-promote MBIL rc prod",
            "command-name-compush-no-args" to "/compush",
            "command-name-args-with-sentence" to "/issue-start MHBO-66 . The ticket was deployed to prod. We need to update the migration with up to date user id's",
            "command-name-namespaced-skill" to "/skill-creator:skill-creator update '/review-review' skill to include line numbers in links using ':[number]' notation. Instead of this: - foo/Bar.java — client sends JSON body, not query params (line 94) Do this: - foo/Bar.java:94 — client sends JSON body, not query params",
            "command-name-clear-indented" to "/clear",
            "command-name-permissions-empty-args" to "/permissions",
            "command-name-ordered-differently" to "/compush",
            "local-command-caveat-only" to "",
            "local-command-stdout-no-content" to "(no content)",
            "local-command-stdout-copied" to "Copied to clipboard (362 characters, 8 lines) Also written to /var/folders/cq/0lj3ty_s1x9czq13mwq1s7p00000gn/T/claude/response.md",
            "bash-input-curl" to "curl -fsSL https://github.com/hlibkoval/vault-code/releases/latest/download/vault-code.tar.gz | tar -xzv -C .obsidian/plugins/",
            "bash-stdout-with-adjacent-stderr" to "x vault-code/ x vault-code/manifest.json x vault-code/styles.css x vault-code/main.js x vault-code/symbols-nerd-font.woff2",
            "scheduled-task-with-attributes-and-body" to "This is an automated run of a scheduled task. The user is not present to answer questions. For implementation details, execute autonomously without asking clarifying questions — make reasonable choices and note them in your output. However, for \"write\" actions (e.g. MCP tools that send, post, create, update, or delete), only take them if the task file asks for that specific action. When in doubt, producing a report of what you found is the correct output. Pull master. Run '/update' skill. When the skill finishes producing new version, commit and push to master.",
            "task-notification-with-trailing-plain-text" to "Read the output file to retrieve the result: /private/tmp/claude-501/-Users-gleb-Projects-claudemd/607cac75-7b85-42b7-a225-073bc10cea9c/tasks/b9g8d0nd2.output",
            "synthetic-system-reminder-wrapper" to "Refactor the telemetry batcher to drop retries after 3 failures.",
            "synthetic-task-wrapper" to "Investigate why the websocket reconnect loop never terminates when the server is down.",
            "synthetic-partial-opening-tag-only" to "Refactor the telemetry batcher to drop retries after 3 failures and log once.",
            "synthetic-partial-closing-tag-only" to "Refactor the telemetry batcher to drop retries after 3 failures.",
            "synthetic-nested-tags" to "/refactor Drop the retry loop and replace with exponential backoff.",
            "synthetic-unmatched-open-tag-midstream" to "Compare a < b and also check the guard in validator.rs",
            "synthetic-code-fence-inside-body" to "```python def foo(): return 1 < 2 ``` Please explain what this does.",
            "synthetic-html-entities" to "Compare &lt;a&gt; and &lt;b&gt; — which is bigger?",
            "synthetic-leading-whitespace-then-tag" to "/review PR-123",
            "synthetic-only-whitespace" to "",
            "synthetic-empty-string" to "",
        )

        @JvmStatic
        fun fixtureArgs(): Stream<Arguments> =
            fixtures.stream().map { Arguments.of(it.label, it.body) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureArgs")
    fun `parse produces expected title for fixture`(label: String, body: String) {
        val want = expected[label]
            ?: fail<String>("No expected output for fixture '$label'. Add it to the expected map.")
        val got = SessionTitleParser.parse(body)
        assertEquals(want, got, "fixture '$label'")
    }

    @Test
    fun `every fixture has an expected mapping and vice versa`() {
        val labels = fixtures.map { it.label }.toSet()
        val missing = labels - expected.keys
        val orphaned = expected.keys - labels
        assertTrue(missing.isEmpty()) { "Fixtures without expected entries: $missing" }
        assertTrue(orphaned.isEmpty()) { "Expected entries without fixtures: $orphaned" }
    }

    @Test
    fun `parse does not throw on adversarial input`() {
        val adversarial = listOf(
            "<<<",
            ">>>",
            "</>",
            "<a<b>c</b</a>",
            "<",
            ">",
            "<>",
            "<<>>",
            "<tag",
            "tag>",
            "<!-- html comment -->",
            "<?xml version=\"1.0\"?>",
            "<![CDATA[foo]]>",
            "&amp;&lt;&gt;",
            "🎉 unicode <tag>emoji</tag> 🚀",
            "<".repeat(1000) + ">".repeat(1000),
            "a".repeat(10_000),
            "<command-name>/x</command-args>",
            "<command-name>/x<command-args>y</command-args>",
        )
        for (input in adversarial) {
            try {
                SessionTitleParser.parse(input)
            } catch (e: Throwable) {
                fail<Unit>("parse threw for input '${input.take(40)}': $e")
            }
        }
    }

    @Test
    fun `parse is idempotent on all fixtures`() {
        for (fixture in fixtures) {
            val once = SessionTitleParser.parse(fixture.body)
            val twice = SessionTitleParser.parse(once)
            assertEquals(once, twice, "idempotence violated for fixture '${fixture.label}'")
        }
    }

    @Test
    fun `parse output never contains an un-stripped opening tag`() {
        val tagLike = Regex("<[a-zA-Z]")
        for (fixture in fixtures) {
            val got = SessionTitleParser.parse(fixture.body)
            assertFalse(tagLike.containsMatchIn(got)) {
                "fixture '${fixture.label}' produced un-stripped tag-like content: $got"
            }
        }
    }
}
