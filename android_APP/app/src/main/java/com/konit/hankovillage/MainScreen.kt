package com.konit.hankovillage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.konit.hankovillage.ui.auth.LoginScreen
import com.konit.hankovillage.ui.auth.UserSession
import com.konit.hankovillage.ui.community.CommentData
import com.konit.hankovillage.ui.community.CommunityCategory
import com.konit.hankovillage.ui.community.CommunityScreen
import com.konit.hankovillage.ui.community.PostData
import com.konit.hankovillage.ui.counter.CounterScreen
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.counter.ProjectListScreen
import com.konit.hankovillage.ui.pattern.PatternData
import com.konit.hankovillage.ui.pattern.PatternScreen
import com.konit.hankovillage.ui.profile.ProfileScreen
import com.konit.hankovillage.ui.yarn.YarnData
import com.konit.hankovillage.ui.yarn.YarnScreen

/**
 * 하단 네비게이션 바의 탭 메뉴 항목 데이터 모델
 */
sealed class BottomNavItem(val title: String, val route: String) {
    object Home : BottomNavItem("단수카운터", "home")
    object Pattern : BottomNavItem("도안 보관", "pattern")
    object Community : BottomNavItem("커뮤니티", "community")
    object Profile : BottomNavItem("프로필", "profile")
    object Yarn : BottomNavItem("실 보관소", "yarn")
}

/**
 * 앱의 메인 화면 컴포저블
 * 하단 네비게이션 바 제어, 사용자 로그인 세션 관리 및 비회원 접근 제한 권한을 제어합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    var currentUserSession by remember { mutableStateOf<UserSession?>(null) }
    var showLoginAuthDialog by remember { mutableStateOf(false) }

    // =========================================================================
    // 상태 호이스팅 (State Hoisting)
    // 탭 전환 시 데이터 손실을 방지하기 위해 MainScreen에서 데이터를 관리합니다.
    // =========================================================================
    val projectList = remember {
        mutableStateListOf(
            ProjectData(
                1, "가을 머플러", "대바늘",
                "메리노 울 · 4.0mm", 47, 120, "진행",
                "20단마다 무늬 바꾸기"
            )
        )
    }

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
                    CommentData(102L, currentUserSession?.profile?.nickname ?: "김뜨개", "메리노울 100g 4.0mm 바늘 썼습니다!", "2026-10-10 14:35", isMyComment = true)
                ),
                isMyPost = true
            )
        )
    }

    var selectedProject by rememberSaveable { mutableStateOf<ProjectData?>(null) }

    val appBgColor = Color(0xFFF7F4EB)
    val pointOrangeColor = Color(0xFFD36D33)

    // 프로필/로그인 탭으로 이동하는 공통 람다 함수
    val navigateToProfile = {
        selectedTab = BottomNavItem.Profile
        navController.navigate(BottomNavItem.Profile.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = appBgColor,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(90.dp),
                containerColor = appBgColor
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
                        selected = selectedTab == item,
                        onClick = {
                            val isLoggedIn = currentUserSession != null

                            if (!isLoggedIn && (item == BottomNavItem.Pattern || item == BottomNavItem.Yarn)) {
                                showLoginAuthDialog = true
                            } else {
                                selectedTab = item
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
                PatternScreen(patternList = patternList)
            }

            composable(BottomNavItem.Home.route) {
                if (selectedProject == null) {
                    ProjectListScreen(
                        projectList = projectList,
                        onAddProject = { newProject -> projectList.add(0, newProject) }, // 이 줄을 추가해야 에러가 사라집니다!
                        onDeleteProject = { projectToDelete -> projectList.remove(projectToDelete) },
                        onProjectSelect = { project -> selectedProject = project }
                    )
                } else {
                    BackHandler {
                        selectedProject = null
                    }

                    CounterScreen(
                        project = selectedProject!!,
                        onBackClick = { selectedProject = null },
                        onSaveProject = { savedProject ->
                            val index = projectList.indexOfFirst { it.id == savedProject.id }
                            if (index != -1) {
                                projectList[index] = savedProject
                            } else {
                                projectList.add(0, savedProject)
                            }
                            selectedProject = null
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
                    LoginScreen(
                        onLoginSuccess = { session ->
                            currentUserSession = session
                        },
                        onGuestMode = {
                            selectedTab = BottomNavItem.Home
                            navController.navigate(BottomNavItem.Home.route)
                        }
                    )
                } else {
                    ProfileScreen(
                        userProfile = currentSession.profile,
                        onUpdateProfile = { updatedProfile ->
                            currentUserSession = currentUserSession?.copy(profile = updatedProfile)
                        },
                        onLogout = {
                            currentUserSession = null
                            selectedTab = BottomNavItem.Home
                            navController.navigate(BottomNavItem.Home.route)
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