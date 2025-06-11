package com.gmail.nathanprazeres.walkunlock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun StepCard(availableSteps: Int, totalSteps: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Steps",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Available / Total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "$availableSteps / $totalSteps",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
            LinearProgressIndicator(
                progress = {
                    if (totalSteps > 0) {
                        availableSteps.toFloat() / totalSteps
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
