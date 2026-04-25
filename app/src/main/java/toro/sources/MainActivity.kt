package toro.sources

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import toro.sources.ui.theme.SourcesTheme
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.compose.composable
import toro.sources.pages.LoginPage
import toro.sources.pages.SignUpPage
import toro.sources.pages.HomePage
import toro.sources.pages.AccountPage
import toro.sources.pages.ChatPage
import toro.sources.pages.SearchPage
import toro.sources.pages.UploadPage
import toro.sources.pages.OverviewPage
import toro.sources.pages.EngagementPage
import toro.sources.pages.ContentPage

sealed class Page(val route: String) {
    object Login : Page("login")
    object SignUp : Page("signup")
    object Account : Page("account")
    object Home : Page("home/{userId}")

    object Upload : Page("upload")
    object Search : Page("search")
    object Content : Page("content") // Actual pages of the comic
    object Overview : Page("overview") // details of comic and episode list
    object Engagement : Page("engagement") // creator posts

    object Chat : Page("chat_page/{creatorId}") { // chat with creator
        fun createRoute(creatorId: String) = "chat_page/$creatorId"
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SourcesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Page.Login.route) {
        composable(Page.Login.route) {
            LoginPage()
        }
        composable(Page.SignUp.route) {
            SignUpPage()
        }
        composable(Page.Home.route) {
            HomePage()
        }
        composable(Page.Content.route) {
            ContentPage()
        }
        composable(Page.Engagement.route) {
            EngagementPage()
        }
        composable(Page.Overview.route) {
            OverviewPage()
        }
        composable(Page.Upload.route) {
            UploadPage()
        }
        composable(Page.Search.route) {
            SearchPage()
        }
        composable(Page.Account.route) {
            AccountPage()
        }
        composable(Page.Chat.route) {
            ChatPage()
        }
    }
}