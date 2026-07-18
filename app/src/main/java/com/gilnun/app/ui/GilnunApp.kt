package com.gilnun.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gilnun.app.GilnunScreen
import com.gilnun.app.GilnunUiState
import com.gilnun.app.GilnunViewModel
import com.gilnun.app.R
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import com.gilnun.app.web.DemoWebView

@Composable
fun GilnunApp(viewModel: GilnunViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.helpPromptVisible) {
        HelpChoiceDialog(
            fromFriction = state.helpPromptFromFriction,
            onAutomatic = viewModel::chooseAutomaticGuidance,
            onHelper = viewModel::chooseHelperHandoff,
            onDecline = viewModel::declineHelp,
        )
    }

    when (state.screen) {
        GilnunScreen.HOME ->
            HomeScreen(
                onSelectService = viewModel::selectService,
            )

        GilnunScreen.PRACTICE -> {
            BackHandler(onBack = viewModel::goHome)
            PracticeScreen(
                state = state,
                onHome = viewModel::goHome,
                onRead = viewModel::readGuidance,
                onHelp = viewModel::requestHelp,
                onEvent = viewModel::onBridgeEvent,
                onBridgeStatus = viewModel::onBridgeStatus,
                onSecurityEvent = viewModel::onSecurityEvent,
            )
        }

        GilnunScreen.HELPER_CONFIRM -> {
            BackHandler(onBack = viewModel::cancelHelperHandoff)
            HelperConfirmScreen(
                state = state,
                onConfirm = viewModel::confirmHelperTarget,
                onCancel = viewModel::cancelHelperHandoff,
                onHome = viewModel::goHome,
            )
        }

        GilnunScreen.HAND_BACK -> {
            BackHandler(onBack = viewModel::returnToLearner)
            HandBackScreen(
                state = state,
                onReturn = viewModel::returnToLearner,
                onHome = viewModel::goHome,
            )
        }
    }
}

@Composable
private fun HomeScreen(onSelectService: (ServiceId) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                BrandHeader()
            }
            item {
                PracticeBanner()
            }
            ServiceId.entries.forEach { serviceId ->
                item(key = serviceId.persistedKey) {
                    ServiceCard(
                        serviceId = serviceId,
                        onClick = { onSelectService(serviceId) },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_gilnun),
            contentDescription = "길눈 로고",
            modifier = Modifier.size(92.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "길눈",
                style = MaterialTheme.typography.headlineLarge,
                color = GilnunNavy,
            )
            Text(
                text = "공공서비스를 천천히 연습해요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ServiceCard(
    serviceId: ServiceId,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = GilnunNavy,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                color = GilnunTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(58.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (ServiceId.entries.indexOf(serviceId) + 1).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = serviceId.homeTitle(),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = serviceId.homeDescription(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun PracticeScreen(
    state: GilnunUiState,
    onHome: () -> Unit,
    onRead: () -> Unit,
    onHelp: () -> Unit,
    onEvent: (com.gilnun.app.web.BridgeEventV2) -> Unit,
    onBridgeStatus: (com.gilnun.app.web.BridgeStatus) -> Unit,
    onSecurityEvent: (String) -> Unit,
) {
    val serviceId = state.selectedService ?: return
    val checkpoint = state.checkpoint ?: return
    val service = ServiceCatalog.require(serviceId)
    val stepIndex =
        service.steps
            .indexOfFirst { it.id == checkpoint }
            .takeIf { it >= 0 }
            ?.plus(1)
            ?: service.steps.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            PracticeDock(
                onRead = onRead,
                onHelp = onHelp,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PracticeTopBar(
                serviceId = serviceId,
                stepIndex = stepIndex,
                onHome = onHome,
            )
            PracticeBanner()
            if (state.receiptMessage != null) {
                HumanReceipt(state.receiptMessage)
            }
            if (state.notice != null) {
                Notice(
                    message = state.notice,
                    isError = state.speechUnavailable,
                )
            }
            DemoWebView(
                serviceId = serviceId,
                layout = state.layout,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp)),
                command = state.webCommand,
                onEvent = onEvent,
                onBridgeStatus = onBridgeStatus,
                onSecurityEvent = onSecurityEvent,
            )
        }
    }
}

@Composable
private fun PracticeTopBar(
    serviceId: ServiceId,
    stepIndex: Int,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = onHome,
            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
        ) {
            Text("← 홈으로")
        }
        Text(
            text = serviceId.shortTitle(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${stepIndex.coerceIn(1, 3)} / 3 단계",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PracticeDock(
    onRead: () -> Unit,
    onHelp: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onRead,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
            ) {
                Text("안내 읽기")
            }
            Button(
                onClick = onHelp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
            ) {
                Text("도움이 필요해요")
            }
        }
    }
}

@Composable
private fun HelperConfirmScreen(
    state: GilnunUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onHome: () -> Unit,
) {
    val serviceId = state.selectedService ?: return
    val checkpoint = state.checkpoint ?: return
    val primary =
        ServiceCatalog
            .require(serviceId)
            .requireCheckpoint(checkpoint)
            .primaryAction
            ?: return

    NativeHandoffLayout(
        title = "가족·도우미 확인",
        onHome = onHome,
    ) {
        Text(
            text = "현재 단계에서 안내할 곳은 하나뿐이에요.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("어르신이 직접 누를 곳", style = MaterialTheme.typography.bodyLarge)
                Text(primary.accessibleName, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            text = "도우미는 이 위치만 확인합니다. 대신 누르거나 신청하지 않아요.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onConfirm,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
        ) {
            Text("이곳을 안내해 주세요")
        }
        OutlinedButton(
            onClick = onCancel,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
        ) {
            Text("취소")
        }
    }
}

@Composable
private fun HandBackScreen(
    state: GilnunUiState,
    onReturn: () -> Unit,
    onHome: () -> Unit,
) {
    val serviceId = state.selectedService ?: return
    NativeHandoffLayout(
        title = "어르신께 돌려드려요",
        onHome = onHome,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_gilnun),
            contentDescription = null,
            modifier =
                Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
        )
        Text(
            text = "${serviceId.shortTitle()} 화면으로 돌아갑니다.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "표시된 곳을 확인한 뒤 어르신이 직접 선택해 주세요.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onReturn,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
        ) {
            Text("연습 화면으로 돌아가기")
        }
    }
}

@Composable
private fun NativeHandoffLayout(
    title: String,
    onHome: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
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
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onHome,
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                ) {
                    Text("홈")
                }
            }
            PracticeBanner()
            content()
        }
    }
}

@Composable
private fun PracticeBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = "연습용 화면 · 실제 기관과 연결되지 않아요",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HumanReceipt(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Notice(
    message: String,
    isError: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            if (isError) {
                GilnunError.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HelpChoiceDialog(
    fromFriction: Boolean,
    onAutomatic: () -> Unit,
    onHelper: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(onDismissRequest = onDecline) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "도움이 필요하신가요?",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text =
                        if (fromFriction) {
                            "같은 선택을 여러 번 하셨어요. 원하는 도움을 골라 주세요."
                        } else {
                            "원하는 도움을 골라 주세요. 앱이 대신 누르지는 않아요."
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onAutomatic,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = GilnunYellow,
                            contentColor = GilnunNavy,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text("자동 안내 받기")
                }
                OutlinedButton(
                    onClick = onHelper,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text("가족·도우미에게 넘기기")
                }
                TextButton(
                    onClick = onDecline,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text("지금은 괜찮아요")
                }
            }
        }
    }
}

private fun ServiceId.homeTitle(): String =
    when (this) {
        ServiceId.BASIC_PENSION -> "기초연금 신청 연습"
        ServiceId.RESIDENT_RECORD -> "주민등록표 등본 발급 연습"
        ServiceId.HEALTH_SCREENING -> "건강검진 대상 조회 연습"
    }

private fun ServiceId.shortTitle(): String =
    when (this) {
        ServiceId.BASIC_PENSION -> "기초연금"
        ServiceId.RESIDENT_RECORD -> "주민등록표 등본"
        ServiceId.HEALTH_SCREENING -> "건강검진"
    }

private fun ServiceId.homeDescription(): String =
    when (this) {
        ServiceId.BASIC_PENSION -> "가상 정보로 신청 순서를 익혀요"
        ServiceId.RESIDENT_RECORD -> "법적 효력 없는 모의 등본을 확인해요"
        ServiceId.HEALTH_SCREENING -> "2026년 모의 조회 과정을 익혀요"
    }
