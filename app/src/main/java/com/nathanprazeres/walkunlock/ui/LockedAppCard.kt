package com.nathanprazeres.walkunlock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.nathanprazeres.walkunlock.models.LockedApp


@Composable
fun LockedAppCard(app: LockedApp, availableSteps: Int, onRemove: () -> Unit, onClick: () -> Unit) {
    val appName = app.appName
    val costPerMinute = app.costPerMinute

    val minutesAvailable =
        if (costPerMinute > 0) availableSteps / costPerMinute else "Unlimited usage available"
    val isUsable = availableSteps >= costPerMinute

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .alpha(if (isUsable) 1f else 0.5f)
                )

                Text(
                    appName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.alpha(if (isUsable) 1f else 0.5f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (costPerMinute > 0) "$costPerMinute steps/min : $minutesAvailable minute${if (minutesAvailable != 1) "s" else ""} available" else minutesAvailable.toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(if (isUsable) 1f else 0.5f),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            LinearProgressIndicator(
                progress = {
                    if (availableSteps >= costPerMinute) 1f
                    else (availableSteps.toFloat() / costPerMinute).coerceAtMost(1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isUsable) 1f else 0.5f)
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
