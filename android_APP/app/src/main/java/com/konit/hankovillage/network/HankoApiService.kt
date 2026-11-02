package com.konit.hankovillage.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path

data class ProjectData(
    val id: Int? = null,
    val title: String,
    val info: String,
    val needleType: String,
    val currentCount: Int,
    val targetCount: Int
)

interface HankoApiService {
    // 1. 단수 카운터 목록 조회
    @GET("/projects")
    suspend fun getProjects(): List<ProjectData>

    // 2. 새 단수 카운터 생성
    @POST("/projects")
    suspend fun createProject(@Body project: ProjectData): ProjectData

    // 3. 단수 조작 (+/- 버튼 클릭 시 서버 반영)
    @PATCH("/projects/{project_id}/counter")
    suspend fun updateCounter(
        @Path("project_id") projectId: Int,
        @Body body: Map<String, Int> // {"current_count": 15}
    ): ProjectData

    // 4. 프로젝트 삭제
    @DELETE("/projects/{project_id}")
    suspend fun deleteProject(@Path("project_id") projectId: Int)
}