package theweeb.dev.practicingcompose.presentation.read
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import theweeb.dev.practicingcompose.R
import theweeb.dev.practicingcompose.domain.model.Section
import theweeb.dev.practicingcompose.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    state: ReadState,
    currentUser: User,
    isSubSectionPassed: Boolean,
    isPreviousSectionPassed: Boolean,
    sectionsQuizzesResultsMap: Map<String, Boolean>,
    sections: List<Section>,
    progress: Map<String, Float>?,
    onRead: (ReadRoute) -> Unit,
    onTakeQuiz: (String) -> Unit,
    toReadPDF: () -> Unit,
    getPreviousSectionPassed: (String, User) -> Unit,
    setCurrentSection: (Section) -> Unit,
    getSubSections: (String, User) -> Unit
) {

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var isBottomSheetShown by remember{
        mutableStateOf(false)
    }

    var infoDialogText by rememberSaveable {
        mutableStateOf("Please have at least one passing quiz record of the previous section to unlock.")
    }

    var infoDialogShown by remember {
        mutableStateOf(false)
    }

    BackHandler(enabled = isBottomSheetShown) {
        if(isBottomSheetShown){
            isBottomSheetShown = false
        }
    }

    if(isBottomSheetShown){
        ModalBottomSheet(
            onDismissRequest = { isBottomSheetShown = !isBottomSheetShown },
            sheetState = sheetState,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column{
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var displayCount = 0
                        state.subSections.sortedBy { it.createdAt }.forEach {
                            it.chapterIdsAndTitles.values.sorted().forEach { s ->
                                if (displayCount < 6) {
                                    Surface(
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = s.substring(2, s.length),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        )
                                    }
                                    displayCount++
                                }
                            }
                        }
                        if (displayCount >= 6) {
                            Text(
                                text = "and more...",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = modifier.padding(4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            isBottomSheetShown = false
                            onRead(ReadRoute(
                                sectionId = state.currentSection?.sectionId,
                                progress = progress?.get(state.currentSection?.sectionId)
                            )) },
                        enabled = isPreviousSectionPassed,
                        shape = RoundedCornerShape(25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Read", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(painter = painterResource(id = R.drawable.read), contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if(isSubSectionPassed){
                                isBottomSheetShown = false
                                state.currentSection?.sectionId?.let { onTakeQuiz(it) }
                            }else{
                                infoDialogText = "Please complete reading and pass all the tests of this section to unlock."
                                infoDialogShown = true
                            } },
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                            contentColor = if(isSubSectionPassed) ButtonDefaults.buttonColors().containerColor else ButtonDefaults.buttonColors().disabledContentColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 5.dp
                        ),
                        shape = RoundedCornerShape(25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Quiz", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        if(isSubSectionPassed){
                            Icon(painter = painterResource(id = R.drawable.quiz_icon), contentDescription = null)
                        }else{
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if(infoDialogShown){
        InfoDialog(
            text = infoDialogText
        ) {
            infoDialogShown = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Card {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.read_background),
                    contentDescription = null,
                    modifier = Modifier.weight(.5f)
                )
                Column(
                    modifier = Modifier.weight(.5f)
                ) {
                    Text(
                        text = "PDF Version Available",
                        style = MaterialTheme.typography.titleLarge
                    )
                    ElevatedButton(
                        onClick = toReadPDF,
                        shape = RoundedCornerShape(25f),
                        colors = ButtonDefaults.elevatedButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 10.dp
                        )
                    ) {
                        Text(
                            text = "Read Now",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        sections.forEachIndexed { index, section ->
            val currentProgress = progress?.get(section.sectionId) ?: 0f
            val sectionBefore = sections[if(index < 1) 0 else index - 1]
            val previousSectionProgress = progress?.get(sectionBefore.sectionId) ?: 0f
            val isPreviousSectionQuizzesPassed = sectionsQuizzesResultsMap[sectionBefore.sectionId] ?: false
            val isPassed = if(index != 0) previousSectionProgress > 0.99f && isPreviousSectionQuizzesPassed else true

            ReadSectionItem(
                section = section,
                isPassed = isPassed,
                progress = currentProgress,
                onClick = {
                    if(isPassed){
                        getSubSections(section.sectionId, currentUser)
                        getPreviousSectionPassed(sectionBefore.sectionId, currentUser)
                        setCurrentSection(section)
                        isBottomSheetShown = true
                    }else{
                        infoDialogText = "Please have at least one passing quiz record of the previous section to unlock."
                        infoDialogShown = true
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoDialog (
    modifier: Modifier = Modifier,
    text: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(25f)
        ) {
            Box {
                FilledIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                }
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painter = painterResource(id = R.drawable.locked_image),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp)
                    )
                    Text(text = "LOCKED!", fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = text,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ReadSectionItem(
    modifier: Modifier = Modifier,
    isPassed: Boolean,
    progress: Float?,
    section: Section,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.CenterEnd)
        ){
            FilledTonalIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.filledIconButtonColors().copy(
                    containerColor = if(isPassed) IconButtonDefaults.filledIconButtonColors().containerColor else IconButtonDefaults.filledIconButtonColors().disabledContainerColor,
                    contentColor = if(isPassed) IconButtonDefaults.filledIconButtonColors().contentColor else IconButtonDefaults.filledIconButtonColors().disabledContentColor
                ),
                modifier = Modifier
                    .then(Modifier.size(30.dp))
            ) {
                Icon(
                    imageVector = if(isPassed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.Lock,
                    contentDescription = null
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row{
                Text(
                    text = "${section.sectionNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = modifier.height(24.dp))
            if (section.subSectionIdsAndTitles.isNotEmpty()) {
                section.subSectionIdsAndTitles.entries
                    .sortedBy { it.value }
                    .associate { it.toPair() }.forEach { (_, value) ->
                        Text(
                            text = value.substring(2, value.length),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
            } else {
                Text(
                    text = "There is no record.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (progress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                CustomLinearProgressIndicator(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    progress = progress
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun CustomLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiaryContainer,
    progressColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(text = String.format("%.2f%%", progress * 100), style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .height(7.dp)
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(progressColor)
            )
        }
    }
}