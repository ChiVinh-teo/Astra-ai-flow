package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.LearningMode
import com.example.model.MainTab
import com.example.service.VoiceEngine
import com.example.ui.modals.*
import com.example.ui.modes.*
import com.example.ui.screens.*
import com.example.ui.theme.AstraFlowTheme
import com.example.ui.theme.LocalAstraColors
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.TutorViewModel

class MainActivity : ComponentActivity() {
    private lateinit var voiceEngine: VoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        voiceEngine = VoiceEngine(this)

        setContent {
            val mainVm: MainViewModel = viewModel()
            val tutorVm: TutorViewModel = viewModel()

            val settings by mainVm.settings.collectAsStateWithLifecycle()
            val courses by mainVm.courses.collectAsStateWithLifecycle()
            val currentCourse by mainVm.currentCourse.collectAsStateWithLifecycle()
            val remediations by mainVm.remediations.collectAsStateWithLifecycle()
            val dailyGoals by mainVm.dailyGoals.collectAsStateWithLifecycle()
            val calendarDays by mainVm.studyCalendar.collectAsStateWithLifecycle()
            val duelHistory by mainVm.duelHistory.collectAsStateWithLifecycle()
            val activeTab by mainVm.activeTab.collectAsStateWithLifecycle()
            val activeSession by mainVm.activeSession.collectAsStateWithLifecycle()
            val toastMessage by mainVm.toastMessage.collectAsStateWithLifecycle()
            val ingestionState by mainVm.ingestionState.collectAsStateWithLifecycle()
            val modelDiscovery by mainVm.modelDiscovery.collectAsStateWithLifecycle()

            val showAiSettings by mainVm.showAiSettings.collectAsStateWithLifecycle()
            val showIngestModal by mainVm.showIngestModal.collectAsStateWithLifecycle()
            val showShareModal by mainVm.showShareModal.collectAsStateWithLifecycle()
            val showDuelModal by mainVm.showDuelModal.collectAsStateWithLifecycle()
            val showModeSheet by mainVm.showModeSheet.collectAsStateWithLifecycle()

            LaunchedEffect(currentCourse.id) {
                tutorVm.attachRepository(mainVm.repository, currentCourse)
            }

            // Sync edge-to-edge system bar icon polarity with Cosmic Dark vs White-Paper Exam theme
            LaunchedEffect(settings.isWhitePaperExam) {
                enableEdgeToEdge(
                    statusBarStyle = if (settings.isWhitePaperExam) {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (settings.isWhitePaperExam) {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    }
                )
            }

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(toastMessage) {
                val msg = toastMessage ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
                mainVm.clearToast()
            }

            AstraFlowTheme(isWhitePaperExam = settings.isWhitePaperExam) {
                val astraColors = LocalAstraColors.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(astraColors.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Adaptive shell: full-bleed on phones, clean centered max-600dp column on tablets
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    ) {
                        val session = activeSession
                        if (session != null) {
                            when (session.mode) {
                                LearningMode.SMART_TEXT -> {
                                    val node = currentCourse.nodes.find { it.id == session.nodeId }
                                        ?: currentCourse.nodes.firstOrNull { it.smartSteps.isNotEmpty() }
                                        ?: currentCourse.nodes.first()
                                    SmartTextSessionScreen(
                                        node = node,
                                        settings = settings,
                                        geminiUseCases = mainVm.geminiUseCases,
                                        onLogMistake = mainVm::logMistake,
                                        onCompleteNode = mainVm::completeSmartNode,
                                        onShowToast = mainVm::showToast,
                                        onBack = mainVm::closeSession
                                    )
                                }
                                LearningMode.PODCAST -> PodcastModeScreen(
                                    course = currentCourse,
                                    ttsSpeed = settings.ttsSpeed,
                                    voiceEngine = voiceEngine,
                                    onSpeedChange = mainVm::setTtsSpeed,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.VIDEO_SIMULATOR -> VideoSimulatorScreen(
                                    course = currentCourse,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.FLASHCARDS -> FlashcardsModeScreen(
                                    course = currentCourse,
                                    onRateCard = mainVm::rateFlashcard,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.KNOWLEDGE_GAPS -> KnowledgeGapsScreen(
                                    course = currentCourse,
                                    remediations = remediations,
                                    settings = settings,
                                    geminiUseCases = mainVm.geminiUseCases,
                                    onResolveGap = mainVm::resolveGap,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.EXERCISES -> ExercisesModeScreen(
                                    course = currentCourse,
                                    settings = settings,
                                    geminiUseCases = mainVm.geminiUseCases,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.ADAPTIVE_QUIZ -> AdaptiveQuizScreen(
                                    course = currentCourse,
                                    onLogMistake = mainVm::logMistake,
                                    onCompleteQuiz = mainVm::recordQuizCompletion,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.TRUE_FALSE_BLITZ -> TrueFalseBlitzScreen(
                                    course = currentCourse,
                                    onLogMistake = mainVm::logMistake,
                                    onComplete = mainVm::recordQuizCompletion,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.SPACED_REPETITION -> SpacedRepetitionScreen(
                                    course = currentCourse,
                                    remediations = remediations,
                                    onRateCard = mainVm::rateFlashcard,
                                    onResolveGap = mainVm::resolveGap,
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.WRITTEN_EXAM -> WrittenMockExamScreen(
                                    course = currentCourse,
                                    settings = settings,
                                    geminiUseCases = mainVm.geminiUseCases,
                                    onToggleWhitePaper = mainVm::toggleWhitePaperExam,
                                    onOpenAiSettings = { mainVm.openAiSettings(true) },
                                    onBack = mainVm::closeSession
                                )
                                LearningMode.ORAL_VIVA -> OralVivaScreen(
                                    course = currentCourse,
                                    settings = settings,
                                    geminiUseCases = mainVm.geminiUseCases,
                                    voiceEngine = voiceEngine,
                                    onBack = mainVm::closeSession
                                )
                            }
                        } else {
                            // 4-Tab Native Material 3 Shell
                            Scaffold(
                                containerColor = astraColors.background,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = astraColors.card,
                                        windowInsets = WindowInsets.navigationBars
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            val selected = activeTab == tab
                                            val icon = when (tab) {
                                                MainTab.ROADMAP -> if (selected) Icons.Rounded.Map else Icons.Outlined.Map
                                                MainTab.TUTOR -> if (selected) Icons.Rounded.Psychology else Icons.Outlined.Psychology
                                                MainTab.PRACTICE -> if (selected) Icons.Rounded.Science else Icons.Outlined.Science
                                                MainTab.LIBRARY -> if (selected) Icons.Rounded.FolderSpecial else Icons.Outlined.FolderSpecial
                                            }
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { mainVm.selectTab(tab) },
                                                icon = { Icon(icon, contentDescription = tab.title) },
                                                label = { Text(tab.title, maxLines = 1) },
                                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (activeTab) {
                                        MainTab.ROADMAP -> RoadmapScreen(
                                            courses = courses,
                                            currentCourse = currentCourse,
                                            settings = settings,
                                            dailyGoals = dailyGoals,
                                            calendarDays = calendarDays,
                                            onSelectCourse = mainVm::selectCourse,
                                            onToggleTheme = mainVm::toggleWhitePaperExam,
                                            onOpenSettings = { mainVm.openAiSettings(true) },
                                            onOpenIngest = { mainVm.openIngestModal(true) },
                                            onOpenDuel = { mainVm.openDuelModal(true) },
                                            onOpenShare = { mainVm.openShareModal(true) },
                                            onOpenModeSheet = { mainVm.openModeSheet(true) },
                                            onUpdateExamPlan = mainVm::updateExamPlan,
                                            onOpenNode = { node -> mainVm.openMode(node.primaryMode, node.id) }
                                        )
                                        MainTab.TUTOR -> TutorScreen(
                                            tutorViewModel = tutorVm,
                                            courses = courses,
                                            settings = settings,
                                            voiceEngine = voiceEngine,
                                            onSaveFlashcard = mainVm::addFlashcardToCurrentCourse,
                                            onOpenAiSettings = { mainVm.openAiSettings(true) }
                                        )
                                        MainTab.PRACTICE -> PracticeLabScreen(
                                            currentCourse = currentCourse,
                                            duelHistory = duelHistory,
                                            onOpenMode = { mode -> mainVm.openMode(mode) },
                                            onOpenDuelModal = { mainVm.openDuelModal(true) }
                                        )
                                        MainTab.LIBRARY -> LibraryScreen(
                                            courses = courses,
                                            currentCourse = currentCourse,
                                            remediations = remediations,
                                            settings = settings,
                                            onSelectCourse = mainVm::selectCourse,
                                            onOpenIngest = { mainVm.openIngestModal(true) },
                                            onOpenShare = { mainVm.openShareModal(true) },
                                            onOpenSettings = { mainVm.openAiSettings(true) },
                                            onOpenKnowledgeGaps = { mainVm.openMode(LearningMode.KNOWLEDGE_GAPS) }
                                        )
                                    }
                                }
                            }
                        }

                        // Toast host when inside a full-screen Learning Mode
                        if (session != null) {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                            )
                        }
                    }
                }

                // Native Spring Bottom Sheet for 11 Modes
                if (showModeSheet) {
                    ElevenModesBottomSheet(
                        onSelectMode = { mode -> mainVm.openMode(mode) },
                        onDismiss = { mainVm.openModeSheet(false) }
                    )
                }

                // AI Settings Modal
                if (showAiSettings) {
                    AiSettingsDialog(
                        settings = settings,
                        discoveryState = modelDiscovery,
                        onTestAndDiscover = mainVm::testConnectionAndDiscoverModels,
                        onSave = mainVm::saveAiSettings,
                        onDismiss = { mainVm.openAiSettings(false) }
                    )
                }

                // Document Ingestion Studio Modal
                if (showIngestModal) {
                    DocumentIngestionDialog(
                        ingestionState = ingestionState,
                        onStartIngest = mainVm::ingestNewCourse,
                        onOpenAiSettings = {
                            mainVm.openIngestModal(false)
                            mainVm.openAiSettings(true)
                        },
                        onDismiss = { mainVm.openIngestModal(false) }
                    )
                }

                // JSON Course Share Modal
                if (showShareModal) {
                    ShareCourseDialog(
                        courseTitle = currentCourse.title,
                        exportString = mainVm.exportCurrentCourseShareString(),
                        onImport = mainVm::importCourseFromShareString,
                        onShowToast = mainVm::showToast,
                        onDismiss = { mainVm.openShareModal(false) }
                    )
                }

                // Friend Duel Modal
                if (showDuelModal) {
                    FriendDuelDialog(
                        courseTitle = currentCourse.title,
                        duelQuestions = mainVm.buildDuelQuestionsForCurrentCourse(),
                        onEncodeDuel = mainVm::createEncodedDuelCode,
                        onDecodeDuel = mainVm::decodeDuelCode,
                        onSaveOutcome = mainVm::saveDuelOutcome,
                        onShowToast = mainVm::showToast,
                        onDismiss = { mainVm.openDuelModal(false) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::voiceEngine.isInitialized) {
            voiceEngine.shutdown()
        }
    }
}
