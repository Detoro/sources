package toro.sources

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import toro.sources.network.RetrofitClient
import toro.sources.pages.AuthorSearchPage
import toro.sources.pages.ChatInboxPage
import toro.sources.pages.ChatThreadPage
import toro.sources.pages.CommentThreadPage
import toro.sources.pages.CommentsPage
import com.toro.models.*
import toro.sources.components.AddChapterForm
import toro.sources.components.NewSeriesForm
import toro.sources.pages.EngagementPage
import toro.sources.pages.FriendRequestPage
import toro.sources.pages.HomePage
import toro.sources.pages.LoginPage
import toro.sources.pages.NotificationsPage
import toro.sources.pages.OverviewPage
import toro.sources.pages.PostPage
import toro.sources.pages.ProfilePage
import toro.sources.pages.ReaderPage
import toro.sources.pages.ReadingListPage
import toro.sources.pages.SearchPage
import toro.sources.pages.SettingsPage
import toro.sources.pages.SignUpPage
import toro.sources.pages.UploadPage
import toro.sources.pages.WelcomePage
import toro.sources.ui.theme.SourcesTheme
import toro.sources.components.ShareDialog
import toro.sources.pages.InterestsPage
import toro.sources.pages.ReportPage
import toro.sources.pages.ReportTargetType
import toro.sources.pages.SuccessfulTaskPage

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Welcome : Screen("welcome")
    object Interests : Screen("interest")
    object Home : Screen("home/{userId}")
    object Upload : Screen("upload")
    object Success : Screen("success/{successMessage}") {
        fun createRoute(message: String) = "success/$message"
    }
    object Inbox : Screen("inbox")
    object Search : Screen("search")
    object Reader : Screen("reader/{chapterId}") {
        fun createRoute(chapterId: String) = "reader/$chapterId"
    }
    object Overview : Screen("overview/{comicId}") {
        fun createRoute(comicId: String) = "overview/$comicId"
    }
    object AddComic : Screen("add_comic")
    object AddChapter : Screen("add_chapter")
    object Post : Screen("post")
    object Engagement : Screen("engagement")
    object Settings : Screen("settings")
    object FriendRequest : Screen("friend_request")
    object ReadingList : Screen("reading_list")
    object Report : Screen("report/{targetType}/{targetId}") {
        fun createRoute(targetType: String, targetId: String = "none") = "report/$targetType/$targetId"
    }
    object Notifications : Screen("notifications")
    object Chat : Screen("chat_page/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_page/$conversationId"
    }
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object AuthorSearch : Screen("author_search")
    object PostComments : Screen("post_comments/{targetId}") {
        fun createRoute(targetId: String) = "post_comments/$targetId"
    }
    object PostCommentThread : Screen("post_comment_thread/{targetId}/{commentId}") {
        fun createRoute(targetId: String, commentId: String) = "post_comment_thread/$targetId/$commentId"
    }
    object ChapterComments : Screen("chapter_comments/{targetId}") {
        fun createRoute(targetId: String) = "chapter_comments/$targetId"
    }
    object ChapterCommentThread : Screen("chapter_comment_thread/{targetId}/{commentId}") {
        fun createRoute(targetId: String, commentId: String) = "chapter_comment_thread/$targetId/$commentId"
    }
}

class MainActivity : ComponentActivity() {
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
            val routingType = it.getStringExtra("type")
            val id = it.getStringExtra("id")

            when (routingType) {
                NotificationType.CHAT.name -> {
                    if (id != null) {
                        viewModel.handleNavigation(Screen.Chat.createRoute(id))
                    }
                }
                NotificationType.LIKE.name -> {
                    viewModel.handleNavigation(Screen.Engagement.route)
                }
                NotificationType.COMMENT.name, NotificationType.FOLLOW.name -> {
                    if (id != null) {
                        viewModel.handleNavigation(Screen.PostComments.createRoute(id))
                    } else {
                        viewModel.handleNavigation(Screen.Notifications.route)
                    }
                }
                NotificationType.FRIEND_REQUEST.name -> {
                    viewModel.handleNavigation(Screen.FriendRequest.route)
                }
            }
            it.removeExtra("type")
            it.removeExtra("id")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        val preferenceManager = PreferenceManager(this)
        RetrofitClient.initialize(preferenceManager)

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
            val viewModel: AppViewModel = viewModel(factory = factory)

            SourcesTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    val pendingNav by viewModel.pendingNavigation.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val sharedContent by viewModel.sharedContent.collectAsState()

    if (showShareDialog && sharedContent != null) {
        ShareDialog(
            viewModel = viewModel,
            sharedId = sharedContent!!.id,
            sharedType = sharedContent!!.type,
            sharedTitle = sharedContent!!.title,
            sharedPreview = sharedContent!!.previewText,
            sharedTargetId = sharedContent!!.targetId,
            onDismiss = { viewModel.showShareDialog(false) }
        )
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val startDestination = remember {
        if (RetrofitClient.preferenceManager.getTokenSync() != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(pendingNav) {
        pendingNav?.let { route ->
            navController.navigate(route)
            viewModel.onNavigationHandled()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library") },
                        label = { Text("Home") },
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ReadingList.route) {
                ReadingListPage(
                    viewModel = viewModel,
                    onComicClick = { comic ->
                        navController.navigate(Screen.Overview.createRoute(comic.id))
                    },
                    onAddComic = { navController.navigate(Screen.Search.route) }
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
                WelcomePage(
                    username = viewModel.currentUser.collectAsState().value.username,
                    onComplete = { selectedUri ->
                        if (selectedUri != null) {
                            viewModel.uploadAvatar(selectedUri)
                        }
                    },
                    onProceed = {
                        navController.navigate(Screen.Interests.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Interests.route) {
                InterestsPage(
                    username = viewModel.currentUser.collectAsState().value.username,
                    onComplete = { selectedGenres ->
                        viewModel.updateInterests(selectedGenres.map { it.name })
                    },
                    onProceed = {
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
                        navController.navigate(Screen.Overview.createRoute(comic.id))
                    },
                    onAccountClick = {
                        navController.navigate(Screen.Profile.createRoute(currentUser.userId)) {
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
                } else {
                    if (pageCount > 0) {
                        val currentChapterIndex = chapters.indexOfFirst { it.id == chapterId }
                        
                        ReaderPage(
                            pageCount = pageCount,
                            comic = comic!!,
                            viewModel = viewModel,
                            chapterId = chapterId ?: "",
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
                                    viewModel.likeChapter(comic!!.id, chapterId)
                                }
                            },
                            onViewAllComments = { _ ->
                                navController.navigate(Screen.ChapterComments.createRoute(chapterId ?: ""))
                            },
                            onCommentThreadClick = { chapterId, commentId ->
                                navController.navigate(Screen.ChapterCommentThread.createRoute(chapterId, commentId))
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "Loading pages...")
                            }
                        }
                    }
                }
            }
            composable(Screen.Engagement.route) {
                EngagementPage(
                    viewModel = viewModel,
                    onCommentClick = { postId ->
                        navController.navigate(Screen.PostComments.createRoute(postId))
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
            composable(Screen.PostComments.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                CommentsPage(
                    viewModel = viewModel,
                    commentLocation = CommentLocation.ON_POST,
                    targetId = targetId,
                    onBackClick = { navController.popBackStack() },
                    onCommentClick = { comment ->
                        navController.navigate(Screen.PostCommentThread.createRoute(targetId, comment.id))
                    }
                )
            }
            composable(Screen.PostCommentThread.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                val commentId = backStackEntry.arguments?.getString("commentId") ?: return@composable
                CommentThreadPage(
                    viewModel = viewModel,
                    commentLocation = CommentLocation.ON_POST,
                    targetId = targetId,
                    commentId = commentId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.ChapterComments.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                CommentsPage(
                    viewModel = viewModel,
                    commentLocation = CommentLocation.ON_CHAPTER,
                    targetId = targetId,
                    onBackClick = { navController.popBackStack() },
                    onCommentClick = { comment ->
                        navController.navigate(Screen.ChapterCommentThread.createRoute(targetId, comment.id))
                    }
                )
            }
            composable(Screen.ChapterCommentThread.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                val commentId = backStackEntry.arguments?.getString("commentId") ?: return@composable
                CommentThreadPage(
                    viewModel = viewModel,
                    commentLocation = CommentLocation.ON_CHAPTER,
                    targetId = targetId,
                    commentId = commentId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Post.route) {
                PostPage(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Overview.route) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: return@composable

                LaunchedEffect(comicId) {
                    viewModel.loadComicById(comicId)
                }

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
                    onUploadNewComic = {
                        navController.navigate(Screen.AddComic.route)
                    },
                    onUploadNewChapter = {
                        navController.navigate(Screen.AddChapter.route)
                    },
                    onUploadComplete = {
                        navController.navigate(Screen.Success.createRoute("Upload Successful!")) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Success.route) { backStackEntry ->
                val successMessage = backStackEntry.arguments?.getString("successMessage") ?: return@composable
                SuccessfulTaskPage(
                    successMessage,
                    onTimeElapsed = {
                        navController.navigate("home/${currentUser.username}") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.AddComic.route) {
                NewSeriesForm(
                    viewModel = viewModel,
                    onCancel = { navController.popBackStack() },
                    onUploadComplete = {
                        navController.navigate(Screen.Success.createRoute("Upload Successful!")) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Screen.AddChapter.route) {
                AddChapterForm(
                    viewModel = viewModel,
                    onCancel = { navController.popBackStack() },
                    onUploadComplete = {
                        navController.navigate(Screen.Success.createRoute("Upload Successful!")) {
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
            composable(Screen.Settings.route) {
                SettingsPage(
                    viewModel = viewModel,
                    onLogoutClick = {
                        viewModel.logoutUser(onLogoutComplete = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    },
                    onBackClick = { navController.popBackStack() }
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
                    },
                    onProfileClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId))
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
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                ChatThreadPage(
                    conversationId = conversationId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onProfileClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId))
                    }
                )
            }
            composable(Screen.Profile.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                ProfilePage(
                    viewModel = viewModel,
                    userId = userId,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Report.route) { backStackEntry ->
                val typeString = backStackEntry.arguments?.getString("targetType") ?: "APP"
                val targetId = backStackEntry.arguments?.getString("targetId")?.takeIf { it != "none" }

                val targetType = ReportTargetType.valueOf(typeString)

                ReportPage(
                    viewModel = viewModel,
                    targetType = targetType,
                    targetId = targetId,
                    onBackClick = { navController.popBackStack() },
                    onSubmitSuccess = {
                        navController.popBackStack()
                        Toast.makeText(context, "Successfully sent", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}