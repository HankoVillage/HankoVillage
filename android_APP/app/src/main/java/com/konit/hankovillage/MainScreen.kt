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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.konit.hankovillage.ui.counter.ProjectListScreen
import com.konit.hankovillage.ui.counter.CounterScreen
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.pattern.PatternScreen
import com.konit.hankovillage.ui.yarn.YarnScreen


/**
 * 앱의 하단 네비게이션 탭 아이템 정의
 *
 * @property title 탭에 표시될 타이틀
 * @property route 네비게이션 경로
 */
sealed class BottomNavItem(val title: String, val route: String) {
    object Home : BottomNavItem("단수카운터", "home")
    object Pattern : BottomNavItem("도안 보관", "pattern")
    object Community : BottomNavItem("커뮤니티", "community")
    object Profile : BottomNavItem("프로필", "profile")
    object Yarn : BottomNavItem("실 보관소", "yarn")
}

/**
 * 앱의 메인 화면 구조 및 네비게이션 바를 구성하는 컴포저블
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

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
                            selectedTab = item
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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

            composable(BottomNavItem.Pattern.route) {
                PatternScreen()
            }

            composable(BottomNavItem.Yarn.route) {
                YarnScreen()
            }

            composable(BottomNavItem.Community.route) {
                Text("프로필 화면 (준비 중)", modifier = Modifier.padding(16.dp))
            }

            composable(BottomNavItem.Profile.route) {
                Text("프로필 화면 (준비 중)", modifier = Modifier.padding(16.dp))
            }


        }
    }
}