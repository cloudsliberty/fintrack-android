@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrack.android.R
import com.fintrack.android.BuildConfig

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("About") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ft_logo),
                contentDescription = "FinTrack",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(96.dp).padding(bottom = 8.dp)
            )
            Text("FinTrack", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "A full-featured personal finance manager, built directly into Nextcloud. " +
                            "Track accounts, income, expenses, transfers, recurring bills, and budgets — " +
                            "all stored in your own Nextcloud database, under your own control.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Developer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Abdul Jaleel Adenpulan", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "github.com/cloudsliberty/fintrack-android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickableLink(context, "https://github.com/cloudsliberty/fintrack-android")
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("License: AGPL-3.0-or-later", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FinTrack Backend (Nextcloud app)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "github.com/cloudsliberty/fintrack",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickableLink(context, "https://github.com/cloudsliberty/fintrack")
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Support the project", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/jaleel1618"))) },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0070BA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Donate with PayPal")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/jaleel1618"))) },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF5E5B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.LocalCafe, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Support on Ko-fi")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.CreditCard, contentDescription = "Card payments accepted", modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

private fun Modifier.clickableLink(context: android.content.Context, url: String): Modifier =
    this.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
