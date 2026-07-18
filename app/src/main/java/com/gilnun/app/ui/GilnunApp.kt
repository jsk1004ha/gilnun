package com.gilnun.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.animation.ValueAnimator
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay

@Composable
fun GilnunApp(viewModel: GilnunViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showStartup by rememberSaveable { mutableStateOf(true) }
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }

    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            delay(STARTUP_DURATION_MS)
        }
        showStartup = false
    }

    if (showStartup) {
        StartupScreen()
        return
    }

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
                onChangeLayout = viewModel::togglePracticeLayout,
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
private fun StartupScreen() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val logoAlpha by
        animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(durationMillis = 360),
            label = "startup logo alpha",
        )
    val logoScale by
        animateFloatAsState(
            targetValue = if (entered) 1f else 0.82f,
            animationSpec = tween(durationMillis = 440),
            label = "startup logo scale",
        )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GilnunNavy,
        contentColor = Color.White,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_gilnun),
                contentDescription = "길눈",
                modifier =
                    Modifier
                        .size(168.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "한 번 찾은 길은,\n모두의 길이 됩니다.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha),
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                color = GilnunYellow,
                trackColor = Color.White.copy(alpha = 0.25f),
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
                ValueProposition()
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
private fun ValueProposition() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GilnunNavy,
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "좌표는 빗나가도,\n의미는 다시 찾습니다.",
                style = MaterialTheme.typography.headlineLarge,
                color = GilnunYellow,
            )
            Text(
                text = "길눈은 화면에서 막히는 순간을 알아채고, 동의를 받은 뒤 검증된 한 단계 도움만 다시 보여드려요.",
                style = MaterialTheme.typography.bodyLarge,
            )
            ProofPoint("1", "복잡한 화면에서 막힌 한 단계만 찾아요")
            ProofPoint("2", "버튼 위치가 바뀌어도 이름·역할·다음 화면을 확인해요")
            ProofPoint("3", "안내 표시 → 직접 선택 → 다음 화면 확인")
            Text(
                text = "대상이 없거나 겹치면 추측하지 않고 멈춰요.",
                style = MaterialTheme.typography.bodyLarge,
                color = GilnunYellow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProofPoint(
    number: String,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            color = GilnunTeal,
            contentColor = Color.White,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
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
                text = "한 번 찾은 길은, 모두의 길이 됩니다.",
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
                    text = serviceId.portalLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = GilnunTeal,
                )
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
    onChangeLayout: () -> Unit,
    onEvent: (com.gilnun.app.web.BridgeEventV2) -> Unit,
    onBridgeStatus: (com.gilnun.app.web.BridgeStatus) -> Unit,
    onSecurityEvent: (String) -> Unit,
) {
    val serviceId = state.selectedService ?: return
    val checkpoint = state.checkpoint ?: return
    var pageReady by remember(serviceId, state.layout) { mutableStateOf(false) }
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
                stepIndex = stepIndex,
                layout = state.layout,
                onHome = onHome,
                onChangeLayout = onChangeLayout,
            )
            if (state.receiptMessage != null) {
                HumanReceipt(state.receiptMessage)
            }
            if (state.notice != null) {
                Notice(
                    message = state.notice,
                    isError = state.speechUnavailable,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp)),
            ) {
                DemoWebView(
                    serviceId = serviceId,
                    layout = state.layout,
                    modifier = Modifier.fillMaxSize(),
                    command = state.webCommand,
                    onEvent = onEvent,
                    onBridgeStatus = { status ->
                        if (status == com.gilnun.app.web.BridgeStatus.PageReady ||
                            status is com.gilnun.app.web.BridgeStatus.PageFailed
                        ) {
                            pageReady = true
                        }
                        onBridgeStatus(status)
                    },
                    onSecurityEvent = onSecurityEvent,
                )
                if (!pageReady) {
                    PracticeLoadingOverlay(serviceId)
                }
            }
        }
    }
}

@Composable
private fun PracticeLoadingOverlay(serviceId: ServiceId) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = GilnunNavy,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_gilnun),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            Text(
                text = "${serviceId.shortTitle()} 연습 화면 준비 중",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = "연습용 화면 · 실제 기관과 연결되지 않아요",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                color = GilnunTeal,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}

@Composable
private fun PracticeTopBar(
    stepIndex: Int,
    layout: com.gilnun.app.web.PracticeLayout,
    onHome: () -> Unit,
    onChangeLayout: () -> Unit,
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
            text = "${stepIndex.coerceIn(1, 3)} / 3 단계",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (stepIndex == 1) {
            OutlinedButton(
                onClick = onChangeLayout,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
            ) {
                Text(
                    if (layout == com.gilnun.app.web.PracticeLayout.A) {
                        "화면 배치 바꿔보기"
                    } else {
                        "원래 배치로 돌아가기"
                    },
                )
            }
            Text(
                text = "위치가 달라져도 도움 버튼을 누르면 같은 의미의 선택을 다시 찾아요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
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

private fun ServiceId.portalLabel(): String =
    when (this) {
        ServiceId.BASIC_PENSION -> "복지로형 복잡 화면"
        ServiceId.RESIDENT_RECORD -> "정부24형 복잡 화면"
        ServiceId.HEALTH_SCREENING -> "건강보험형 복잡 화면"
    }

private const val STARTUP_DURATION_MS = 720L
