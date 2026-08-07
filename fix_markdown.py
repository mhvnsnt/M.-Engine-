import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

deps = """  implementation("io.noties.markwon:core:4.6.2")
  implementation("io.noties.markwon:ext-strikethrough:4.6.2")
  implementation("io.noties.markwon:ext-tables:4.6.2")"""
  
if 'markwon' not in content:
    content = content.replace('implementation(libs.onnxruntime.android)', 'implementation(libs.onnxruntime.android)\n' + deps)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/MarkdownText.kt', 'w') as f:
    f.write("""package com.example.ui

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                if (color != Color.Unspecified) {
                    setTextColor(color.toArgb())
                }
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, text)
            if (color != Color.Unspecified) {
                textView.setTextColor(color.toArgb())
            }
        }
    )
}
""")
