package com.example.votingclient.data.network

import com.example.votingclient.data.model.AuthResponse
import com.example.votingclient.data.model.CreatePollRequest
import com.example.votingclient.data.model.LoginRequest
import com.example.votingclient.data.model.PollResponse
import com.example.votingclient.data.model.RegisterRequest
import com.example.votingclient.data.model.ResultsResponse
import com.example.votingclient.data.model.VoteRequest
import com.example.votingclient.data.model.VoteResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("polls/active")
    suspend fun activePolls(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<PollResponse>

    @GET("polls/{id}")
    suspend fun poll(@Path("id") id: String): PollResponse

    @POST("polls")
    suspend fun createPoll(@Body request: CreatePollRequest): PollResponse

    @POST("polls/{id}/vote")
    suspend fun vote(
        @Path("id") id: String,
        @Body request: VoteRequest,
    ): VoteResponse

    @GET("polls/{id}/results")
    suspend fun results(@Path("id") id: String): ResultsResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<PollResponse>
}
