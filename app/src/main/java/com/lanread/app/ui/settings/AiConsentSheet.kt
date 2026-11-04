package com.lanread.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.theme.AccentBlue

/** AI 数据权限提示 consent sheet (mirrors iOS AIPrivacyLaunchConsentSheet). */
@Composable
fun AiConsentSheet(
    appSettings: AppSettingsViewModel,
    isLaunchPrompt: Boolean,
    onDismiss: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = { if (!isLaunchPrompt) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(stringResource(R.string.ai_consent_launch_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ai_consent_launch_intro),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            ConsentLine(Icons.Filled.PersonPin, stringResource(R.string.ai_consent_launch_usage))
            ConsentLine(Icons.Filled.AccountBalance, stringResource(R.string.ai_consent_launch_location))
            ConsentLine(Icons.Filled.Link, stringResource(R.string.ai_consent_launch_third_party))
            ConsentLine(Icons.Filled.Badge, stringResource(R.string.ai_consent_launch_third_party_provider_unknown))
            ConsentLine(Icons.Filled.CheckCircle, stringResource(R.string.ai_consent_launch_explicit_permission))

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ai_consent_launch_privacy_policy_link),
                fontSize = 14.sp,
                color = AccentBlue,
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://isla-reader.top/privacy"))
                        )
                    } catch (_: Exception) {
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ai_consent_launch_manage_in_settings),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ai_consent_launch_dont_show_again), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
            }
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    appSettings.recordAiConsent(true, dontShowAgain)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ai_consent_launch_allow), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    appSettings.recordAiConsent(false, dontShowAgain)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ai_consent_launch_decline), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ConsentLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
