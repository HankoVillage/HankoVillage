package com.konit.hankovillage.network

import com.konit.hankovillage.ui.counter.ProjectCounterUpdateRequest
import com.konit.hankovillage.ui.counter.ProjectData
import retrofit2.Response
import com.google.gson.annotations.SerializedName
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

    @POST("auth/social/kakao")
    suspend fun socialLoginKakao(
        @Body request: SocialLoginRequest
    ): Response<SocialLoginResponse>

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

// 1. 요청/응답 DTO (HankoApiService 인터페이스 바깥 아래쪽에 추가)
data class SocialLoginRequest(
    @SerializedName("token")
    val token: String
)

data class SocialLoginResponse(
    @SerializedName("is_new_user")
    val isNewUser: Boolean,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Int? = null,
    @SerializedName("user")
    val user: AuthUserResponse? = null,
    @SerializedName("signup_token")
    val signupToken: String? = null,
    @SerializedName("signup_expires_in")
    val signupExpiresIn: Int? = null
)

data class AuthUserResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("nickname")
    val nickname: String
)