package theweeb.dev.practicingcompose.presentation.read

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.yml.charts.common.extensions.isNotNull
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageData
import com.mohamedrejeb.richeditor.model.ImageLoader
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import theweeb.dev.practicingcompose.R
import theweeb.dev.practicingcompose.domain.model.Chapter
import theweeb.dev.practicingcompose.domain.model.Question
import theweeb.dev.practicingcompose.domain.model.SubSection
import kotlin.math.sin

@Composable
fun ReadingScreenRoute(
    modifier: Modifier = Modifier,
    progress: Float,
    viewModel: ReadingViewModel = hiltViewModel(),
    sectionId: String
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val subSectionQuizzesProgress by viewModel.subSectionQuizzesProgress.collectAsStateWithLifecycle()

    LaunchedEffect(sectionId){
        viewModel.getSubSections(sectionId)
        viewModel.getQuestions(sectionId)
    }

    AnimatedContent(
        targetState = !state.isContentLoading,
        transitionSpec = {
            slideInVertically(
                animationSpec = tween(
                    durationMillis = 500,
                    easing = Ease
                )
            ){ it } togetherWith slideOutVertically(
                animationSpec = tween(
                    durationMillis = 500,
                    easing = Ease
                )
            ){ it }
        },
        label = ""
    ) { isNotLoading ->
        when(isNotLoading){
            true -> {
                ReadingScreen(
                    sectionId = sectionId,
                    progress = progress,
                    isQuizDialogOpen = state.isQuizDialogOpen,
                    questions = questions,
                    currentSubSection = state.currentSubSection,
                    subSectionQuizzesProgress = subSectionQuizzesProgress,
                    subSections = state.subSectionsWithChapters.subSections.sortedBy { it.createdAt },
                    chapters = state.subSectionsWithChapters.chapters.sortedBy { it.createdAt }.map { it.copy(chapterTitle = it.chapterTitle?.substring(1, it.chapterTitle.length)) },
                    updateProgress = viewModel::updateSectionProgress,
                    toggleQuizDialog = viewModel::toggleQuizDialog,
                    setSubSection = viewModel::setSubSection,
                    updateQuizResults = viewModel::updateSubSectionQuizzesProgress
                )
            }
            false -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedPreloader()
                    WavingText(text = "Please wait for a while...")
                }
            }
        }
    }
}

@Composable
fun WavingText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    val yOffset = sin(waveOffset.toDouble()).toFloat() * 10.dp.value

    Text(
        text = text,
        modifier = Modifier.offset(y = yOffset.dp),
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.labelMedium
    )
}

@OptIn(FlowPreview::class, ExperimentalRichTextApi::class)
@Composable
fun ReadingScreen(
    modifier: Modifier = Modifier,
    currentSubSection: SubSection?,
    isQuizDialogOpen: Boolean,
    questions: List<Question>,
    progress: Float,
    subSectionQuizzesProgress: Map<String, Map<String, Boolean>>,
    sectionId: String,
    subSections: List<SubSection>,
    chapters: List<Chapter>,
    setSubSection: (SubSection?) -> Unit,
    toggleQuizDialog: () -> Unit,
    updateProgress: (String, Float) -> Unit,
    updateQuizResults: (String, Map<String, Boolean>) -> Unit,
) {

    val scrollState = rememberScrollState()
    val richTextState = rememberRichTextState()

    val filteredQuestions = subSections.filterSubSectionsContainingQuestions(questions)
    val groupedChapters = chapters.groupChapters(subSections = subSections)
    val passedChapters = groupedChapters.returnPassedChapters(
        quizzesRecords = subSectionQuizzesProgress,
        existingSubSectionIds = filteredQuestions.keys.toList(),
        sectionId = sectionId
    )
    val lastSubSectionQuestions = questions.filter { it.subSectionId == subSections.last().subSectionId }
    val areAllQuestionsPassed = subSectionQuizzesProgress[sectionId]?.all { it.value }
    val result = if(subSectionQuizzesProgress[sectionId]?.any { it.isNotNull() } == true) areAllQuestionsPassed else false
    val isMaxValue by remember {
        derivedStateOf {
            scrollState.value == scrollState.maxValue
        }
    }

    var initialProgress by remember {
        mutableFloatStateOf(0f)
    }

    val currentProgress = remember { mutableFloatStateOf(0f) }
    val debounceDelay = 500L

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value / scrollState.maxValue.toFloat() }
            .collect { progress ->
                currentProgress.floatValue = progress
                Log.d("CurrentProgress", "Smooth Progress: $progress")
            }
    }

    LaunchedEffect(currentProgress) {
        snapshotFlow { currentProgress.floatValue }
            .debounce(debounceDelay)
            .collectLatest { debouncedProgress ->
                updateProgress(sectionId, debouncedProgress)
                Log.d("DebouncedProgress", "Updated Progress: $debouncedProgress")
            }
    }

    LaunchedEffect(scrollState.maxValue) {
        scrollState.animateScrollBy(progress * scrollState.maxValue - scrollState.value)
        initialProgress = scrollState.value / scrollState.maxValue.toFloat()
        Log.d("initial", "Initial: $initialProgress")
    }

    LaunchedEffect(subSectionQuizzesProgress) {
        val modifiedChapters = addSubSectionTitlesToChapters(
            passedChapters, subSections)
        val combinedContent =  modifiedChapters.joinToString(separator = "") { it.content }
        richTextState.setHtml(combinedContent)
    }

    if(isQuizDialogOpen){
        filteredQuestions[currentSubSection?.subSectionId]?.let { questionsList ->
            QuizDialog(
                questions = questionsList,
                isNextQuizAvailable = chapters.size != passedChapters.size,
                onSubmit = {
                    updateQuizResults(
                        sectionId,
                        mapOf(currentSubSection!!.subSectionId to it)
                    )
                },
                onDismiss = toggleQuizDialog
            )
        }
    }

    if (chapters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "There is no content.")
        }
    }

    Column{
        LinearProgressIndicator(
            progress = { currentProgress.floatValue },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(
                        scrollState
                    )
            ) {
                RichText(
                    state = richTextState,
                    style = MaterialTheme.typography.bodySmall
                )
                if(chapters.size != passedChapters.size || lastSubSectionQuestions.isNotEmpty() && result == false){
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(.25f))
                        Text(
                            text = "To be continued",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(.5f)
                                .padding(horizontal = 4.dp)
                        )
                        HorizontalDivider(Modifier.weight(.25f))
                    }
                }
            }
            if(chapters.size != passedChapters.size || lastSubSectionQuestions.isNotEmpty() && result == false){
                androidx.compose.animation.AnimatedVisibility(
                    visible = isMaxValue,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    QuizCheckPoint(
                        modifier = Modifier.fillMaxWidth(),
                        chapter = chapters.getOrNull(passedChapters.lastIndex + 1),
                        onTakeQuiz = {
                            setSubSection(subSections.find { it.subSectionId == passedChapters.last().subSectionId })
                            toggleQuizDialog()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizDialog(
    modifier: Modifier = Modifier,
    questions: List<Question>,
    isNextQuizAvailable: Boolean,
    onSubmit: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var remainingTime by remember { mutableIntStateOf(20) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    val userAnswers = remember { mutableStateMapOf<String, String>() }

    // Timer logic
    LaunchedEffect(currentQuestionIndex) {
        remainingTime = 15
        while (remainingTime > 0) {
            delay(1000L)
            remainingTime--
        }
        if (currentQuestionIndex < questions.lastIndex) {
            currentQuestionIndex++
        } else {
            isQuizCompleted = true
            onSubmit(checkIfPassed(questions, userAnswers))
        }
    }

    Dialog(
        onDismissRequest = { /* Optional dismiss handler */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Box {
                if(isQuizCompleted){
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isQuizCompleted) {
                        val question = questions[currentQuestionIndex]
                        Text(text = question.content, style = MaterialTheme.typography.titleMedium)
                        question.options?.forEach { (_, optionValue) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = userAnswers[question.questionId] == optionValue,
                                    onClick = {
                                        userAnswers[question.questionId] = optionValue
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = optionValue, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Text(text = "Time remaining: $remainingTime seconds", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(25f),
                            onClick = {
                                if (currentQuestionIndex < questions.lastIndex) {
                                    currentQuestionIndex++
                                } else {
                                    isQuizCompleted = true
                                    onSubmit(checkIfPassed(questions, userAnswers))
                                }
                            }) {
                            Text(
                                text = if (currentQuestionIndex < questions.lastIndex) "Next" else "Finish",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }else{
                        val isPassed = checkIfPassed(questions, userAnswers)
                        Image(
                            painter = painterResource(id = if(isPassed){ if(!isNextQuizAvailable) R.drawable.passed_all else R.drawable.passed_read  } else R.drawable.failed_read),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp)
                        )
                        Text(
                            text = if(isPassed){ if(!isNextQuizAvailable) "FINISHED!" else "PASSED!" } else "FAILED!",
                            fontSize = 34.sp
                        )
                        Text(
                            text = if(isPassed){ if(!isNextQuizAvailable) "Quiz can now be taken in the progress tab." else "Next chapter is now available to read." } else "Please review your answer very carefully.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

fun checkIfPassed(questions: List<Question>, userAnswers: Map<String, String>): Boolean {
    return questions.all { question ->
        userAnswers[question.questionId] == question.answer // Compare the answer text
    } && userAnswers.size == questions.size // Ensure all questions are answered
}

@Composable
fun QuizCheckPoint(
    modifier: Modifier = Modifier,
    chapter: Chapter?,
    onTakeQuiz: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.padding(8.dp)
    ){
        Column{
            if(chapter != null){
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ){
                    Text(text = "Next Chapter: ")
                    Text(
                        text = "${chapter.chapterTitle}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
                HorizontalDivider()
            }
            Row(
                modifier = modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Please answer several questions to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onTakeQuiz,
                    shape = RoundedCornerShape(25f),
                ) {
                    Text(text = "Start")
                }
            }
        }
    }
}

fun List<Chapter>.groupChapters(
    subSections: List<SubSection>,
): Map<String?, List<Chapter>> {

    val chapters = groupBy { chapter ->
        subSections.find { it.subSectionId == chapter.subSectionId }?.subSectionId
    }

    val chapterTitles = chapters.values.flatten().map { it.chapterTitle }
    Log.d("sortedChapters", "$chapterTitles")

    return chapters
}

fun Map<String?, List<Chapter>>.returnPassedChapters(
    existingSubSectionIds: List<String>,
    quizzesRecords: Map<String, Map<String, Boolean>>,
    sectionId: String
): List<Chapter> {
    val chaptersToAdd = mutableListOf<Chapter>()

    // Step 1: Process the quiz records
    this.keys.forEach { subSectionId ->
        val chapters = this[subSectionId] ?: emptyList()
        val isQuizInSubSectionIds = existingSubSectionIds.contains(subSectionId)
        val isPassed = quizzesRecords[sectionId]?.get(subSectionId)

        if (isPassed == null) {
            // No record of quiz - add chapters, return if quiz exists in `existingSubSectionIds`
            chaptersToAdd.addAll(chapters)
            if (isQuizInSubSectionIds) return chaptersToAdd
        } else if (isPassed) {
            // Quiz passed - add chapters
            chaptersToAdd.addAll(chapters)
        } else {
            // Quiz failed - add chapters and return early
            chaptersToAdd.addAll(chapters)
            return chaptersToAdd

        }
    }

    return chaptersToAdd
}

fun List<SubSection>.filterSubSectionsContainingQuestions(questions: List<Question>): Map<String, List<Question>> {
    val filteredQuestions = questions.filter { it.subSectionId.isNotBlank() }
    val questionsGroupedBySubSectionId = filteredQuestions.groupBy { it.subSectionId }
    Log.d("SubSectionsFiltered", "${map { Pair(it.header, it.subSectionId) }}")
    return questionsGroupedBySubSectionId.filterKeys { subSectionId ->
        this.any { it.subSectionId == subSectionId }
    }
}

fun addSubSectionTitlesToChapters(
    chapters: List<Chapter>,
    subSections: List<SubSection>
): List<Chapter> {
    val updatedChapters = mutableListOf<Chapter>()
    var previousSubSectionId: String? = null

    for (chapter in chapters) {
        val subSection = subSections.find { it.subSectionId == chapter.subSectionId }
        val subSectionTitle = subSection?.header?.substring(1, subSection.header.length)

        if (subSectionTitle != null && subSection.subSectionId != previousSubSectionId) {
            // Concatenate the title to the chapter's content
            val updatedContent = "<br><br><h2>$subSectionTitle</h2><br><br>${chapter.content}"
            updatedChapters.add(chapter.copy(content = updatedContent))
            previousSubSectionId = subSection.subSectionId
        } else {
            updatedChapters.add(chapter.copy(content = "<br>${chapter.content}"))
        }
    }

    return updatedChapters
}

@Composable
fun AnimatedPreloader(modifier: Modifier = Modifier) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.animatedbook
        )
    )
    val preloaderProgress by animateLottieCompositionAsState(
        preloaderLottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )
    LottieAnimation(
        composition = preloaderLottieComposition,
        progress = preloaderProgress,
        modifier = modifier.size(70.dp),
        contentScale = ContentScale.Crop
    )
}