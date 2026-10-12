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
import com.konit.hankovillage.ui.community.CommunityScreen
import com.konit.hankovillage.ui.counter.CounterScreen
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.counter.ProjectListScreen
import com.konit.hankovillage.ui.pattern.PatternScreen
import com.konit.hankovillage.ui.profile.ProfileScreen
import com.konit.hankovillage.ui.yarn.YarnScreen

sealed class BottomNavItem(val title: String, val route: String) {
    object Home : BottomNavItem("단수카운터", "home")
    object Pattern : BottomNavItem("도안 보관", "pattern")
    object Community : BottomNavItem("커뮤니티", "community")
    object Profile : BottomNavItem("프로필", "profile")
    object Yarn : BottomNavItem("실 보관소", "yarn")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    // 💡 로그인 유저 세션 관리 (null일 경우 비회원 둘러보기 상태)
    var currentUserSession by remember { mutableStateOf<UserSession?>(null) }

    // 🔒 비회원 제한 기능(도안/실 보관소) 접근 시 로그인 유도 다이얼로그 상태
    var showLoginAuthDialog by remember { mutableStateOf(false) }

    val projectList = remember {
        mutableStateListOf(
            ProjectData(
                1, "가을 머플러", "대바늘",
                "메리노 울 · 4.0mm", 47, 120, "진행",
                "20단마다 무늬 바꾸기"
            )
        )
    }

    var selectedProject by rememberSaveable { mutableStateOf<ProjectData?>(null) }

    val appBgColor = Color(0xFFF7F4EB)
    val pointOrangeColor = Color(0xFFD36D33)

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

                            // 🔒 비회원(로그아웃)일 때 회원 전용 탭(도안 보관소, 실 보관소) 누르면 접근 제어
                            // 💡 단수카운터(Home)와 커뮤니티(Community)는 비회원도 둘러보기 가능!
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
            // 🧶 실 보관소 (회원 전용)
            composable(BottomNavItem.Yarn.route) {
                YarnScreen()
            }

            // 📂 도안 보관소 (회원 전용)
            composable(BottomNavItem.Pattern.route) {
                PatternScreen()
            }

            // ⏱️ 단수 카운터 (비회원/회원 모두 둘러보기 가능)
            composable(BottomNavItem.Home.route) {
                if (selectedProject == null) {
                    ProjectListScreen(
                        projectList = projectList,
                        onAddProject = { newProject -> projectList.add(newProject) },
                        onDeleteProject = { projectToDelete -> projectList.remove(projectToDelete) },
                        onProjectSelect = { project -> selectedProject = project }
                    )
                } else {
                    BackHandler {
                        selectedProject = null
                    }

                    CounterScreen(
                        project = selectedProject!!,
                        onBackClick = { selectedProject = null }
                    )
                }
            }

            // 💬 커뮤니티 (비회원/회원 모두 둘러보기 가능)
            composable(BottomNavItem.Community.route) {
                CommunityScreen(
                    isLoggedIn = currentUserSession != null,
                    currentNickname = currentUserSession?.profile?.nickname ?: "익명"
                )
            }

            // 👤 프로필 (비회원 시 소셜 로그인 화면)
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

    // 🔒 도안/실 보관소 탭 클릭 시 표시되는 로그인 유도 팝업
    if (showLoginAuthDialog) {
        AlertDialog(
            onDismissRequest = { showLoginAuthDialog = false },
            title = { Text("로그인 필요", fontWeight = FontWeight.Bold) },
            text = { Text("도안 보관소와 실 보관소는 로그인 후 이용할 수 있습니다. 로그인하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginAuthDialog = false
                        selectedTab = BottomNavItem.Profile
                        navController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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