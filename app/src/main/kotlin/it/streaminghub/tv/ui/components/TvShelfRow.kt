package it.streaminghub.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvShelfRow(
    title: String,
    items: List<MediaItemDto>,
    modifier: Modifier = Modifier,
    serverUrl: String? = null,
    countSubtitle: String? = null,
    progressMap: Map<String, Float> = emptyMap(),
    onItemFocused: ((MediaItemDto) -> Unit)? = null,
    onItemClick: (MediaItemDto) -> Unit
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Shelf Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (!countSubtitle.isNullOrEmpty()) {
                Text(
                    text = " • $countSubtitle",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal TV Row with 48dp overscan padding
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.id }) { item ->
                val progress = progressMap[item.id] ?: 0f
                TvPosterCard(
                    item = item,
                    serverUrl = serverUrl,
                    progressPercent = progress,
                    onFocused = { onItemFocused?.invoke(item) },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
