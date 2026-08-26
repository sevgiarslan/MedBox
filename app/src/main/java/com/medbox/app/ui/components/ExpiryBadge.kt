package com.medbox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medbox.app.data.ExpiryStatus
import com.medbox.app.data.Medicine
import com.medbox.app.data.expiryStatus
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun ExpiryBadge(medicine: Medicine, modifier: Modifier = Modifier) {
    val status = medicine.expiryStatus()
    val (badgeColor, textColor, label) = when (status) {
        ExpiryStatus.EXPIRED -> Triple(Color(0xFFFFCDD2), Color(0xFFB71C1C), "Süresi Doldu")
        ExpiryStatus.EXPIRING_SOON -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), "Yakında Doluyor")
        ExpiryStatus.OK -> Triple(Color(0xFFC8E6C9), Color(0xFF2E7D32), "Uygun")
    }

    Text(
        text = "$label · ${medicine.expirationDate.format(dateFormatter)}",
        color = textColor,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(badgeColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
