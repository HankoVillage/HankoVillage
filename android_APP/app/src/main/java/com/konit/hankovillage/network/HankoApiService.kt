package com.konit.hankovillage.network

import com.google.gson.annotations.SerializedName
import com.konit.hankovillage.ui.counter.ProjectCounterUpdateRequest
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.pattern.ImageUploadResponse
import com.konit.hankovillage.ui.pattern.PatternCreateRequest
import com.konit.hankovillage.ui.pattern.PatternData
import com.konit.hankovillage.ui.pattern.PatternUpdateRequest
import com.konit.hankovillage.ui.yarn.YarnCreateRequest
import com.konit.hankovillage.ui.yarn.YarnData
import com.konit.hankovillage.ui.yarn.YarnUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 한코마을 백엔드 RESTful API 통신 인터페이스입니다.
 */
interface HankoApiService {

    /**
     * 소셜 재인증 ID 토큰을 검증하여 회원 탈퇴를 처리합니다.
     */
    @POST("users/me/withdrawal")
    suspend fun withdrawUser(
        @Body request: SocialLoginRequest
    ): Response<Unit>

    /**
     * 만료된 액세스 토큰을 리프레시 토큰으로 갱신합니다.
     */
    @POST("auth/refresh")
    suspend fun refreshTokens(
        @Body request: RefreshRequest
    ): Response<RefreshResponse>

    /**
     * 서버 측 리프레시 토큰을 만료시켜 로그아웃을 처리합니다.
     */
    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Response<Unit>

    /**
     * 구글 인증 토큰(ID Token)을 전달하여 소셜 로그인 또는 가입 필요 여부를 확인합니다.
     */
    @POST("auth/social/google")
    suspend fun socialLoginGoogle(
        @Body request: SocialLoginRequest
    ): Response<SocialLoginResponse>

    /**
     * 소셜 임시 가입 토큰과 사용자 입력 프로필 정보를 전달하여 최종 회원가입을 완료합니다.
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

    /**
     * 현재 로그인된 사용자의 기본 프로필 정보를 조회합니다.
     */
    @GET("users/me")
    suspend fun getMyProfile(): Response<AuthUserResponse>

    // 1. 실 목록 조회 (최신순: NEWEST, 이름순: NAME)
    @GET("yarns")
    suspend fun getYarns(
        @Query("sort") sort: String = "NEWEST",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<YarnData>>

    // 2. 새 실 등록 (텍스트 정보)
    @POST("yarns")
    suspend fun createYarn(@Body request: YarnCreateRequest): Response<YarnData>

    // 3. 실 상세 조회
    @GET("yarns/{id}")
    suspend fun getYarnDetail(@Path("id") yarnId: Long): Response<YarnData>

    // 4. 실 정보 수정
    @PATCH("yarns/{id}")
    suspend fun updateYarn(
        @Path("id") yarnId: Long,
        @Body request: YarnUpdateRequest
    ): Response<YarnData>

    // 5. 실 삭제
    @DELETE("yarns/{id}")
    suspend fun deleteYarn(@Path("id") yarnId: Long): Response<Unit>

    // 6. 라벨 사진 업로드/교체 (800x800 JPEG Multipart)
    @Multipart
    @PUT("yarns/{id}/image")
    suspend fun uploadYarnImage(
        @Path("id") yarnId: Long,
        @Part file: MultipartBody.Part
    ): Response<ImageUploadResponse>

    // 7. 라벨 사진만 삭제
    @DELETE("yarns/{id}/image")
    suspend fun deleteYarnImage(@Path("id") yarnId: Long): Response<Unit>

    /**
     * 검색어, 태그, 진행 상태 및 정렬 기준에 따라 도안 목록을 조회합니다.
     */
    @GET("patterns")
    suspend fun getPatterns(
        @Query("search") search: String? = null,
        @Query("tag") tag: String? = null,
        @Query("status") status: String? = null,
        @Query("sort") sort: String = "NEWEST",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<PatternData>>

    /**
     * 신규 도안 정보를 등록합니다.
     */
    @POST("patterns")
    suspend fun createPattern(
        @Body request: PatternCreateRequest
    ): Response<PatternData>

    /**
     * 기존 도안의 상세 정보를 수정합니다.
     */
    @PATCH("patterns/{pattern_id}")
    suspend fun updatePattern(
        @Path("pattern_id") patternId: Long,
        @Body request: PatternUpdateRequest
    ): Response<PatternData>

    /**
     * 지정한 도안을 삭제합니다.
     */
    @DELETE("patterns/{pattern_id}")
    suspend fun deletePattern(
        @Path("pattern_id") patternId: Long
    ): Response<Unit>

    /**
     * 특정 도안의 대표 이미지를 업로드하거나 교체합니다.
     */
    @Multipart
    @PUT("patterns/{pattern_id}/image")
    suspend fun uploadPatternImage(
        @Path("pattern_id") patternId: Long,
        @Part file: MultipartBody.Part
    ): Response<ImageUploadResponse>

    /**
     * 특정 도안에 등록된 이미지를 삭제합니다.
     */
    @DELETE("patterns/{pattern_id}/image")
    suspend fun deletePatternImage(
        @Path("pattern_id") patternId: Long
    ): Response<Unit>
}

/**
 * 소셜 로그인 및 재인증 요청 DTO입니다.
 *
 * @property token 소셜 인프라(카카오, 구글 등)로부터 전달받은 OIDC ID 토큰
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
    val termsAgreed: Boolean = true,

    @SerializedName("age_over_14")
    val ageOver14: Boolean
)

/**
 * 액세스 토큰 갱신 및 로그아웃 요청 DTO입니다.
 */
data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * 토큰 갱신 성공 시 반환되는 신규 토큰 쌍 응답 DTO입니다.
 */
data class RefreshResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)