package toro.sources.sharing

import models.ShareType
import toro.sources.Screen
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel

fun handleSharedNavigation(
    id: String,
    type: ShareType,
    comicsViewModel: ComicsViewModel?,
    sessionViewModel: SessionViewModel
) {
    when (type) {
        ShareType.COMIC -> comicsViewModel?.loadAndNavigateToComic(id)
        ShareType.COMMENT -> sessionViewModel.handleNavigation(Screen.PostComments.createRoute(id))
        ShareType.POST -> sessionViewModel.handleNavigation(Screen.Engagement.route)
        ShareType.USER -> sessionViewModel.handleNavigation(Screen.Profile.createRoute(id))
    }
}