package com.konit.hankovillage.network

import com.konit.hankovillage.ui.counter.ProjectCounterUpdateRequest
import com.konit.hankovillage.ui.counter.ProjectData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 한코마을 백엔드 FastAPI RESTful API 통신 인터페이스
 */
interface HankoApiService {

    // 1. 단수 카운터 목록 전체 조회 (GET /projects)
    @GET("projects")
    suspend fun getProjects(): Response<List<ProjectData>>

    // 2. 새 단수 카운터 생성 (POST /projects)
    @POST("projects")
    suspend fun createProject(
        @Body project: ProjectData
    ): Response<ProjectData>

    // 3. 단수 카운터 실시간 갱신 (PATCH /projects/{id}/counter) - {"current_row": 15}
    @PATCH("projects/{id}/counter")
    suspend fun updateProjectCounter(
        @Path("id") projectId: Long,
        @Body request: ProjectCounterUpdateRequest
    ): Response<ProjectData>

    // 4. 프로젝트 삭제 (DELETE /projects/{id})
    @DELETE("projects/{id}")
    suspend fun deleteProject(
        @Path("id") projectId: Long
    ): Response<Unit>
}