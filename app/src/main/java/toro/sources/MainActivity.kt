package toro.sources

import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import toro.sources.dataModels.TokenManager
import toro.sources.network.RetrofitClient
import toro.sources.pages.AccountPage
import toro.sources.pages.EngagementPage
import toro.sources.pages.HomePage
import toro.sources.pages.LoginPage
import toro.sources.pages.OverviewPage
import toro.sources.pages.ReaderScreen
import toro.sources.pages.SearchPage
import toro.sources.pages.SignUpPage
import toro.sources.pages.WelcomeScreen
import toro.sources.pages.UploadPage
import toro.sources.ui.theme.SourcesTheme
import toro.sources.pages.AuthorSearchPage
import toro.sources.pages.ChatInboxPage
import toro.sources.pages.ChatThreadPage
import toro.sources.pages.CommentThreadPage
import toro.sources.pages.CommentsPage
import toro.sources.pages.FriendRequestPage
import toro.sources.pages.NotificationsPage
import toro.sources.pages.PostPage
import toro.sources.pages.ReadingList

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Welcome : Screen("welcome")
    object Account : Screen("account")
    object Home : Screen("home/{userId}")
    object Upload : Screen("upload")
    object Inbox : Screen("inbox")
    object Search : Screen("search")
    object Reader : Screen("reader/{chapterId}") {
        fun createRoute(chapterId: String) = "reader/$chapterId"
    }
    object Overview : Screen("overview")
    object Post : Screen("post")
    object Engagement : Screen("engagement")
    object FriendRequest : Screen("friend_request")
    object ReadingList : Screen("reading_list")
    object Notifications : Screen("notifications")
    object Chat : Screen("chat_page/{userId}") {
        fun createRoute(userId: String) = "chat_page/$userId"
    }
    object AuthorSearch : Screen("author_search")
    object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object CommentThread : Screen("comment_thread/{postId}/{commentId}") {
        fun createRoute(postId: String, commentId: String) = "comment_thread/$postId/$commentId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onUserLeaveHint() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("requestPermission", "Permission granted")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, viewModel: AppViewModel) {
        intent?.let {
            val type = it.getStringExtra("type")
            val conversationId = it.getStringExtra("conversationId")
            val postId = it.getStringExtra("postId")

            when (type) {
                "CHAT" -> {
                    if (conversationId != null) {
                        viewModel.handleNavigation(Screen.Chat.createRoute(conversationId))
                    }
                }
                "LIKE", "COMMENT", "FOLLOW" -> {
                    if (postId != null) {
                        viewModel.handleNavigation(Screen.Comments.createRoute(postId))
                    } else {
                        viewModel.handleNavigation(Screen.Notifications.route)
                    }
                }
            }
            it.removeExtra("type")
            it.removeExtra("conversationId")
            it.removeExtra("postId")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        val tokenManager = TokenManager(this)
        RetrofitClient.initialize(tokenManager)

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SourcesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AppViewModel = viewModel(factory = factory)

                    LaunchedEffect(intent) {
                        handleIntent(intent, viewModel)
                    }

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

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Upload.route,
        Screen.Inbox.route,
        Screen.ReadingList.route
    )
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val pendingNav by viewModel.pendingNavigation.collectAsState()

    LaunchedEffect(pendingNav) {
        pendingNav?.let { route ->
            navController.navigate(route)
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(error) {
        error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library") },
                        label = { Text("Library") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate("home/${currentUser.username}") {
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
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Reading List") },
                        label = { Text("Reading") },
                        selected = currentRoute == Screen.ReadingList.route,
                        onClick = {
                            navController.navigate(Screen.ReadingList.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
            composable(Screen.ReadingList.route) {
                ReadingList(
                    viewModel = viewModel,
                    onComicClick = { comic ->
                        viewModel.setCurrentComic(comic)
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginPage(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onLoginSubmit = { credentials ->
                        viewModel.loginUser(credentials, onSuccess = {
                            navController.navigate("home/${currentUser.username}") {
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
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    username = viewModel.currentUser.collectAsState().value.username,
                    onComplete = { selectedUri ->
                        if (selectedUri != null) {
                            viewModel.uploadAvatar(context, selectedUri)
                        }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomePage(
                    viewModel = viewModel,
                    onComicClick = { comic ->
                        viewModel.setCurrentComic(comic)
                        navController.navigate(Screen.Overview.route)
                    },
                    onAccountClick = {
                        navController.navigate(Screen.Account.route) {
                            launchSingleTop = true
                        }
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                )
            }
            composable(Screen.Reader.route) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId")
                val comic by viewModel.currentComic.collectAsState()
                val pageCount by viewModel.pageCount.collectAsState()
                val chapters by viewModel.chapters.collectAsState()

                LaunchedEffect(chapterId, comic) {
                    if (chapterId != null && comic != null) {
                        viewModel.openChapter(comic!!, chapterId)
                        viewModel.getPostComments(chapterId)
                    }
                }

                if (comic == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: Comic data missing. Please go back.")
                    }
                } else if (pageCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val currentChapterIndex = chapters.indexOfFirst { it.id == chapterId }
                    
                    ReaderScreen(
                        pageCount = pageCount,
                        comic = comic!!,
                        viewModel = viewModel,
                        startingIndex = 0,
                        onPageChanged = { newPageIndex ->
                            if (chapterId != null) {
                                 viewModel.onPageTurned(chapterId, newPageIndex)
                            }
                        },
                        onNextChapter = {
                            if (currentChapterIndex != -1 && currentChapterIndex < chapters.size - 1) {
                                val nextId = chapters[currentChapterIndex + 1].id
                                navController.navigate(Screen.Reader.createRoute(nextId)) {
                                    popUpTo(Screen.Reader.route) { inclusive = true }
                                }
                            }
                        },
                        onPreviousChapter = {
                            if (currentChapterIndex > 0) {
                                val prevId = chapters[currentChapterIndex - 1].id
                                navController.navigate(Screen.Reader.createRoute(prevId)) {
                                    popUpTo(Screen.Reader.route) { inclusive = true }
                                }
                            }
                        },
                        onLikeChapter = {
                            if (chapterId != null) {
                                viewModel.likePost(chapterId)
                            }
                        },
                        onViewAllComments = { id ->
                            navController.navigate(Screen.Comments.createRoute(id))
                        },
                        onCommentThreadClick = { postId, commentId ->
                            navController.navigate(Screen.CommentThread.createRoute(postId, commentId))
                        }
                    )
                }
            }
            composable(Screen.Engagement.route) {
                EngagementPage(
                    viewModel = viewModel,
                    onCommentClick = { postId ->
                        navController.navigate(Screen.Comments.createRoute(postId))
                    },
                    onMakePost = {
                        navController.navigate(Screen.Post.route)
                    },
                    onAddAuthorClick = {
                        navController.navigate(Screen.AuthorSearch.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Comments.route) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                CommentsPage(
                    viewModel = viewModel,
                    postId = postId,
                    onBackClick = { navController.popBackStack() },
                    onCommentClick = { comment ->
                        navController.navigate(Screen.CommentThread.createRoute(postId, comment.id))
                    }
                )
            }
            composable(Screen.CommentThread.route) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                val commentId = backStackEntry.arguments?.getString("commentId") ?: return@composable
                CommentThreadPage(
                    viewModel = viewModel,
                    commentId = commentId,
                    postId = postId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Post.route) {
                PostPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Overview.route) {
                OverviewPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAuthorClick = {
                        navController.navigate(Screen.Engagement.route)
                                    },
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
                        navController.navigate("home/${currentUser.username}") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchPage(
                    viewModel = viewModel,
                    onComicClick = { comic ->
                        viewModel.setCurrentComic(comic)
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }
            composable(Screen.Account.route) {
                AccountPage(
                    viewModel = viewModel,
                    onLogoutClick = {
                        viewModel.logoutUser(onLogoutComplete = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    }
                )
            }
            composable(Screen.Inbox.route) {
                ChatInboxPage(
                    viewModel = viewModel,
                    onChatClick = { userId ->
                        navController.navigate(Screen.Chat.createRoute(userId))
                    },
                    onFriendRequest = {
                        navController.navigate(Screen.FriendRequest.route)
                    }
                )
            }
            composable(Screen.FriendRequest.route) {
                FriendRequestPage(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.AuthorSearch.route) {
                AuthorSearchPage(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.Chat.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                ChatThreadPage(
                    targetUserId = userId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}