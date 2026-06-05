package me.magnum.melonds.ui.emulator.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import coil.compose.AsyncImage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.JoystickDirectionMode
import me.magnum.melonds.domain.model.SaveStateSlot
import me.magnum.melonds.ui.emulator.EmulatorViewModel
import me.magnum.melonds.ui.emulator.PauseMenuState
import me.magnum.melonds.ui.emulator.rom.RomPauseMenuOption
import me.magnum.melonds.ui.theme.MelonTheme
import java.text.SimpleDateFormat
import java.util.Locale

private enum class SlotAction { SAVE, LOAD }

@AndroidEntryPoint
class PauseMenuBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: EmulatorViewModel by activityViewModels()

    /** 액션 없이 닫힐 때만 resumeEmulator() 호출 */
    private var actionTaken = false

    /** Activity가 주입하는 dismiss 콜백 — overlay 해제 등 처리 */
    var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MelonTheme {
                    val state by viewModel.pauseMenuState.collectAsState()
                    val fastForwardSpeed by viewModel.fastForwardSpeed.collectAsState()
                    val hideAuxiliaryButtons by viewModel.hideAuxiliaryButtons.collectAsState()
                    val joystickDirectionMode by viewModel.joystickDirectionMode.collectAsState()
                    state?.let { pauseState ->
                        PauseMenuSheetContent(
                            state = pauseState,
                            fastForwardSpeed = fastForwardSpeed,
                            hideAuxiliaryButtons = hideAuxiliaryButtons,
                            joystickDirectionMode = joystickDirectionMode,
                            onResume = {
                                actionTaken = true
                                viewModel.resumeEmulator()
                                dismiss()
                            },
                            onSave = { slot ->
                                actionTaken = true
                                viewModel.saveStateToSlot(slot)
                                dismiss()
                            },
                            onLoad = { slot ->
                                actionTaken = true
                                viewModel.loadStateFromSlot(slot)
                                dismiss()
                            },
                            onReset = {
                                actionTaken = true
                                viewModel.resetEmulator()
                                viewModel.resumeEmulator()
                                dismiss()
                            },
                            onExit = {
                                actionTaken = true
                                dismiss()
                                viewModel.exitEmulator(false)
                            },
                            onCheats = {
                                actionTaken = true
                                dismiss()
                                viewModel.onPauseMenuOptionSelected(RomPauseMenuOption.CHEATS)
                            },
                            onDeleteSlot = { slot ->
                                viewModel.deleteSaveStateSlot(slot)
                                viewModel.refreshPauseMenuSaveSlots()
                            },
                            onSpeedChange = { viewModel.setFastForwardSpeed(it) },
                            onHideAuxiliaryButtonsChange = { viewModel.setHideAuxiliaryButtons(it) },
                            onJoystickDirectionModeChange = { viewModel.setJoystickDirectionMode(it) },
                        )
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!actionTaken) {
            viewModel.resumeEmulator()
        }
        viewModel.dismissPauseMenu()
        onDismissCallback?.invoke()
    }
}

// ──────────────────────────────────────────────────
// Composables
// ──────────────────────────────────────────────────

@Composable
private fun PauseMenuSheetContent(
    state: PauseMenuState,
    fastForwardSpeed: Float,
    hideAuxiliaryButtons: Boolean,
    joystickDirectionMode: JoystickDirectionMode,
    onResume: () -> Unit,
    onSave: (SaveStateSlot) -> Unit,
    onLoad: (SaveStateSlot) -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    onCheats: () -> Unit,
    onDeleteSlot: (SaveStateSlot) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onHideAuxiliaryButtonsChange: (Boolean) -> Unit,
    onJoystickDirectionModeChange: (JoystickDirectionMode) -> Unit,
) {
    val mintColor = Color(0xFF00BFA5)
    var pendingSlot by remember { mutableStateOf<SaveStateSlot?>(null) }
    var slotAction by remember { mutableStateOf(SlotAction.SAVE) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // 드래그 핸들
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Divider(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                )
            }
            Spacer(Modifier.height(12.dp))

            // ROM 이름
            if (state.romName != null) {
                Text(
                    text = state.romName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }

            // 액션 행 1: 계속 / 저장 / 불러오기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PauseActionButton(icon = Icons.Default.PlayArrow, label = stringResource(R.string.pause_menu_resume), mintColor = mintColor, onClick = onResume)
                PauseActionButton(icon = Icons.Default.Save, label = stringResource(R.string.pause_menu_save), mintColor = mintColor, onClick = {
                    slotAction = SlotAction.SAVE
                    val target = state.saveSlots
                        .filter { it.slot != SaveStateSlot.QUICK_SAVE_SLOT }
                        .take(6)
                        .firstOrNull { !it.exists }
                        ?: state.saveSlots.filter { it.slot != SaveStateSlot.QUICK_SAVE_SLOT }.take(6).firstOrNull()
                    pendingSlot = target
                })
                PauseActionButton(icon = Icons.Default.Folder, label = stringResource(R.string.pause_menu_load), mintColor = mintColor, onClick = {
                    slotAction = SlotAction.LOAD
                    val target = state.saveSlots
                        .filter { it.slot != SaveStateSlot.QUICK_SAVE_SLOT && it.exists }
                        .take(6)
                        .maxByOrNull { it.lastUsedDate?.time ?: 0L }
                    pendingSlot = target
                })
            }

            Spacer(Modifier.height(8.dp))

            // 액션 행 2: 재시작 / 나가기 / 치트(옵션)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PauseActionButton(icon = Icons.Default.Refresh, label = stringResource(R.string.pause_menu_reset), mintColor = mintColor, onClick = { showResetConfirm = true })
                PauseActionButton(icon = Icons.AutoMirrored.Filled.ExitToApp, label = stringResource(R.string.pause_menu_exit), mintColor = mintColor, onClick = { showExitConfirm = true })
                if (state.options.any { it is RomPauseMenuOption && it == RomPauseMenuOption.CHEATS }) {
                    PauseActionButton(icon = Icons.Default.BugReport, label = stringResource(R.string.pause_menu_cheats), mintColor = mintColor, onClick = onCheats)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            // 저장 슬롯 스트립 (ROM 실행 중일 때만)
            if (state.saveSlots.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.pause_menu_save_slots),
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = state.saveSlots
                            .filter { it.slot != SaveStateSlot.QUICK_SAVE_SLOT }
                            .take(6),
                        key = { it.slot },
                    ) { slot ->
                        SaveSlotCard(
                            slot = slot,
                            mintColor = mintColor,
                            onTap = {
                                slotAction = SlotAction.SAVE
                                pendingSlot = slot
                            },
                            onLongPress = { onDeleteSlot(slot) },
                        )
                    }
                }
            }

            // 컨트롤 설정 영역
            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(4.dp))
            HideAuxButtonsToggleRow(
                enabled = hideAuxiliaryButtons,
                mintColor = mintColor,
                onToggle = onHideAuxiliaryButtonsChange,
            )
            JoystickDirectionToggleRow(
                mode = joystickDirectionMode,
                mintColor = mintColor,
                onToggle = onJoystickDirectionModeChange,
            )
            SpeedSpinnerRow(
                currentSpeed = fastForwardSpeed,
                mintColor = mintColor,
                onSpeedChange = onSpeedChange,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── 슬롯 선택 미니 다이얼로그 ──
    pendingSlot?.let { slot ->
        val dateStr = slot.lastUsedDate
            ?.let { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(it) }
            ?: stringResource(R.string.pause_menu_empty_slot)
        AlertDialog(
            onDismissRequest = { pendingSlot = null },
            title = { Text(stringResource(R.string.pause_menu_slot_label, slot.slot)) },
            text = { Text(dateStr) },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { pendingSlot = null }) { Text(stringResource(R.string.cancel)) }
                    if (slot.exists) {
                        TextButton(onClick = { pendingSlot = null; onLoad(slot) }) {
                            Text(stringResource(R.string.pause_menu_load))
                        }
                    }
                    TextButton(onClick = { pendingSlot = null; onSave(slot) }) {
                        Text(stringResource(R.string.pause_menu_save), color = mintColor)
                    }
                }
            },
        )
    }

    // ── 재시작 확인 다이얼로그 ──
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.pause_menu_reset_confirm_title)) },
            text = { Text(stringResource(R.string.pause_menu_reset_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; onReset() }) {
                    Text(stringResource(R.string.pause_menu_reset), color = mintColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // ── 나가기 확인 다이얼로그 ──
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.pause_menu_exit_confirm_title)) },
            text = { Text(stringResource(R.string.pause_menu_exit_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onExit() }) {
                    Text(stringResource(R.string.exit), color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PauseActionButton(
    icon: ImageVector,
    label: String,
    mintColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .padding(4.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaveSlotCard(
    slot: SaveStateSlot,
    mintColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .size(width = 72.dp, height = 90.dp)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        border = if (slot.exists) BorderStroke(1.5.dp, mintColor.copy(alpha = 0.6f)) else null,
        elevation = if (slot.exists) 3.dp else 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (slot.exists)
            MaterialTheme.colors.surface
        else
            MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (slot.exists && slot.screenshot != null) {
                AsyncImage(
                    model = slot.screenshot,
                    contentDescription = stringResource(R.string.pause_menu_slot_screenshot, slot.slot),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = slot.slot.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (slot.exists) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(
                        text = slot.slot.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = mintColor,
                    )
                    slot.lastUsedDate?.let {
                        Text(
                            text = SimpleDateFormat("MM/dd", Locale.getDefault()).format(it),
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.pause_menu_empty_slot),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = slot.slot.toString(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HideAuxButtonsToggleRow(
    enabled: Boolean,
    mintColor: Color,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.VideogameAsset,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.pause_menu_hide_aux),
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = mintColor,
                checkedTrackColor = mintColor.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun JoystickDirectionToggleRow(
    mode: JoystickDirectionMode,
    mintColor: Color,
    onToggle: (JoystickDirectionMode) -> Unit,
) {
    val isEightWay = mode == JoystickDirectionMode.EIGHT_WAY
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Gamepad,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.pause_menu_joystick_8way),
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEightWay,
            onCheckedChange = { checked ->
                onToggle(if (checked) JoystickDirectionMode.EIGHT_WAY else JoystickDirectionMode.FOUR_WAY)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = mintColor,
                checkedTrackColor = mintColor.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun SpeedSpinnerRow(
    currentSpeed: Float,
    mintColor: Color,
    onSpeedChange: (Float) -> Unit,
) {
    val speedCycle = listOf(-1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 8.0f)
    val unlimitedLabel = stringResource(R.string.pause_menu_speed_unlimited)
    val labels = mapOf(
        -1.0f to unlimitedLabel,
        1.5f  to "1.5×",
        2.0f  to "2×",
        3.0f  to "3×",
        4.0f  to "4×",
        8.0f  to "8×",
    )
    val currentIndex = speedCycle.indexOf(currentSpeed).takeIf { it >= 0 } ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.FastForward,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.pause_menu_fast_forward),
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                val prev = speedCycle[(currentIndex - 1 + speedCycle.size) % speedCycle.size]
                onSpeedChange(prev)
            },
            modifier = Modifier.size(36.dp),
        ) {
            Text("‹", fontSize = 20.sp, color = mintColor, fontWeight = FontWeight.Bold)
        }
        Text(
            text = labels[currentSpeed] ?: unlimitedLabel,
            fontSize = 14.sp,
            color = mintColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 52.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = {
                val next = speedCycle[(currentIndex + 1) % speedCycle.size]
                onSpeedChange(next)
            },
            modifier = Modifier.size(36.dp),
        ) {
            Text("›", fontSize = 20.sp, color = mintColor, fontWeight = FontWeight.Bold)
        }
    }
}
