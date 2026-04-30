package toro.sources.pages

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import toro.sources.components.ChatRequestsList
import toro.sources.components.ActiveChatsList
import toro.sources.R
import androidx.compose.ui.res.stringResource
import androidx.navigation.ActivityNavigator
import androidx.navigation.compose.ComposeNavigator
import toro.sources.AppViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInboxPage(
    viewModel: AppViewModel,
    onChatClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pendingRequestsCount by viewModel.pendingRequestsCount.collectAsState()

    val requestsTitle = if (pendingRequestsCount > 0) {
        "${stringResource(R.string.requests)} ($pendingRequestsCount)"
    } else {
        stringResource(R.string.requests)
    }

    val tabs = listOf(stringResource(R.string.messages), requestsTitle)

    LaunchedEffect(Unit) {
        viewModel.getInbox()
        viewModel.getChatRequests()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inbox)) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // The Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // The Content
            if (selectedTabIndex == 0) {
                ActiveChatsList(
                    viewModel,
                    onChatClick
                )
            } else {
                ChatRequestsList(viewModel)
            }
        }
    }
}