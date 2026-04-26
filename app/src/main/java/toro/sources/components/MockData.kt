package toro.sources.components

import toro.sources.DataModels.Comic
import toro.sources.DataModels.Chapter

object MockDataGenerator {

    val mockLibrary = listOf(
        Comic(
            id = "comic_1",
            title = "The Neon Samurai",
            author = "K. Reynolds",
            description = "In a cyberpunk future, a lone warrior must protect the last remaining organic city from a rogue AI corporation.",
            coverImageUrl = "https://picsum.photos/seed/cyber/400/600",
            isLocalSideload = false
        ),
        Comic(
            id = "comic_2",
            title = "Astromancers",
            author = "Sarah J. Lin",
            description = "Magic meets astrophysics. Follow a crew of spell-casters as they navigate the dangerous outer rim of the galaxy.",
            coverImageUrl = "https://picsum.photos/seed/space/400/600",
            isLocalSideload = false
        ),
        Comic(
            id = "comic_3",
            title = "Midnight Detectives",
            author = "Marcus Thorne",
            description = "A gritty noir series about two private eyes investigating supernatural occurrences in 1920s Chicago.",
            coverImageUrl = "https://picsum.photos/seed/noir/400/600",
            isLocalSideload = true
        )
    )

    val mockChapters = listOf(
        Chapter(id = "ch_1", comicId = "comic_1", chapterTitle = "Issue #1: Awakening", lastReadPageIndex = 12, isDownloaded = true),
        Chapter(id = "ch_2", comicId = "comic_1", chapterTitle = "Issue #2: The Grid", lastReadPageIndex = 0, isDownloaded = false),
        Chapter(id = "ch_3", comicId = "comic_1", chapterTitle = "Issue #3: Override", lastReadPageIndex = 0, isDownloaded = false),
        Chapter(id = "ch_4", comicId = "comic_2", chapterTitle = "Volume 1: The Nebula", lastReadPageIndex = 0, isDownloaded = false),
        Chapter(id = "ch_1", comicId = "comic_3", chapterTitle = "Volume 1: The Nebula", lastReadPageIndex = 50, isDownloaded = true)
    )
}