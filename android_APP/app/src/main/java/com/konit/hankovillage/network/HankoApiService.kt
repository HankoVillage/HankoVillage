package com.konit.hankovillage.network

import com.google.gson.annotations.SerializedName
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
 * 한코마을 백엔드 RESTful API 통신 인터페이스입니다.
 */
interface HankoApiService {

    @POST("auth/refresh")
    suspend fun refreshTokens(@Body request: RefreshRequest): Response<RefreshResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @POST("auth/social/google")
    suspend fun socialLoginGoogle(
        @Body request: SocialLoginRequest
    ): Response<SocialLoginResponse>

    /**
     * 카카오 임시 가입 토큰과 사용자 입력 프로필 정보를 전달하여 최종 회원가입을 완료합니다.
     */
    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SocialLoginResponse>

    /**
     * 카카오 인증 토큰(ID Token)을 전달하여 소셜 로그인 또는 가입 필요 여부를 확인합니다.
     */
    @POST("auth/social/kakao")
    suspend fun socialLoginKakao(
        @Body request: SocialLoginRequest
    ): Response<SocialLoginResponse>

    /**
     * 로그인한 사용자의 등록된 프로젝트 목록을 조회합니다.
     */
    @GET("projects")
    suspend fun getProjects(): Response<List<ProjectData>>

    /**
     * 신규 뜨개 프로젝트를 생성합니다.
     */
    @POST("projects")
    suspend fun createProject(
        @Body project: ProjectData
    ): Response<ProjectData>

    /**
     * 특정 프로젝트의 현재 단수를 실시간으로 업데이트합니다.
     */
    @PATCH("projects/{id}/counter")
    suspend fun updateProjectCounter(
        @Path("id") projectId: Long,
        @Body request: ProjectCounterUpdateRequest
    ): Response<ProjectData>

    /**
     * 지정한 프로젝트를 삭제합니다.
     */
    @DELETE("projects/{id}")
    suspend fun deleteProject(
        @Path("id") projectId: Long
    ): Response<Unit>
}

/**
 * 소셜 로그인 요청 DTO입니다.
 *
 * @property token 소셜 인프라(카카오 등)로부터 전달받은 OIDC ID 토큰
 */
data class SocialLoginRequest(
    @SerializedName("token")
    val token: String
)

/**
 * 소셜 로그인 및 가입 요청 결과 응답 DTO입니다.
 *
 * 기존 회원의 경우 [accessToken] 및 [user] 정보가 반환되며,
 * 신규 회원([isNewUser] = true)인 경우 추가 프로필 설정을 위해 [signupToken]이 반환됩니다.
 */
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

/**
 * 인증 성공 후 반환되는 핵심 사용자 프로필 응답 DTO입니다.
 */
data class AuthUserResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nickname")
    val nickname: String
)

/**
 * 신규 회원 가입 최종 요청 DTO입니다.
 */
data class SignupRequest(
    @SerializedName("signup_token")
    val signupToken: String,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("terms_agreed")
    val termsAgreed: Boolean = true
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RefreshResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)