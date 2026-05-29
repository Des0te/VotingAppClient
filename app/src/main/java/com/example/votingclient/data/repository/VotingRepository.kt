package com.example.votingclient.data.repository

import com.example.votingclient.data.local.SessionStore
import com.example.votingclient.data.model.AuthResponse
import com.example.votingclient.data.model.CreatePollRequest
import com.example.votingclient.data.model.LoginRequest
import com.example.votingclient.data.model.PollResponse
import com.example.votingclient.data.model.RegisterRequest
import com.example.votingclient.data.model.ResultsResponse
import com.example.votingclient.data.model.VoteRequest
import com.example.votingclient.data.model.VoteResponse
import com.example.votingclient.data.network.ApiService
import org.json.JSONObject
import retrofit2.HttpException

class VotingRepository(
    private val api: ApiService,
    private val sessionStore: SessionStore,
) {
    suspend fun register(name: String, email: String, password: String): AuthResponse {
        return request { api.register(RegisterRequest(name, email, password)) }
            .also { sessionStore.save(it.token, it.user) }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        return request { api.login(LoginRequest(email, password)) }
            .also { sessionStore.save(it.token, it.user) }
    }

    suspend fun activePolls(): List<PollResponse> = request { api.activePolls() }

    suspend fun search(query: String): List<PollResponse> = request { api.search(query) }

    suspend fun poll(id: String): PollResponse = request { api.poll(id) }

    suspend fun createPoll(request: CreatePollRequest): PollResponse = request { api.createPoll(request) }

    suspend fun vote(id: String, optionIds: List<String>): VoteResponse = request { api.vote(id, VoteRequest(optionIds)) }

    suspend fun results(id: String): ResultsResponse = request { api.results(id) }

    private suspend fun <T> request(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val message = runCatching { JSONObject(body ?: "").getString("message") }.getOrNull()
            throw Exception(message ?: "Ошибка сервера: ${e.code()}")
        } catch (_: java.net.ConnectException) {
            throw Exception("Не удалось подключиться к серверу")
        } catch (_: java.net.SocketTimeoutException) {
            throw Exception("Сервер долго не отвечает")
        }
    }
}
