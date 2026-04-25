package toro.sources
//
//import retrofit2.http.Body
//import retrofit2.http.GET
//import retrofit2.http.POST
//import retrofit2.http.PUT
//import retrofit2.http.Path
//import tofunmi.volunteer.volunteertasktracker.models.LoginCredentials
//import tofunmi.volunteer.volunteertasktracker.models.Organization
//import tofunmi.volunteer.volunteertasktracker.models.DashboardItem.Task
//import tofunmi.volunteer.volunteertasktracker.models.SignUpPayload
//import tofunmi.volunteer.volunteertasktracker.models.UserProfile
//import tofunmi.volunteer.volunteertasktracker.models.VolunteerGroup
//
//
interface ApiInterface {
//    @GET("api/tasks")
//    suspend fun getAllTasks(): List<Task>
//
//    @POST("api/users/register")
//    suspend fun registerUser(@Body user: SignUpPayload)
//
//    @POST("api/users/login")
//    suspend fun loginUser(@Body loginCredentials: LoginCredentials): UserProfile
//
//    @POST("api/tasks/create")
//    suspend fun setTask(@Body task: Task)
//
//    @POST("api/tasks/delete")
//    suspend fun deleteTask(@Body task: Task)
//
//    @POST("api/tasks/log")
//    suspend fun changeTaskStatus(@Body task: Task)
//
//    @PUT("api/tasks/{taskId}/update")
//    suspend fun updateTaskAssignee(@Path("taskId") taskId: String, @Body updateData: Map<String, UserProfile?>)
//
//    @GET("api/orgs")
//    suspend fun getOrgs(): List<Organization>
//
//    @GET("api/users/{orgId}")
//    suspend fun getUserProfiles(@Path("orgId") orgId: String): List<UserProfile>
//
//    @GET("/api/groups/{org_id}")
//    suspend fun getGroups(@Path("org_id") orgId: String): List<VolunteerGroup>
//
//
//    @POST("/api/groups/create")
//    suspend fun setGroup(@Body group: VolunteerGroup)
//
//    @POST("/api/groups/delete")
//    suspend fun deleteGroup(@Body group: VolunteerGroup)
}