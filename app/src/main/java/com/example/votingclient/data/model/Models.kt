package com.example.votingclient.data.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val token: String,
    val user: UserResponse,
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
)

data class CreatePollRequest(
    val question: String,
    val options: List<String>,
    val startsAt: String,
    val endsAt: String,
    val choiceType: String,
    val anonymous: Boolean,
    val maxChoices: Int,
)

data class VoteRequest(
    val optionIds: List<String>,
)

data class VoteResponse(
    val message: String,
    val results: ResultsResponse?,
)

data class PollResponse(
    val id: String,
    val question: String,
    val startsAt: String,
    val endsAt: String,
    val choiceType: String,
    val anonymous: Boolean,
    val authorId: String,
    val maxChoices: Int,
    val options: List<OptionResponse>,
)

data class OptionResponse(
    val id: String,
    val text: String,
)

data class ResultsResponse(
    val pollId: String,
    val totalVoters: Int,
    val options: List<ResultOptionResponse>,
)

data class ResultOptionResponse(
    val optionId: String,
    val text: String,
    val votes: Int,
    val percent: Double,
)
