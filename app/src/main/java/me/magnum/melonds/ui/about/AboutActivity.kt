package me.magnum.melonds.ui.about

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.ui.theme.MelonTheme

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MelonTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    val mintColor = ComposeColor(0xFF00BFA5)
    val kofiColor = ComposeColor(0xFFFF5E5B)

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            // ── 앱 아이콘 + 이름 ──
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(mintColor, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        Modifier
                            .width(44.dp).height(18.dp)
                            .background(ComposeColor.White, RoundedCornerShape(3.dp))
                    )
                    Box(
                        Modifier
                            .width(40.dp).height(2.dp)
                            .background(ComposeColor.White.copy(alpha = 0.3f))
                    )
                    Box(
                        Modifier
                            .width(44.dp).height(18.dp)
                            .background(ComposeColor.White.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "LiteDS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.about_version, versionName),
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_tagline),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            // ── 개발자 후원 ──
            SectionHeader(title = stringResource(R.string.about_support_title), color = mintColor)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_support_desc),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))

            // Ko-fi 버튼
            DonationButton(
                label = stringResource(R.string.about_kofi),
                subLabel = stringResource(R.string.about_kofi_url).removePrefix("https://"),
                backgroundColor = kofiColor,
                textColor = ComposeColor.White,
                onClick = { openUrl(context.getString(R.string.about_kofi_url)) }
            )

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            // ── 오픈소스 ──
            SectionHeader(title = stringResource(R.string.about_opensource_title), color = ComposeColor(0xFF888888))
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colors.surface.copy(alpha = 0.6f),
                elevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.about_opensource_desc),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.clickable { openUrl(context.getString(R.string.about_github_url)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_github_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mintColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = mintColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.about_melonds_credit),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clickable { openUrl(context.getString(R.string.about_melonds_url)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_melonds_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mintColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = mintColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            // ── 링크 ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                elevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val packageName = context.packageName
                            try {
                                openUrl("market://details?id=$packageName")
                            } catch (e: Exception) {
                                openUrl("https://play.google.com/store/apps/details?id=$packageName")
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.about_review_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: ComposeColor) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = color,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DonationButton(
    label: String,
    subLabel: String,
    backgroundColor: ComposeColor,
    textColor: ComposeColor,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        elevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(subLabel, fontSize = 11.sp, color = textColor.copy(alpha = 0.75f))
            }
            Text("→", fontSize = 16.sp, color = textColor.copy(alpha = 0.7f))
        }
    }
}
