package com.jobtracker.presentation.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.BuildConfig
import com.jobtracker.data.remote.api.OpenAiMessage
import com.jobtracker.data.remote.api.OpenAiRequest
import com.jobtracker.data.remote.api.OpenAiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatbotUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            role = "assistant",
            content = "Hi! 👋 I'm your JobTracker assistant. I can help you:\n\n" +
                "• **Add a new job application** – tap the + button on the dashboard\n" +
                "• **Track your status** – update jobs from Applied → Interview → Offer\n" +
                "• **Analyse your CV** – open a job and tap ⋮ → AI CV Analysis\n" +
                "• **View your stats** – check the Analytics screen\n\n" +
                "What would you like help with?"
        )
    ),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val openAiService: OpenAiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    private val systemPrompt = """You are a friendly, concise in-app assistant for JobTracker — an Android app that helps users manage their job applications.

The app has these screens and features:
- Dashboard: overview of stats (total, interviews, offers), recent applications, quick actions
- Job List: all applications with search and filter by status (Applied, Interview, Offer, Rejected, Withdrawn, Saved)
- Add/Edit Job: form to add company name, job title, location, salary, job URL, status, job description, contact details, notes
- AI CV Analysis: upload your CV PDF or paste CV text to get a match score, present skills, missing skills, and suggestions against a job description
- Analytics: bar chart of weekly applications, pie chart of status distribution, success rate
- Chatbot (this screen): help for users

Navigation: tap the + FAB to add a job. Long-press a job card to delete. Tap a job card to see options (Edit, AI Analysis, Delete).

Your job:
- Answer questions about how to use the app clearly and briefly
- Guide new users step by step
- If someone asks about something outside the app, say you only know about JobTracker
- Use bullet points for steps
- Be encouraging and friendly
- Keep responses under 150 words unless a step-by-step guide is truly needed"""

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isTyping) return

        val userMsg = ChatMessage(role = "user", content = text)
        val currentMessages = _uiState.value.messages + userMsg

        _uiState.value = _uiState.value.copy(
            messages = currentMessages,
            inputText = "",
            isTyping = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val apiMessages = mutableListOf(OpenAiMessage("system", systemPrompt))
                // Include last 10 messages for context
                currentMessages.takeLast(10).forEach {
                    apiMessages.add(OpenAiMessage(it.role, it.content))
                }

                val response = openAiService.chat(
                    auth = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                    request = OpenAiRequest(
                        model = "gpt-4o-mini",
                        messages = apiMessages,
                        max_tokens = 300,
                        temperature = 0.7
                    )
                )

                val reply = response.choices.firstOrNull()?.message?.content
                    ?: "Sorry, I didn't get a response. Please try again."

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(role = "assistant", content = reply),
                    isTyping = false
                )
            } catch (e: Exception) {
                // Fallback to rule-based responses if OpenAI fails
                val fallback = getRuleBasedResponse(text)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(role = "assistant", content = fallback),
                    isTyping = false
                )
            }
        }
    }

    private fun getRuleBasedResponse(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("add") && (lower.contains("job") || lower.contains("application")) ->
                "To add a job application:\n1. Tap the **+** button on the Dashboard or Job List screen\n2. Fill in the Company Name and Job Title (required)\n3. Add the job description — this is needed for AI CV analysis\n4. Set the status (Applied, Interview, etc.)\n5. Tap **Save Application**"

            lower.contains("delete") || lower.contains("remove") ->
                "To delete a job:\n• **Long-press** the job card, or\n• **Tap** the job card → select Delete from the menu\n\nYou'll be asked to confirm before it's deleted."

            lower.contains("cv") || lower.contains("resume") || lower.contains("analys") ->
                "To analyse your CV:\n1. Make sure the job has a **job description** saved\n2. Go to Job List → tap the job → select **AI CV Analysis**\n3. Upload your CV as a PDF or paste the text\n4. Tap **Analyse My CV**\n\nYou'll get a match score, present skills, missing skills, and suggestions."

            lower.contains("status") || lower.contains("update") ->
                "To update a job's status:\n1. Tap the job card on Job List\n2. Select **Edit Application**\n3. Change the Status dropdown\n4. Tap **Update Application**\n\nStatuses: Saved → Applied → Interview → Offer / Rejected / Withdrawn"

            lower.contains("search") || lower.contains("find") || lower.contains("filter") ->
                "To search or filter:\n• **Search bar** at the top of Job List — type company name or job title\n• **Filter chips** below the search bar — tap a status to filter by it\n• Tap **All** to clear the filter"

            lower.contains("analytics") || lower.contains("chart") || lower.contains("stats") ->
                "The Analytics screen shows:\n• **Success rate** — percentage of applications that reached interview or offer\n• **Weekly bar chart** — how many you applied each week\n• **Status pie chart** — breakdown of all your applications\n\nReach it from the Dashboard → Analytics quick action."

            lower.contains("sign out") || lower.contains("logout") || lower.contains("log out") ->
                "To sign out:\n• Tap the **logout icon** (→) in the top-right corner of the Dashboard"

            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hi there! 👋 I'm here to help you get the most out of JobTracker. What would you like to do — add a job, check your stats, or analyse your CV?"

            else ->
                "I'm not sure about that specific question, but here's what I can help with:\n\n• Adding or editing job applications\n• Deleting jobs\n• AI CV analysis\n• Updating job status\n• Using search and filters\n• Understanding your analytics\n\nWhat would you like help with?"
        }
    }
}
