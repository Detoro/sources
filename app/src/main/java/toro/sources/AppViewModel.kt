package toro.sources
//
//import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
//import java.util.Calendar
//import java.util.Date
//
class AppViewModel : ViewModel() {
//    val currentUser = MutableStateFlow<UserProfile>(UserProfile())
//    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
//    private val _userProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
//    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
//    private val _loginError = MutableStateFlow<String?>(null)
//
//    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
//
//    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
//    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()
//    val organizations: StateFlow<List<Organization>> = _organizations.asStateFlow()
//    val userProfiles: StateFlow<List<UserProfile>> = _userProfiles.asStateFlow()
//    val loginError = _loginError.asStateFlow()
//
//
//    init {
//        fetchOrgsFromFlask()
//        fetchAllTasksFromFlask()
//    }
//
//    fun fetchAllTasksFromFlask() {
//        viewModelScope.launch {
//            try {
//                _tasks.value = RetrofitClient.apiInterface.getAllTasks()
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not fetch tasks: ${e.message}")
//            }
//        }
//    }
//
//    fun fetchUserProfilesFromFlask(orgId: String) {
//        viewModelScope.launch {
//            try {
//                _userProfiles.value = RetrofitClient.apiInterface.getUserProfiles(orgId)
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not fetch users: ${e.message}")
//            }
//        }
//    }
//
//    fun fetchOrgsFromFlask() {
//        viewModelScope.launch {
//            try {
//                _organizations.value = RetrofitClient.apiInterface.getOrgs()
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not fetch orgs: ${e.message}")
//            }
//        }
//    }
//
//    fun createTask(
//        orgId: String, title: String, description: String,
//        assignee: UserProfile?, daysUntilExpiry: Long
//    ) {
//        val calendar = Calendar.getInstance()
//        calendar.time = Date()
//        calendar.add(Calendar.DAY_OF_YEAR, daysUntilExpiry.toInt())
//
//        val newTask = Task(
//            id = "t_${System.currentTimeMillis()}",
//            orgId = orgId,
//            title = title,
//            description = description,
//            isCompleted = false,
//            assigner = currentUser.value,
//            assignee = assignee,
//            assignedDateTime = Date(),
//            expiryDate = calendar.time
//        )
//
//        viewModelScope.launch {
//            _tasks.value = _tasks.value + newTask
//            try {
//                RetrofitClient.apiInterface.setTask(newTask)
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not create task: ${e.message}")
//                _tasks.value = _tasks.value.filterNot { it.id == newTask.id }
//            }
//        }
//    }
//
//    fun pickTask(taskId: String, user: UserProfile) {
//        viewModelScope.launch {
//            _tasks.value = _tasks.value.map { task ->
//                if (task.id == taskId) task.copy(
//                    assignee = user,
//                    startTimestamp = Date()
//                ) else task
//            }
//
//            try {
//                val updateData = mapOf("assignee" to user)
//                RetrofitClient.apiInterface.updateTaskAssignee(taskId, updateData)
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not pick task: ${e.message}")
//                _tasks.value = _tasks.value.filterNot { it.id == taskId }
//            }
//        }
//    }
//
//    fun deleteTask(task: Task) {
//        viewModelScope.launch {
//            try {
//                RetrofitClient.apiInterface.deleteTask(task)
//
//                fetchAllTasksFromFlask()
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not delete task: ${e.message}")
//            }
//        }
//    }
//
//    fun assignTask(taskId: String, user: UserProfile? = null) {
//        viewModelScope.launch {
//            _tasks.value = _tasks.value.map { task ->
//                if (task.id == taskId) task.copy(assignee = user) else task
//            }
//
//            try {
//                val updateData = mapOf("assignee" to user)
//                RetrofitClient.apiInterface.updateTaskAssignee(taskId, updateData)
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not assign task: ${e.message}")
//            }
//        }
//    }
//
//    fun changeTaskCompletedStatus(task: Task) {
//        viewModelScope.launch {
//            val isNowComplete = !task.isCompleted
//            val currentTime = if (isNowComplete) Date() else null
//
//            _tasks.value = _tasks.value.map { t ->
//                if (t.id == task.id) {
//                    t.copy(
//                        isCompleted = isNowComplete,
//                        endTimestamp = currentTime
//                    )
//                } else t
//            }
//
//            try {
//                RetrofitClient.apiInterface.changeTaskStatus(task)
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not change task status: ${e.message}")
//            }
//        }
//    }
//
//    fun registerNewUser(newUser: SignUpPayload, onSuccess: () -> Unit) {
//        viewModelScope.launch {
//            try {
//                RetrofitClient.apiInterface.registerUser(newUser)
//                Log.d("Auth", "Successfully registered user: ${newUser.name}")
//                currentUser.value = UserProfile(
//                    id = newUser.id,
//                    name = newUser.name,
//                    role = newUser.role
//                )
//                onSuccess()
//            } catch (e: Exception) {
//                Log.e("NetworkError", "Could not register user: ${e.message}")
//            }
//        }
//    }
//
//    fun loginUser(credentials: LoginCredentials, onSuccess: () -> Unit) {
//        viewModelScope.launch {
//            _loginError.value = null
//
//            try {
//                val loggedInProfile = RetrofitClient.apiInterface.loginUser(credentials)
//                Log.d("Auth", "Successfully logged in as: ${loggedInProfile.name}")
//                currentUser.value = loggedInProfile
//                _loginError.value = null
//
//                onSuccess()
//
//            } catch (e: retrofit2.HttpException) {
//                if (e.code() == 401) {
//                    _loginError.value = e.message
//                } else {
//                    _loginError.value = "Server error occurred"
//                }
//                Log.e("NetworkError", "Login failed: ${e.message()}")
//            } catch (e: Exception) {
//                _loginError.value = "Could not connect to server"
//                Log.e("NetworkError", "Network failed: ${e.message}")
//            }
//        }
//    }
}