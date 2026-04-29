package toro.sources.components

import toro.sources.DataModels.Comic
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Post
import toro.sources.DataModels.Comment

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

object MockEngagementData {

    val mockCommunityPosts = listOf(
        Post(
            id = "post_1",
            authorId = "user_101",
            authorName = "RenderJunkie",
            content = "Does anyone know any good comics that explain path tracing algorithms? Trying to visually understand the rendering equation better.",
            timestamp = System.currentTimeMillis() - 3600000L, // 1 hour ago
            likesCount = 14
        ),
        Post(
            id = "post_2",
            authorId = "user_102",
            authorName = "NihongoLearner",
            content = "Just uploaded a raw Japanese volume! Great practice for recognizing particles like 'o' and 'to' in casual conversation.",
            timestamp = System.currentTimeMillis() - 7200000L, // 2 hours ago
            likesCount = 32
        ),
        Post(
            id = "post_3",
            authorId = "user_103",
            authorName = "StitchMaster",
            content = "Currently reading a slice-of-life comic about a crocheting club. Highly recommend it if you need something relaxing to read after coding all day.",
            timestamp = System.currentTimeMillis() - 86400000L, // 1 day ago
            likesCount = 8
        ),
        Post(
            id = "post_4",
            authorId = "user_104",
            authorName = "Striker99",
            content = "Can we get a dedicated tag for action and soccer manga? My library is getting way too cluttered.",
            timestamp = System.currentTimeMillis() - 172800000L, // 2 days ago
            likesCount = 45
        )
    )

    // These comments simulate a thread attached specifically to "post_1"
    val mockCommentsForPost1 = listOf(
        Comment(
            id = "comment_1",
            authorId = "user_201",
            authorName = "RayTraceEnthusiast",
            content = "There's a great indie zine called 'Ray Tracing in One Weekend: The Comic'. I'll try to find the .cbz and upload it!",
            timestamp = System.currentTimeMillis() - 3000000L
        ),
        Comment(
            id = "comment_2",
            authorId = "user_202",
            authorName = "PixelPusher",
            content = "Wait, that exists?! Please upload it, I need that for my C++ pipeline.",
            timestamp = System.currentTimeMillis() - 1500000L
        )
    )
}