import re

content = """package com.example.ui

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.core.spans.CodeSpan
import java.util.regex.Pattern

class SyntaxHighlightPlugin : AbstractMarkwonPlugin() {

    private val keywordColor = Color.parseColor("#C678DD")
    private val stringColor = Color.parseColor("#98C379")
    private val commentColor = Color.parseColor("#5C6370")
    private val numberColor = Color.parseColor("#D19A66")
    private val functionColor = Color.parseColor("#61AFEF")

    private val keywordPattern = Pattern.compile("\\\\b(public|private|protected|class|fun|var|val|if|else|for|while|return|import|package|def|String|Int|Boolean|null|true|false|const|let|var|function|await|async)\\\\b")
    private val stringPattern = Pattern.compile("\\"[^\\"]*\\"|'[^']*'")
    private val commentPattern = Pattern.compile("//.*|/\\\\*[\\\\s\\\\S]*?\\\\*/")
    private val numberPattern = Pattern.compile("\\\\b\\\\d+\\\\b")
    private val functionPattern = Pattern.compile("\\\\b([a-zA-Z_][a-zA-Z0-9_]*)\\\\s*\\\\(")

    override fun beforeSetText(textView: android.widget.TextView, markdown: android.text.Spanned) {
        if (markdown is Spannable) {
            val codeBlocks = markdown.getSpans(0, markdown.length, CodeBlockSpan::class.java)
            for (block in codeBlocks) {
                val start = markdown.getSpanStart(block)
                val end = markdown.getSpanEnd(block)
                highlight(markdown, start, end)
            }
            
            val inlineCodes = markdown.getSpans(0, markdown.length, CodeSpan::class.java)
            for (inline in inlineCodes) {
                val start = markdown.getSpanStart(inline)
                val end = markdown.getSpanEnd(inline)
                highlight(markdown, start, end)
            }
        }
    }

    private fun highlight(spannable: Spannable, startOffset: Int, endOffset: Int) {
        val text = spannable.subSequence(startOffset, endOffset).toString()

        applySpan(spannable, text, keywordPattern, keywordColor, startOffset)
        applySpan(spannable, text, functionPattern, functionColor, startOffset, 1)
        applySpan(spannable, text, numberPattern, numberColor, startOffset)
        applySpan(spannable, text, stringPattern, stringColor, startOffset)
        applySpan(spannable, text, commentPattern, commentColor, startOffset)
    }

    private fun applySpan(spannable: Spannable, text: String, pattern: Pattern, color: Int, offset: Int, group: Int = 0) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start(group) + offset
            val end = matcher.end(group) + offset
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
"""

with open('app/src/main/java/com/example/ui/SyntaxHighlightPlugin.kt', 'w') as f:
    f.write(content)
