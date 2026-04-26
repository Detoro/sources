package toro.sources

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import toro.sources.pages.AccountPage
import toro.sources.pages.EngagementPage
import toro.sources.pages.HomePage
import toro.sources.pages.LoginPage
import toro.sources.pages.OverviewPage
import toro.sources.pages.ReaderScreen
import toro.sources.pages.SearchPage
import toro.sources.pages.SignUpPage
import toro.sources.pages.UploadPage
import toro.sources.ui.theme.SourcesTheme
import toro.sources.pages.ChatInboxPage
import toro.sources.pages.ChatThreadPage

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Account : Screen("account")
    object Home : Screen("home/{userId}")
    object Upload : Screen("upload")
    object Inbox : Screen("inbox")
    object Search : Screen("search")
    object Reader : Screen("reader/{chapterId}") {
        fun createRoute(chapterId: String) = "reader/$chapterId"
    }
    object Overview : Screen("overview")
    object Engagement : Screen("engagement")
    object Chat : Screen("chat_page/{userId}") {
        fun createRoute(userId: String) = "chat_page/$userId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = application as SourcesCanvas
        val appRepository = appContainer.repository

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                    return AppViewModel(appRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        setContent {
            SourcesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AppViewModel = viewModel(factory = factory)
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // routes where the Bottom Navigation should be visible
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Upload.route,
        Screen.Inbox.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    // TAB 1: LIBRARY (Home)
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library") },
                        label = { Text("Library") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            // The dummy user ID you established earlier
                            navController.navigate("home/user123") {
                                // Pop up to the start destination to avoid building a massive backstack
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chat") },
                        label = { Text("Chat") },
                        selected = currentRoute == Screen.Inbox.route,
                        onClick = {
                            navController.navigate(Screen.Inbox.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // TAB 2: SEARCH
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        selected = currentRoute == Screen.Search.route,
                        onClick = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // TAB 3: UPLOAD
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Upload") },
                        label = { Text("Upload") },
                        selected = currentRoute == Screen.Upload.route,
                        onClick = {
                            navController.navigate(Screen.Upload.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                        label = { Text("Account") },
                        selected = currentRoute == Screen.Account.route,
                        onClick = {
                            navController.navigate(Screen.Account.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginPage(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onLoginSubmit = { credentials ->
                        viewModel.loginUser(credentials, onSuccess = {
                            navController.navigate("home/user123") {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    },
                    loginError = null
                )
            }
            composable(Screen.SignUp.route) {
                SignUpPage (
                    onNavigateBack = { navController.navigate((Screen.Login.route)) },
                    onSignUpSuccess = {newUser ->
                        viewModel.registerNewUser(newUser, onSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    }
                )
            }
            composable(Screen.Home.route) {
                HomePage(
                    viewModel = viewModel,
                    onComicClick = { comicId ->
                        viewModel.selectComic(comicId)
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }
            composable(Screen.Reader.route) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId")
                LaunchedEffect(chapterId) {
                    if (chapterId != null) {
                        viewModel.openChapter(chapterId)
                    }
                }
                val readerState by viewModel.readerState.collectAsState()
                when (val state = readerState) {
                    is ReaderUiState.Idle -> {}
                    is ReaderUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ReaderUiState.Success -> {
                        ReaderScreen(
                            pages = state.pages,
                            startingIndex = state.startingPageIndex,
                            onPageChanged = { newPageIndex ->
                                if (chapterId != null) {
                                    viewModel.onPageTurned(chapterId, newPageIndex)
                                }
                            }
                        )
                    }
                    is ReaderUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: ${state.message}")
                        }
                    }
                }
            }
            composable(Screen.Engagement.route) { EngagementPage() }
            composable(Screen.Overview.route) {
                OverviewPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onChapterClick = { chapter ->
                        navController.navigate(Screen.Reader.createRoute(chapter.id))
                    }
                )
            }
            composable(Screen.Upload.route) {
                UploadPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onUploadComplete = {
                        navController.navigate("home/user123") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchPage(
                    viewModel = viewModel,
                    onComicClick = { comicId ->
                        // When a user taps a search result, select it and go to the details page
                        viewModel.selectComic(comicId)
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }
            composable(Screen.Account.route) {
                AccountPage(
                    viewModel = viewModel,
                    onLogoutClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Inbox.route) {
                ChatInboxPage(
                    onChatClick = { userId ->
                        navController.navigate(Screen.Chat.createRoute(userId))
                    }
                )
            }

            composable(Screen.Chat.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

                ChatThreadPage(
                    targetUserId = userId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}