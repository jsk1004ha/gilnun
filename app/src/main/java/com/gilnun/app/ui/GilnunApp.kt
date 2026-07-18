package com.gilnun.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gilnun.app.DemoLayout
import com.gilnun.app.DemoRole
import com.gilnun.app.GilnunUiState
import com.gilnun.app.GilnunViewModel
import com.gilnun.app.data.ReceiptOutcome
import com.gilnun.app.web.DemoWebView

@Composable
fun GilnunApp(viewModel: GilnunViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.helpPromptVisible) {
        HelpConfirmationDialog(
            fromFriction = state.helpPromptFromFriction,
            onAccept = viewModel::acceptHelp,
            onDecline = viewModel::declineHelp,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(state, viewModel::resetDemo)
            ModeControls(
                state = state,
                onRole = viewModel::setRole,
                onLayout = viewModel::setLayout,
            )
            StatusBanner(state.message)
            GuidanceCard(
                state = state,
                onHelp = { viewModel.requestHelp(direct = true) },
                onReplay = viewModel::replayPatch,
                onMismatch = viewModel::demonstrateMismatch,
            )
            DemoWebView(
                layoutVariant = state.layout.name,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .clip(RoundedCornerShape(20.dp)),
                command = state.webCommand,
                onEvent = viewModel::onEvent,
                onBridgeStatus = viewModel::onBridgeStatus,
                onSecurityEvent = viewModel::onSecurityEvent,
            )
            ReceiptCard(state)
            ScopeNote()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(
    state: GilnunUiState,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "길눈 AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "오프라인 복지 신청 길찾기",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        TextButton(onClick = onReset) {
            Text("Demo Reset")
        }
    }
    Text(
        text = "브리지 · ${state.bridgeLabel}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun ModeControls(
    state: GilnunUiState,
    onRole: (DemoRole) -> Unit,
    onLayout: (DemoLayout) -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("역할", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoRole.entries.forEach { role ->
                    FilterChip(
                        selected = state.role == role,
                        onClick = { onRole(role) },
                        label = {
                            Text(if (role == DemoRole.LEARNER) "학습자" else "도우미")
                        },
                    )
                }
            }
            Text("레이아웃", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoLayout.entries.forEach { layout ->
                    FilterChip(
                        selected = state.layout == layout,
                        onClick = { onLayout(layout) },
                        label = { Text("레이아웃 ${layout.name}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GuidanceCard(
    state: GilnunUiState,
    onHelp: () -> Unit,
    onReplay: () -> Unit,
    onMismatch: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("도움 정류장", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "현재 도움 · 레벨 ${state.helpLevel}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    if (state.patch == null) "패치 없음" else "PatchV1 준비",
                    color =
                        if (state.patch == null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (state.patch == null) {
                    "임시 저장을 6초 안에 세 번 누르거나 도움을 요청한 뒤, 도우미가 ‘신청 내용 확인’을 한 번 선택합니다."
                } else {
                    "여섯 의미 필드가 정확히 일치하는 대상 하나만 강조합니다. 앱이 대신 누르지 않습니다."
                },
            )
            Button(
                onClick = onHelp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("직접 도움 요청")
            }
            OutlinedButton(
                onClick = onReplay,
                enabled = state.patch != null && state.bridgeAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("레이아웃 B에서 패치 재사용")
            }
            if (!state.bridgeAvailable) {
                Text(
                    text = "안전한 로컬 브리지가 준비되면 패치 재사용을 시작할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            TextButton(
                onClick = onMismatch,
                enabled = state.patch != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("불일치 실패 안전 확인")
            }
        }
    }
}

@Composable
private fun ReceiptCard(state: GilnunUiState) {
    val receipt = state.receipt
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (receipt?.outcome == ReceiptOutcome.VERIFIED) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("검증 영수증", style = MaterialTheme.typography.labelLarge)
            Text(
                receipt?.outcome?.name ?: "아직 발급 전",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            HorizontalDivider()
            ReceiptLine("guidanceShown", receipt?.guidanceShown == true)
            ReceiptLine("userActionObserved", receipt?.userActionObserved == true)
            ReceiptLine("postconditionVerified", receipt?.postconditionVerified == true)
            Text(
                "클릭만으로는 완료가 아닙니다. review-ready 사후조건까지 확인된 경우만 VERIFIED입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ReceiptLine(
    label: String,
    value: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(if (value) "TRUE" else "FALSE", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScopeNote() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text =
                "통제된 합성 WebView와 규칙 기반 시제품입니다. 실제 개인정보, 외부 앱, 결제, 동의, 최종 제출을 다루지 않습니다.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HelpConfirmationDialog(
    fromFriction: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("도움이 필요하신가요?") },
        text = {
            Text(
                if (fromFriction) {
                    "같은 비진행 동작이 6초 안에 세 번 관찰됐습니다. 도우미는 다음 버튼의 의미만 기록하고 대신 누르지 않습니다."
                } else {
                    "도우미는 다음 버튼의 의미만 기록하고 대신 누르거나 제출하지 않습니다."
                },
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("도움 받기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("지금은 괜찮아요")
            }
        },
    )
}
