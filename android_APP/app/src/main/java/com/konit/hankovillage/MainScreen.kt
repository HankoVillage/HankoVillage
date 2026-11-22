package com.konit.hankovillage

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kakao.sdk.user.UserApiClient
import com.konit.hankovillage.data.local.GuestProjectStorage
import com.konit.hankovillage.network.RefreshRequest
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SessionManager
import com.konit.hankovillage.ui.auth.LoginScreen
import com.konit.hankovillage.ui.auth.SignupScreen
import com.konit.hankovillage.ui.auth.UserSession
import com.konit.hankovillage.ui.community.CommentData
import com.konit.hankovillage.ui.community.CommunityCategory
import com.konit.hankovillage.ui.community.CommunityScreen
import com.konit.hankovillage.ui.community.PostData
import com.konit.hankovillage.ui.counter.CounterScreen
import com.konit.hankovillage.ui.counter.ProjectCounterUpdateRequest
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.counter.ProjectDataSaver
import com.konit.hankovillage.ui.counter.ProjectListScreen
import com.konit.hankovillage.ui.pattern.PatternCreateRequest
import com.konit.hankovillage.ui.pattern.PatternData
import com.konit.hankovillage.ui.pattern.PatternScreen
import com.konit.hankovillage.ui.pattern.PatternUpdateRequest
import com.konit.hankovillage.ui.profile.ProfileScreen
import com.konit.hankovillage.ui.profile.UserProfileData
import com.konit.hankovillage.ui.yarn.YarnData
import com.konit.hankovillage.ui.yarn.YarnScreen
import kotlinx.coroutines.launch

/**
 * 하단 네비게이션 바의 탭 메뉴 항목 데이터 모델입니다.
 *
 * @property title 화면 노출용 탭 이름
 * @property route 네비게이션 라우트 경로
 */
sealed class BottomNavItem(val title: String, val route: String) {
    object Home : BottomNavItem("단수카운터", "home")
    object Pattern : BottomNavItem("도안 보관", "pattern")
    object Community : BottomNavItem("커뮤니티", "community")
    object Profile : BottomNavItem("프로필", "profile")
    object Yarn : BottomNavItem("실 보관소", "yarn")
}

/**
 * 앱의 메인 화면 컴포저블입니다.
 *
 * 하단 네비게이션 바 제어, 로그인 세션 상태 및 회원가입 흐름 관리,
 * 프로젝트·도안 API 연동 및 비회원 로컬 저장소 동기화를 담당합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { RetrofitInstance.getApiService(context) }

    var currentUserSession by remember { mutableStateOf<UserSession?>(null) }
    var isCheckingAutoLogin by remember { mutableStateOf(true) }
    var pendingSignupToken by remember { mutableStateOf<String?>(null) }
    var pendingSignupProvider by remember { mutableStateOf("kakao") }
    var showLoginAuthDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavItem.Home.route

    val projectList = remember { mutableStateListOf<ProjectData>() }
    val yarnList = remember { mutableStateListOf<YarnData>() }
    val patternList = remember { mutableStateListOf<PatternData>() }

    val postList = remember {
        mutableStateListOf(
            PostData(
                id = 1L,
                title = "이번에 완성한 대바늘 꽈배기 니트 뽐냅니다!",
                category = CommunityCategory.SHOWOFF,
                authorName = currentUserSession?.profile?.nickname ?: "김뜨개",
                createdAt = "2026-10-10",
                content = "3주 동안 도안 보면서 열공해서 만들었어요! 생각보다 핏이 너무 예쁘게 나와서 만족스럽습니다.",
                likeCount = 12,
                comments = mutableListOf(
                    CommentData(101L, "뜨개장인", "와 너무 예뻐요! 어떤 실 쓰셨나요?", "2026-10-10 14:20"),
                    CommentData(
                        102L,
                        currentUserSession?.profile?.nickname ?: "김뜨개",
                        "메리노울 100g 4.0mm 바늘 썼습니다!",
                        "2026-10-10 14:35",
                        isMyComment = true
                    )
                ),
                isMyPost = true
            )
        )
    }

    var selectedProject by rememberSaveable(stateSaver = ProjectDataSaver) {
        mutableStateOf<ProjectData?>(null)
    }

    fun clearUserSessionData() {
        SessionManager.clearTokens(context)
        currentUserSession = null
        pendingSignupToken = null
        pendingSignupProvider = "kakao"

        projectList.clear()
        selectedProject = null
        yarnList.clear()
        patternList.clear()
        postList.clear()
    }

    val appBgColor = Color(0xFFF7F4EB)
    val pointOrangeColor = Color(0xFFD36D33)

    val navigateToProfile = {
        navController.navigate(BottomNavItem.Profile.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        val savedRefresh = SessionManager.getRefreshToken(context)
        val savedUserId = SessionManager.getUserId(context)
        val savedNickname = SessionManager.getNickname(context)
        val savedProvider = SessionManager.getProvider(context)

        if (!savedRefresh.isNullOrEmpty() && !savedUserId.isNullOrEmpty()) {
            currentUserSession = UserSession(
                userId = savedUserId,
                provider = savedProvider,
                providerId = "",
                profile = UserProfileData(
                    nickname = savedNickname ?: "뜨개이웃"
                )
            )
        }
        isCheckingAutoLogin = false
    }

    LaunchedEffect(currentUserSession, isCheckingAutoLogin) {
        if (isCheckingAutoLogin) return@LaunchedEffect

        if (currentUserSession == null) {
            val savedGuestProjects = GuestProjectStorage.loadProjects(context)
            projectList.clear()
            projectList.addAll(savedGuestProjects)
            return@LaunchedEffect
        }

        projectList.clear()
        patternList.clear()

        try {
            val response = apiService.getProjects()
            if (response.isSuccessful) {
                response.body()?.let { serverList ->
                    projectList.addAll(serverList)
                }
            } else {
                Toast.makeText(context, "프로젝트 불러오기 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
            }

            val patternResponse = apiService.getPatterns(limit = 100)
            if (patternResponse.isSuccessful) {
                patternResponse.body()?.let { serverPatterns ->
                    patternList.addAll(serverPatterns)
                }
            } else {
                Toast.makeText(context, "도안 불러오기 실패 (${patternResponse.code()})", Toast.LENGTH_SHORT).show()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Toast.makeText(context, "목록 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        SessionManager.sessionExpiredEvent.collect {
            clearUserSessionData()
            Toast.makeText(context, "로그인이 만료되었습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()
            navigateToProfile()
        }
    }

    if (isCheckingAutoLogin) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = appBgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pointOrangeColor)
            }
        }
        return
    }

    Scaffold(
        containerColor = appBgColor,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = appBgColor,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                val items = listOf(
                    BottomNavItem.Yarn,
                    BottomNavItem.Pattern,
                    BottomNavItem.Home,
                    BottomNavItem.Community,
                    BottomNavItem.Profile,
                )

                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            val isLoggedIn = currentUserSession != null
                            if (!isLoggedIn && (item == BottomNavItem.Pattern || item == BottomNavItem.Yarn)) {
                                showLoginAuthDialog = true
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        label = { Text(item.title) },
                        icon = {
                            when (item) {
                                BottomNavItem.Home -> Icon(Icons.Default.Timer, contentDescription = "단수카운터")
                                BottomNavItem.Pattern -> Icon(Icons.Default.List, contentDescription = "도안 보관")
                                BottomNavItem.Community -> Icon(Icons.Default.Share, contentDescription = "커뮤니티")
                                BottomNavItem.Profile -> Icon(Icons.Default.Person, contentDescription = "프로필")
                                BottomNavItem.Yarn -> Icon(Icons.Default.ShoppingBag, contentDescription = "실 보관소")
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = pointOrangeColor,
                            selectedTextColor = pointOrangeColor,
                            indicatorColor = pointOrangeColor.copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF4A3B32),
                            unselectedTextColor = Color(0xFF4A3B32)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Yarn.route) {
                YarnScreen(yarnList = yarnList)
            }

            composable(BottomNavItem.Pattern.route) {
                PatternScreen(
                    patternList = patternList,
                    onAddPattern = { newPattern ->
                        coroutineScope.launch {
                            try {
                                val request = PatternCreateRequest(
                                    title = newPattern.title,
                                    tag = newPattern.tagCode,
                                    status = newPattern.statusCode,
                                    linkUrl = newPattern.patternUrl?.ifBlank { null },
                                    finishedPhotoKey = newPattern.imageUrl,
                                    yarnInfo = newPattern.yarnInfo?.ifBlank { null }
                                )
                                val response = apiService.createPattern(request)
                                if (response.isSuccessful && response.body() != null) {
                                    patternList.add(0, response.body()!!)
                                } else {
                                    Toast.makeText(context, "도안 등록 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onUpdatePattern = { updatedPattern ->
                        coroutineScope.launch {
                            try {
                                val request = PatternUpdateRequest(
                                    title = updatedPattern.title,
                                    tag = updatedPattern.tagCode,
                                    status = updatedPattern.statusCode,
                                    linkUrl = updatedPattern.patternUrl?.ifBlank { null },
                                    finishedPhotoKey = updatedPattern.imageUrl,
                                    yarnInfo = updatedPattern.yarnInfo?.ifBlank { null }
                                )
                                val response = apiService.updatePattern(updatedPattern.id, request)
                                if (response.isSuccessful && response.body() != null) {
                                    val serverPattern = response.body()!!
                                    val index = patternList.indexOfFirst { it.id == serverPattern.id }
                                    if (index != -1) {
                                        patternList[index] = serverPattern
                                    }
                                } else {
                                    Toast.makeText(context, "도안 수정 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeletePattern = { deletedId ->
                        coroutineScope.launch {
                            try {
                                val response = apiService.deletePattern(deletedId)
                                if (response.isSuccessful) {
                                    patternList.removeIf { it.id == deletedId }
                                } else {
                                    Toast.makeText(context, "도안 삭제 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            composable(BottomNavItem.Home.route) {
                val isLoggedIn = currentUserSession != null

                if (selectedProject == null) {
                    ProjectListScreen(
                        projectList = projectList,
                        yarnList = yarnList,
                        onAddProject = { newProject ->
                            if (isLoggedIn) {
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.createProject(newProject)
                                        if (response.isSuccessful && response.body() != null) {
                                            projectList.add(0, response.body()!!)
                                        } else {
                                            Toast.makeText(context, "생성 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                val guestProject = newProject.copy(id = System.currentTimeMillis())
                                projectList.add(0, guestProject)
                                GuestProjectStorage.saveProjects(context, projectList)
                            }
                        },
                        onDeleteProject = { projectToDelete ->
                            if (isLoggedIn) {
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.deleteProject(projectToDelete.id)
                                        if (response.isSuccessful) {
                                            projectList.remove(projectToDelete)
                                        } else {
                                            Toast.makeText(context, "삭제 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                projectList.remove(projectToDelete)
                                GuestProjectStorage.saveProjects(context, projectList)
                            }
                        },
                        onProjectSelect = { project -> selectedProject = project }
                    )
                } else {
                    BackHandler {
                        selectedProject = null
                    }
                    CounterScreen(
                        project = selectedProject!!,
                        yarnList = yarnList,
                        onBackClick = { selectedProject = null },
                        onSaveProject = { savedProject ->
                            val index = projectList.indexOfFirst { it.id == savedProject.id }
                            if (index != -1) {
                                projectList[index] = savedProject
                            } else {
                                projectList.add(0, savedProject)
                            }
                            selectedProject = savedProject

                            if (isLoggedIn) {
                                coroutineScope.launch {
                                    try {
                                        apiService.updateProjectCounter(
                                            projectId = savedProject.id,
                                            request = ProjectCounterUpdateRequest(currentRow = savedProject.currentRows)
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "단수 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                GuestProjectStorage.saveProjects(context, projectList)
                            }
                        }
                    )
                }
            }

            composable(BottomNavItem.Community.route) {
                CommunityScreen(
                    postList = postList,
                    isLoggedIn = currentUserSession != null,
                    currentNickname = currentUserSession?.profile?.nickname ?: "익명",
                    onRequireLogin = {
                        navigateToProfile()
                    }
                )
            }

            composable(BottomNavItem.Profile.route) {
                val currentSession = currentUserSession
                if (currentSession == null) {
                    if (pendingSignupToken != null) {
                        SignupScreen(
                            signupToken = pendingSignupToken!!,
                            provider = pendingSignupProvider,
                            onSignupSuccess = { session ->
                                GuestProjectStorage.clearProjects(context)
                                SessionManager.saveUserInfo(context, session.userId, session.profile.nickname, session.provider)
                                pendingSignupToken = null
                                currentUserSession = session
                            },
                            onCancelOrExpired = {
                                pendingSignupToken = null
                            }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = { session ->
                                GuestProjectStorage.clearProjects(context)
                                SessionManager.saveUserInfo(context, session.userId, session.profile.nickname, session.provider)
                                currentUserSession = session
                            },
                            onGuestMode = {
                                navController.navigate(BottomNavItem.Home.route)
                            },
                            onSignupRequired = { signupToken, provider ->
                                pendingSignupToken = signupToken
                                pendingSignupProvider = provider
                            }
                        )
                    }
                } else {
                    ProfileScreen(
                        userProfile = currentSession.profile,
                        provider = currentSession.provider,
                        postList = postList,
                        patternList = patternList,
                        onUpdateProfile = { updatedProfile ->
                            currentUserSession?.let { session ->
                                SessionManager.saveUserInfo(context, session.userId, updatedProfile.nickname, session.provider)
                            }
                            currentUserSession = currentUserSession?.copy(profile = updatedProfile)
                        },
                        onLogout = {
                            val refreshToken = SessionManager.getRefreshToken(context)
                            val provider = currentSession.provider

                            clearUserSessionData()
                            navController.navigate(BottomNavItem.Home.route)

                            if (provider == "kakao") {
                                UserApiClient.instance.logout { error ->
                                    if (error != null) {
                                        Log.e("KakaoAuth", "카카오 SDK 로그아웃 실패", error)
                                    }
                                }
                            } else if (provider == "google") {
                                coroutineScope.launch {
                                    try {
                                        CredentialManager.create(context)
                                            .clearCredentialState(ClearCredentialStateRequest())
                                    } catch (e: Exception) {
                                        Log.e("GoogleAuth", "구글 자격 증명 캐시 초기화 실패", e)
                                    }
                                }
                            }

                            if (!refreshToken.isNullOrEmpty()) {
                                coroutineScope.launch {
                                    try {
                                        apiService.logout(RefreshRequest(refreshToken))
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        },
                        onWithdrawSuccess = {
                            clearUserSessionData()
                            navigateToProfile()
                        }
                    )
                }
            }
        }
    }

    if (showLoginAuthDialog) {
        AlertDialog(
            onDismissRequest = { showLoginAuthDialog = false },
            title = { Text("로그인 필요", fontWeight = FontWeight.Bold) },
            text = { Text("도안 보관소와 실 보관소는 로그인 후 이용할 수 있습니다. 로그인하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginAuthDialog = false
                        navigateToProfile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pointOrangeColor)
                ) {
                    Text("로그인하러 가기", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginAuthDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}