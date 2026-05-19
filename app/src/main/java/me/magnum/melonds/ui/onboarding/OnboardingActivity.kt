package me.magnum.melonds.ui.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.common.Permission
import me.magnum.melonds.common.contracts.DirectoryPickerContract
import me.magnum.melonds.ui.romlist.RomListActivity
import me.magnum.melonds.ui.theme.MelonTheme

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MelonTheme {
                OnboardingScreen(
                    onFinish = { selectedUri ->
                        val intent = Intent(this, RomListActivity::class.java).apply {
                            if (selectedUri != null) {
                                putExtra(RomListActivity.EXTRA_ROM_DIRECTORY_URI, selectedUri.toString())
                            }
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: (selectedUri: android.net.Uri?) -> Unit) {
    val onboardingViewModel: OnboardingViewModel = viewModel()
    var currentStep by remember { mutableStateOf(1) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val mintColor = ComposeColor(0xFF00BFA5)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = DirectoryPickerContract(Permission.READ_WRITE)
    ) { uri ->
        selectedUri = uri
        currentStep = 3
    }

    BackHandler(enabled = currentStep > 1) {
        currentStep -= 1
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        when (currentStep) {
            1 -> OnboardingStep1(
                mintColor = mintColor,
                onNext = { currentStep = 2 },
                onSkip = {
                    onboardingViewModel.markOnboardingComplete()
                    onFinish(null)
                }
            )
            2 -> OnboardingStep2(
                mintColor = mintColor,
                onPickFolder = { folderPickerLauncher.launch(null) },
                onSkip = { currentStep = 3 }
            )
            3 -> OnboardingStep3(
                mintColor = mintColor,
                onFinish = {
                    onboardingViewModel.markOnboardingComplete()
                    onFinish(selectedUri)
                }
            )
        }
    }
}

@Composable
private fun OnboardingStep1(mintColor: ComposeColor, onNext: () -> Unit, onSkip: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(mintColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.width(54.dp).height(22.dp).background(ComposeColor.White, RoundedCornerShape(3.dp)))
                    Box(Modifier.width(50.dp).height(2.dp).background(ComposeColor.White.copy(alpha = 0.3f)))
                    Box(Modifier.width(54.dp).height(22.dp).background(ComposeColor.White.copy(alpha = 0.65f), RoundedCornerShape(3.dp)))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(text = stringResource(R.string.onboarding_step1_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_step1_desc),
                fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(32.dp))
            PageIndicator(currentPage = 0, totalPages = 3, activeColor = mintColor)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = mintColor)
            ) {
                Text(stringResource(R.string.onboarding_step1_button), color = ComposeColor.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OnboardingStep2(mintColor: ComposeColor, onPickFolder: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📁", fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_step2_title),
            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, lineHeight = 32.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_step2_desc),
            fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(32.dp))
        PageIndicator(currentPage = 1, totalPages = 3, activeColor = mintColor)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = mintColor)
        ) {
            Text(stringResource(R.string.onboarding_step2_button), color = ComposeColor.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_step2_skip))
        }
    }
}

@Composable
private fun OnboardingStep3(mintColor: ComposeColor, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✓", fontSize = 72.sp, textAlign = TextAlign.Center, color = mintColor)
        Spacer(Modifier.height(24.dp))
        Text(text = stringResource(R.string.onboarding_step3_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_step3_desc),
            fontSize = 15.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(32.dp))
        PageIndicator(currentPage = 2, totalPages = 3, activeColor = mintColor)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = mintColor)
        ) {
            Text(stringResource(R.string.onboarding_step3_button), color = ComposeColor.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, totalPages: Int, activeColor: ComposeColor) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .background(
                        color = if (index == currentPage) activeColor else activeColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}
