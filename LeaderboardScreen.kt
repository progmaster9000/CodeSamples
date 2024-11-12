package theweeb.dev.practicingcompose.presentation.leaderboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import theweeb.dev.practicingcompose.R
import theweeb.dev.practicingcompose.common.AppDrawerState
import theweeb.dev.practicingcompose.domain.model.Department
import theweeb.dev.practicingcompose.domain.model.User
import theweeb.dev.practicingcompose.presentation.leaderboard.components.FilterButtons
import theweeb.dev.practicingcompose.presentation.leaderboard.components.LeaderboardItem
import theweeb.dev.practicingcompose.presentation.leaderboard.components.TopThreeRow
import theweeb.dev.practicingcompose.presentation.leaderboard.components.fadingEdge

@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    isFetching: Boolean,
    users: List<User>,
    userId: String?,
    onFilterSelected: (Department?) -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilterButtons(
            onFilterSelected = onFilterSelected
        )
        AnimatedContent(targetState = isFetching, label = "") {
            when(it){
                true -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                false -> {
                    if(users.isEmpty()){
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(painter = painterResource(id = R.drawable.error_illustration), contentDescription = null)
                            Text(text = "No user found.")
                        }
                    }else{
                        LeaderboardSection(
                            users = users,
                            userId = userId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardSection(
    users: List<User>,
    userId: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopThreeRow(
                users = users.take(3).reorder()
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Column{
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Name", fontWeight = FontWeight.Bold)
                    Text(text = "Rank", fontWeight = FontWeight.Bold)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(items = users, key = { _, user -> user.uid}) { index, user ->
                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(
                                    durationMillis = 200,
                                    easing = EaseIn
                                )
                            )
                        ){
                            user.department?.let {
                                LeaderboardItem(
                                    modifier = Modifier.then(
                                        if(userId == user.uid)
                                            Modifier.background(color = Color.Gray.copy(alpha = .40f))
                                        else
                                            Modifier
                                    ),
                                    username = user.username ?: "Unknown user",
                                    photoUrl = user.photoUrl,
                                    department = it,
                                    score = user.score,
                                    rank = index + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.reorder(): List<T> {
    val newOrder = listOf(1, 0, 2)
    return if (this.size < 3) {
        newOrder.filter { it < this.size }.map { index -> this[index] }
    } else {
        newOrder.map { index -> this[index] }
    }
}