package com.example.project_s

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
// 💡 실 보관소 아이콘용 ShoppingBag 임포트 추가 (원하는 아이콘으로 변경 가능)
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // 🎨 색상 설정을 위한 Color 임포트
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_s.ui.ProjectListScreen
import com.example.project_s.ui.counter.CounterScreen

// 🛠️ 1. 하단 탭 설정표 (BottomNavItem) 수정
sealed class BottomNavItem(val title: String, val route: String) {
    // ⬇️ 버튼의 설정표 ⬇️
    object Home : BottomNavItem("단수카운터", "home")    // 👈 탭 이름 '단수카운터'로 변경
    object Pattern : BottomNavItem("도안 보관", "pattern") // 👈 탭 이름 '도안 보관'으로 변경
    object Community : BottomNavItem("커뮤니티", "community")
    object Profile : BottomNavItem("프로필", "profile")
    object Yarn : BottomNavItem("실 보관소", "yarn")      // 👈 새로 추가된 탭!
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // 🛠️ 2. 시작 탭을 Home(단수카운터)으로 설정
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    val projectList = remember {
        mutableStateListOf(
            ProjectData(
                1, "가을 머플러", "대바늘",
                "메리노 울 · 4.0mm", 47, 120, "진행",
                "20단마다 무늬 바꾸기 🧶"
            )
        )
    }

    var selectedProject by remember { mutableStateOf<ProjectData?>(null) }

    // 🎨 앱 디자인 테마 색상 정의 (포인트 오렌지 및 아이보리 배경)
    val appBgColor = Color(0xFFF7F4EB)
    val pointOrangeColor = Color(0xFFD36D33)

    Scaffold(
        // 🛠️ [디자인 수정] 상단 상태바 및 전체 화면 배경색을 아이보리 톤으로 지정하여 흰색 들뜸 방지
        containerColor = appBgColor,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(90.dp), // 👈 하단 탭바 높이 설정 창
                // 🛠️ [디자인 수정] 하단 탭 바 배경색을 전체 화면 배경색과 동일하게 입힘
                containerColor = appBgColor
            ) {
                // 🛠️ 3. 하단 탭바 순서 재설정 (요청하신 5개 순서대로 배치)
                val items = listOf(
                    BottomNavItem.Yarn,       // 실 보관소 👈 추가됨
                    BottomNavItem.Pattern,    // 도안 보관
                    BottomNavItem.Home,       // 단수카운터
                    BottomNavItem.Community,  // 커뮤니티
                    BottomNavItem.Profile,    // 프로필
                )

                // ⬇️ 버튼을 하나씩 그려주는 일꾼 ⬇️
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
                            // 🛠️ 4. 하단 탭바 그림(아이콘) 매핑 수정
                            when (item) {
                                BottomNavItem.Home -> Icon(Icons.Default.Timer, contentDescription = "단수카운터") // 👈 홈(단수카운터)에 타이머 아이콘 적용
                                BottomNavItem.Pattern -> Icon(Icons.Default.List, contentDescription = "도안 보관")
                                BottomNavItem.Community -> Icon(Icons.Default.Share, contentDescription = "커뮤니티")
                                BottomNavItem.Profile -> Icon(Icons.Default.Person, contentDescription = "프로필")
                                BottomNavItem.Yarn -> Icon(Icons.Default.ShoppingBag, contentDescription = "실 보관소") // 👈 실 보관소용 아이콘 매핑 추가
                            }
                        },
                        // 🛠️ [디자인 수정] 탭 아이콘/글자 선택 시 강조 색상 지정
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = pointOrangeColor,
                            selectedTextColor = pointOrangeColor,
                            indicatorColor = pointOrangeColor.copy(alpha = 0.15f), // 선택된 탭 오렌지 하이라이트
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
            startDestination = BottomNavItem.Home.route, // 🛠️ 5. 앱 켜졌을 때 첫 화면을 Home(단수카운터)으로 유지
            modifier = Modifier.padding(innerPadding)
        ) {

            // 🛠️ 6. Home(route = "home") 화면을 단수카운터 로직으로 완전히 교체!
            composable(BottomNavItem.Home.route) {
                if (selectedProject == null) {
                    ProjectListScreen(
                        // 🧺 기존 있던 단수카운터 불러오기
                        projectList = projectList,
                        // ➕ 단 카운터 추가
                        onAddProject = { newProject -> projectList.add(newProject) },
                        // 🗑️ 단 카운터 삭제
                        onDeleteProject = { projectToDelete -> projectList.remove(projectToDelete) },
                        // 🧺 기존 카운터의 정보들을 불러오는 정보
                        onProjectSelect = { project -> selectedProject = project }
                    )
                } else {
                    CounterScreen(
                        project = selectedProject!!,
                        onBackClick = { selectedProject = null }
                    )
                }
            }

            // 🛠️ 7. 각 화면 이름 수정 및 신규 화면 추가
            composable(BottomNavItem.Pattern.route) {
                Text("도안 보관 화면 (준비 중)", modifier = Modifier.padding(16.dp)) // 👈 텍스트 수정
            }

            composable(BottomNavItem.Community.route) {
                Text("커뮤니티 화면 (준비 중)", modifier = Modifier.padding(16.dp))
            }

            composable(BottomNavItem.Profile.route) {
                Text("프로필 화면 (준비 중)", modifier = Modifier.padding(16.dp))
            }

            // 🛠️ 8. Yarn(실 보관소) 화면 라우팅 블록 새로 추가
            composable(BottomNavItem.Yarn.route) {
                Text("실 보관소 화면 (준비 중)", modifier = Modifier.padding(16.dp)) // 👈 신규 화면
            }
        }
    }
}