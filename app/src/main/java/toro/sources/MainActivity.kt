package toro.sources

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import toro.sources.viewmodel.AuthViewModel
import toro.sources.viewmodel.ChatViewModel
import toro.sources.viewmodel.CommunityViewModel
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
import toro.sources.pages.ActivityPage
import toro.sources.pages.SearchPage
import toro.sources.pages.SettingsPage
import toro.sources.pages.SignUpPage
import toro.sources.pages.UploadPage
import toro.sources.pages.WelcomePage
import toro.sources.ui.theme.SourcesTheme
import toro.sources.components.ShareDialog
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.pages.AboutPage
import toro.sources.pages.InterestsPage
import toro.sources.pages.ReportPage
import toro.sources.pages.ReportTargetType
import toro.sources.pages.SuccessfulTaskPage
import toro.sources.viewmodel.NotificationsViewModel
import toro.sources.viewmodel.ProfileViewModel
import toro.sources.viewmodel.SessionViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object About : Screen("about")
    object Welcome : Screen("welcome")
    object Interests : Screen("interest")
    object Home : Screen("home/{userId}")
    object Upload : Screen("upload")
    object Success : Screen("success/{successMessage}") {
        fun createRoute(successMessage: String) = "success/$successMessage"
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
    object Activity : Screen("activity")
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

@AndroidEntryPoint
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val sessionViewModel: SessionViewModel = hiltViewModel()
            val isDarkTheme by sessionViewModel.isDarkTheme.collectAsState()

            SourcesTheme(
                darkTheme = isDarkTheme,
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(intent) {
                        sessionViewModel.handleIntent(intent)
                    }

                    AppNavigation(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(sessionViewModel: SessionViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Upload.route,
        Screen.Inbox.route,
        Screen.Activity.route
    )

    val currentUser by sessionViewModel.userProfile.collectAsState()
    val pendingNav by sessionViewModel.pendingNavigation.collectAsState()
    val showShareDialog by sessionViewModel.showShareDialog.collectAsState()
    val sharedContent by sessionViewModel.sharedContent.collectAsState()
    val chatViewModel: ChatViewModel = hiltViewModel()

    if (showShareDialog && sharedContent != null) {
        ShareDialog(
            sessionViewModel = sessionViewModel,
            chatViewModel = chatViewModel,
            sharedId = sharedContent!!.id,
            sharedType = sharedContent!!.type,
            sharedTitle = sharedContent!!.title,
            sharedPreview = sharedContent!!.previewText,
            sharedTargetId = sharedContent!!.targetId,
            onDismiss = { sessionViewModel.showShareDialog(false) }
        )
    }

    val startDestination = remember {
        if (RetrofitClient.preferenceManager.getAccessTokenSync() != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(pendingNav) {
        pendingNav?.let { route ->
            navController.navigate(route)
            sessionViewModel.onNavigationHandled()
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
                            navController.navigate("home/${currentUser?.id}") {
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
                        label = { Text("Activity") },
                        selected = currentRoute == Screen.Activity.route,
                        onClick = {
                            navController.navigate(Screen.Activity.route) {
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
            composable(Screen.Login.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()
                LoginPage(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onLoginSubmit = { credentials ->
                        authViewModel.loginUser(credentials, onSuccess = {
                            navController.navigate("home/${currentUser?.id}") {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    },
                    loginError = authState.errorMessage
                )
            }
            composable(Screen.SignUp.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()
                SignUpPage (
                    onNavigateBack = { navController.navigate((Screen.Login.route)) },
                    onSignUpSuccess = {newUser ->
                        authViewModel.registerNewUser(newUser, onSuccess = {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    },
                    signError = authState.errorMessage
                )
            }
            composable(Screen.Welcome.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                WelcomePage(
                    username = currentUser?.username ?: "User",
                    onComplete = { selectedUri ->
                        if (selectedUri != null) {
                            profileViewModel.uploadAvatar(selectedUri)
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
                val profileViewModel: ProfileViewModel = hiltViewModel()
                InterestsPage(
                    username = currentUser?.username ?: "User",
                    onComplete = { selectedGenres ->
                        profileViewModel.updateInterests(selectedGenres.map { it.name })
                    },
                    onProceed = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val profileViewModel: ProfileViewModel = hiltViewModel()
                SettingsPage(
                    sessionViewModel = sessionViewModel,
                    profileViewModel = profileViewModel,
                    onLogoutClick = {
                        authViewModel.logoutUser(onLogoutComplete = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    },
                    onDeleteAccountClick = {
                        authViewModel.deleteAccount(onComplete = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val chatViewModel: ChatViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val communityViewModel: CommunityViewModel = hiltViewModel()
                ProfilePage(
                    profileViewModel = profileViewModel,
                    sessionViewModel = sessionViewModel,
                    communityViewModel = communityViewModel,
                    chatViewModel = chatViewModel,
                    userId = userId,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onLogoutClick = {
                        authViewModel.logoutUser(onLogoutComplete = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                HomePage(
                    comicsViewModel = comicsViewModel,
                    sessionViewModel = sessionViewModel,
                    onComicClick = { comic ->
                        navController.navigate(Screen.Overview.createRoute(comic.id))
                    },
                    onAccountClick = {
                        currentUser?.id?.let { id ->
                            navController.navigate(Screen.Profile.createRoute(id)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                )
            }
            composable(Screen.Search.route) {
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                SearchPage(
                    comicsViewModel = comicsViewModel,
                    sessionViewModel = sessionViewModel,
                    onComicClick = { comic ->
                        comicsViewModel.setCurrentComic(comic)
                        navController.navigate(Screen.Overview.createRoute(comic.id))
                    }
                )
            }
            composable(Screen.Activity.route) {
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                val communityViewModel: CommunityViewModel = hiltViewModel()
                ActivityPage(
                    comicsViewModel = comicsViewModel,
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
                    onComicClick = { comic ->
                        navController.navigate(Screen.Overview.createRoute(comic.id))
                    },
                    onAddComic = { navController.navigate(Screen.Search.route) }
                )
            }
            composable(Screen.Overview.route) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: return@composable
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                val chatViewModel: ChatViewModel = hiltViewModel()
                val profileViewModel: ProfileViewModel = hiltViewModel()

                LaunchedEffect(comicId) {
                    comicsViewModel.setCurrentComic(Comic(id = comicId, title = "", description = "", coverImageUrl = ""))
                }

                OverviewPage(
                    comicsViewModel = comicsViewModel,
                    sessionViewModel = sessionViewModel,
                    chatViewModel = chatViewModel,
                    profileViewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() },
                    onAuthorClick = { authorId ->
                        navController.navigate(Screen.Profile.createRoute(authorId))
                    },
                    onComicClick = {
                        navController.navigate(Screen.Reader.createRoute(comicsViewModel.chapters.value.firstOrNull()?.id ?: ""))
                    },
                    onChapterClick = { chapter ->
                        navController.navigate(Screen.Reader.createRoute(chapter.id))
                        comicsViewModel.markChapterAsRead(comicId, chapter.id)
                    }
                )
            }
            composable(Screen.Reader.route) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                val comic by comicsViewModel.currentComic.collectAsState()
                val pageCount by comicsViewModel.pageCount.collectAsState()
                val chapters by comicsViewModel.chapters.collectAsState()

                LaunchedEffect(chapterId, comic) {
                    if (chapterId.isNotEmpty() && comic != null) {
                        comicsViewModel.openChapter(comic!!, chapterId)
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
                            comicsViewModel = comicsViewModel,
                            sessionViewModel = sessionViewModel,
                            chapterId = chapterId,
                            startingIndex = 0,
                            onBack = { navController.popBackStack() },
                            onPageChanged = { newPageIndex ->
                                comicsViewModel.onPageTurned(chapterId, newPageIndex)
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
                                comicsViewModel.likeChapter(comic!!.id, chapterId)
                            },
                            onViewAllComments = { _ ->
                                navController.navigate(Screen.ChapterComments.createRoute(chapterId))
                            },
                            onCommentThreadClick = { cId, commentId ->
                                navController.navigate(Screen.ChapterCommentThread.createRoute(cId, commentId))
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
            composable(Screen.Inbox.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatInboxPage(
                    chatViewModel = chatViewModel,
                    sessionViewModel = sessionViewModel,
                    onChatClick = { cId ->
                        navController.navigate(Screen.Chat.createRoute(cId))
                    },
                    onFriendRequest = {
                        navController.navigate(Screen.FriendRequest.route)
                    },
                    onProfileClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId))
                    }
                )
            }
            composable(Screen.Chat.route) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                val chatViewModel: ChatViewModel = hiltViewModel()
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                ChatThreadPage(
                    conversationId = conversationId,
                    chatViewModel = chatViewModel,
                    comicsViewModel = comicsViewModel,
                    profileViewModel = profileViewModel,
                    sessionViewModel = sessionViewModel,
                    onBackClick = { navController.popBackStack() },
                    onProfileClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId))
                    }
                )
            }
            composable(Screen.FriendRequest.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                FriendRequestPage(
                    chatViewModel = chatViewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.AuthorSearch.route) {
                val communityViewModel: CommunityViewModel = hiltViewModel()
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                AuthorSearchPage(
                    communityViewModel = communityViewModel,
                    comicsViewModel = comicsViewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.Engagement.route) {
                val communityViewModel: CommunityViewModel = hiltViewModel()
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                EngagementPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
                    comicsViewModel = comicsViewModel,
                    onCommentClick = { postId ->
                        navController.navigate(Screen.PostComments.createRoute(postId))
                    },
                    onMakePost = {
                        navController.navigate(Screen.Post.route)
                    },
                    onAddAuthorClick = {
                        navController.navigate(Screen.AuthorSearch.route)
                    }
                )
            }
            composable(Screen.Post.route) {
                val communityViewModel: CommunityViewModel = hiltViewModel()
                PostPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.PostComments.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                val communityViewModel: CommunityViewModel = hiltViewModel()
                CommentsPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
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
                val communityViewModel: CommunityViewModel = hiltViewModel()
                CommentThreadPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
                    commentLocation = CommentLocation.ON_POST,
                    targetId = targetId,
                    commentId = commentId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.ChapterComments.route) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: return@composable
                val communityViewModel: CommunityViewModel = hiltViewModel()
                CommentsPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
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
                val communityViewModel: CommunityViewModel = hiltViewModel()
                CommentThreadPage(
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
                    commentLocation = CommentLocation.ON_CHAPTER,
                    targetId = targetId,
                    commentId = commentId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Report.route) { backStackEntry ->
                val typeString = backStackEntry.arguments?.getString("targetType") ?: "APP"
                val targetId = backStackEntry.arguments?.getString("targetId")?.takeIf { it != "none" }
                val communityViewModel: CommunityViewModel = hiltViewModel()
                val targetType = ReportTargetType.valueOf(typeString)

                ReportPage(
                    communityViewModel = communityViewModel,
                    targetType = targetType,
                    targetId = targetId,
                    onBackClick = { navController.popBackStack() },
                    onSubmitSuccess = {
                        navController.popBackStack()
                        Toast.makeText(context, "Successfully sent", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            composable(Screen.Upload.route) {
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                UploadPage(
                    viewModel = comicsViewModel,
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
            composable(Screen.AddComic.route) {
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                val communityViewModel: CommunityViewModel = hiltViewModel()
                NewSeriesForm(
                    comicsViewModel = comicsViewModel,
                    communityViewModel = communityViewModel,
                    sessionViewModel = sessionViewModel,
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
                val comicsViewModel: ComicsViewModel = hiltViewModel()
                val profileViewModel: ProfileViewModel = hiltViewModel()
                AddChapterForm(
                    comicsViewModel = comicsViewModel,
                    profileViewModel = profileViewModel,
                    sessionViewModel = sessionViewModel,
                    onCancel = { navController.popBackStack() },
                    onUploadComplete = {
                        navController.navigate(Screen.Success.createRoute("Upload Successful!")) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Notifications.route) {
                val notificationsViewModel: NotificationsViewModel = hiltViewModel()
                NotificationsPage(
                    viewModel = notificationsViewModel,
                    sessionViewModel = sessionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                AboutPage (
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Screen.Success.route) { backStackEntry ->
                val successMessage = backStackEntry.arguments?.getString("successMessage") ?: ""
                SuccessfulTaskPage(
                    successMessage,
                    onTimeElapsed = {
                        navController.navigate("home/${currentUser?.id}") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}