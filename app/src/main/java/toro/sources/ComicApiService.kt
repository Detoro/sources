package toro.sources

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body
import toro.sources.DataModels.Comic
import toro.sources.DataModels.ServerResponse
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Page

interface ComicApiService {
    @GET("api/comics/catalog")
    suspend fun getCatalog(): List<Comic>

    // Fetches metadata for a single specific comic
    @GET("api/comics/{comicId}")
    suspend fun getComicDetails(
        @Path("comicId") comicId: String
    ): Comic

    @POST("api/hello")
    suspend fun sayHello(@Body comic: Comic): ServerResponse

    // Fetches all available chapters/issues for a specific comic
    @GET("api/comics/{comicId}/chapters")
    suspend fun getChaptersForComic(
        @Path("comicId") comicId: String
    ): List<Chapter>

    // Fetches the list of page images to stream for a specific chapter
    @GET("api/chapters/{chapterId}/pages")
    suspend fun getPagesForChapter(
        @Path("chapterId") chapterId: String
    ): List<Page>
}