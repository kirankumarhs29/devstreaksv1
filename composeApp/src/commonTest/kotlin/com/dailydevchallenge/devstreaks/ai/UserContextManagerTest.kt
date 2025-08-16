package com.dailydevchallenge.devstreaks.ai

import com.dailydevchallenge.devstreaks.repository.*
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import io.mockk.*

class UserContextManagerTest {

    private lateinit var memoryRepository: MemoryRepository
    private lateinit var resumeAnalysisRepository: ResumeAnalysisRepository
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var challengeRepository: ChallengeRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var userStatsManager: UserStatsManager
    private lateinit var userContextManager: UserContextManager

    @BeforeTest
    fun setup() {
        memoryRepository = mockk()
        resumeAnalysisRepository = mockk()
        interviewRepository = mockk()
        challengeRepository = mockk()
        profileRepository = mockk()
        userStatsManager = mockk()

        userContextManager = UserContextManager(
            memoryRepository = memoryRepository,
            resumeAnalysisRepository = resumeAnalysisRepository,
            interviewRepository = interviewRepository,
            challengeRepository = challengeRepository,
            profileRepository = profileRepository,
            userStatsManager = userStatsManager
        )

        // Mock UserPreferences to return a test user ID
        mockkObject(com.dailydevchallenge.devstreaks.settings.UserPreferences)
        every { com.dailydevchallenge.devstreaks.settings.UserPreferences.getSafeUserId() } returns "test-user-123"
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getUserProfile returns complete unified profile`() = runTest {
        // Given
        val mockUserStats = UserStats(
            level = 5,
            totalXp = 1500,
            currentStreak = 7,
            skillsProgress = mapOf("JavaScript" to 80, "Python" to 60)
        )
        val mockProfile = mockk<UserProfile> {
            every { name } returns "Test User"
            every { email } returns "test@example.com"
            every { createdAt } returns Clock.System.now().toEpochMilliseconds()
        }
        val mockCompletedChallenges = listOf(
            CompletedChallenge("1", "JavaScript", true, 3, Clock.System.now().toEpochMilliseconds()),
            CompletedChallenge("2", "Python", true, 2, Clock.System.now().toEpochMilliseconds())
        )

        every { userStatsManager.getUserStats() } returns mockUserStats
        every { profileRepository.getUserProfile("test-user-123") } returns mockProfile
        every { challengeRepository.getCompletedChallenges("test-user-123") } returns mockCompletedChallenges
        every { resumeAnalysisRepository.getAllForUser("test-user-123") } returns emptyList()
        every { interviewRepository.getSessionsForUser("test-user-123") } returns emptyList()
        every { memoryRepository.getAllConversations() } returns emptyList()
        every { memoryRepository.getRecentMemory(any()) } returns emptyList()

        // When
        val profile = userContextManager.getUserProfile()

        // Then
        assertNotNull(profile)
        assertEquals("Test User", profile.basicInfo.displayName)
        assertEquals("test@example.com", profile.basicInfo.email)
        assertEquals(5, profile.learningProgress.currentLevel)
        assertEquals(1500, profile.learningProgress.totalXp)
        assertEquals(7, profile.learningProgress.currentStreak)
        assertEquals(2, profile.learningProgress.challengesCompleted)
        assertTrue(profile.lastUpdated > 0)
    }

    @Test
    fun `getResumeContext returns null when no analyses exist`() = runTest {
        // Given
        every { resumeAnalysisRepository.getAllForUser("test-user-123") } returns emptyList()

        // When
        val context = userContextManager.getResumeContext()

        // Then
        assertNull(context)
    }

    @Test
    fun `getResumeContext returns valid context with analyses`() = runTest {
        // Given
        val mockAnalysis = mockk<com.dailydevchallenge.database.ResumeAnalysis> {
            every { toModel() } returns ResumeAnalysis(
                id = "analysis-1",
                userId = "test-user-123",
                resumeText = "Sample resume",
                summary = "Software Developer",
                skillsParsed = listOf("JavaScript", "Python", "React"),
                skillsGaps = listOf("System Design"),
                recommendations = "Focus on backend development",
                jobMatchScore = 85,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            every { recommendations } returns "Focus on senior developer roles"
            every { skillsParsed } returns "JavaScript,Python,React"
            every { skillsGaps } returns "System Design,Algorithms"
        }

        every { resumeAnalysisRepository.getAllForUser("test-user-123") } returns listOf(mockAnalysis)

        // When
        val context = userContextManager.getResumeContext()

        // Then
        assertNotNull(context)
        assertEquals("analysis-1", context.latestAnalysis.id)
        assertEquals("Software Developer", context.careerInsights.targetRole)
        assertTrue(context.skillStrengths.contains("JavaScript"))
        assertTrue(context.improvementAreas.contains("System Design"))
    }

    @Test
    fun `getChallengeProgress returns comprehensive progress data`() = runTest {
        // Given
        val mockUserStats = UserStats(level = 3, totalXp = 750, currentStreak = 5, skillsProgress = emptyMap())
        val mockChallenges = listOf(
            CompletedChallenge("1", "JavaScript", true, 3, Clock.System.now().toEpochMilliseconds()),
            CompletedChallenge("2", "JavaScript", true, 4, Clock.System.now().toEpochMilliseconds()),
            CompletedChallenge("3", "Python", false, 2, Clock.System.now().toEpochMilliseconds())
        )

        every { userStatsManager.getUserStats() } returns mockUserStats
        every { challengeRepository.getCompletedChallenges("test-user-123") } returns mockChallenges

        // When
        val progress = userContextManager.getChallengeProgress()

        // Then
        assertEquals(3, progress.totalCompleted)
        assertEquals(5, progress.currentStreak)
        assertEquals(750, progress.totalXp)
        assertEquals(3, progress.level)
        assertTrue(progress.skillBreakdown.containsKey("JavaScript"))
        assertTrue(progress.skillBreakdown.containsKey("Python"))
        assertEquals(100, progress.skillBreakdown["JavaScript"]) // 2/2 correct
        assertEquals(0, progress.skillBreakdown["Python"]) // 0/1 correct
    }

    @Test
    fun `getConversationMemory extracts user preferences correctly`() = runTest {
        // Given
        val mockConversations = listOf(
            Conversation("I need visual examples to understand", "Sure! Let me show you..."),
            Conversation("Can you explain with diagrams?", "Here's a visual representation..."),
            Conversation("I prefer hands-on practice", "Let's code together...")
        )

        every { memoryRepository.getAllConversations() } returns mockConversations
        every { memoryRepository.getRecentMemory(10) } returns emptyList()

        // When
        val memory = userContextManager.getConversationMemory()

        // Then
        assertEquals(3, memory.conversationCount)
        assertTrue(memory.userPreferences.contains("Visual learning"))
        assertTrue(memory.userPreferences.contains("Hands-on learning"))
    }

    @Test
    fun `buildContextualPrompt includes all relevant context for DevCoach`() = runTest {
        // Given
        setupMockDataForContextualPrompt()

        // When
        val prompt = userContextManager.buildContextualPrompt(
            feature = AIFeature.DEVCOACH_CHAT,
            basePrompt = "Help me improve my JavaScript skills"
        )

        // Then
        assertTrue(prompt.contains("=== USER PROFILE ==="))
        assertTrue(prompt.contains("=== DEVCOACH CONTEXT ==="))
        assertTrue(prompt.contains("=== USER REQUEST ==="))
        assertTrue(prompt.contains("Help me improve my JavaScript skills"))
        assertTrue(prompt.contains("Level:"))
        assertTrue(prompt.contains("Streak:"))
    }

    @Test
    fun `buildContextualPrompt includes resume analysis context for resume feature`() = runTest {
        // Given
        setupMockDataForContextualPrompt()

        // When
        val prompt = userContextManager.buildContextualPrompt(
            feature = AIFeature.RESUME_ANALYSIS,
            basePrompt = "Analyze my resume for a senior role"
        )

        // Then
        assertTrue(prompt.contains("=== RESUME ANALYSIS CONTEXT ==="))
        assertTrue(prompt.contains("Verified skills from challenges:"))
        assertTrue(prompt.contains("Skill proficiency levels:"))
    }

    @Test
    fun `buildContextualPrompt includes interview context for mock interview`() = runTest {
        // Given
        setupMockDataForContextualPrompt()

        // When
        val prompt = userContextManager.buildContextualPrompt(
            feature = AIFeature.MOCK_INTERVIEW,
            basePrompt = "Start a technical interview"
        )

        // Then
        assertTrue(prompt.contains("=== MOCK INTERVIEW CONTEXT ==="))
        assertTrue(prompt.contains("Technical strengths:"))
        assertTrue(prompt.contains("Areas needing practice:"))
    }

    @Test
    fun `buildContextualPrompt includes challenge feedback context`() = runTest {
        // Given
        setupMockDataForContextualPrompt()

        // When
        val prompt = userContextManager.buildContextualPrompt(
            feature = AIFeature.CHALLENGE_FEEDBACK,
            basePrompt = "Provide feedback on my solution"
        )

        // Then
        assertTrue(prompt.contains("=== CHALLENGE FEEDBACK CONTEXT ==="))
        assertTrue(prompt.contains("Current level:"))
        assertTrue(prompt.contains("Recent performance trend:"))
        assertTrue(prompt.contains("Learning preferences:"))
    }

    @Test
    fun `context remains consistent across multiple calls`() = runTest {
        // Given
        setupMockDataForContextualPrompt()

        // When
        val profile1 = userContextManager.getUserProfile()
        val profile2 = userContextManager.getUserProfile()
        val progress1 = userContextManager.getChallengeProgress()
        val progress2 = userContextManager.getChallengeProgress()

        // Then
        assertEquals(profile1.basicInfo.userId, profile2.basicInfo.userId)
        assertEquals(profile1.learningProgress.currentLevel, profile2.learningProgress.currentLevel)
        assertEquals(progress1.totalCompleted, progress2.totalCompleted)
        assertEquals(progress1.currentStreak, progress2.currentStreak)
    }

    @Test
    fun `skill gap identification works correctly`() = runTest {
        // Given
        val mockUserStats = UserStats(level = 2, totalXp = 500, currentStreak = 3, skillsProgress = emptyMap())
        val mockChallenges = listOf(
            CompletedChallenge("1", "JavaScript", false, 2, Clock.System.now().toEpochMilliseconds()) // Low mastery
        )
        val mockAnalysis = mockk<com.dailydevchallenge.database.ResumeAnalysis> {
            every { skillsParsed } returns "React,Node.js" // Skills not practiced in challenges
            every { skillsGaps } returns ""
            every { recommendations } returns ""
            every { toModel() } returns mockk()
        }

        every { userStatsManager.getUserStats() } returns mockUserStats
        every { challengeRepository.getCompletedChallenges("test-user-123") } returns mockChallenges
        every { resumeAnalysisRepository.getAllForUser("test-user-123") } returns listOf(mockAnalysis)
        every { interviewRepository.getSessionsForUser("test-user-123") } returns emptyList()
        every { memoryRepository.getAllConversations() } returns emptyList()
        every { memoryRepository.getRecentMemory(any()) } returns emptyList()
        every { profileRepository.getUserProfile("test-user-123") } returns null

        // When
        val profile = userContextManager.getUserProfile()

        // Then
        val skillGaps = profile.skillAssessment.skillGaps
        assertTrue(skillGaps.any { it.contains("React") && it.contains("Needs practical validation") })
        assertTrue(skillGaps.any { it.contains("JavaScript") && it.contains("Low proficiency") })
        assertTrue(skillGaps.any { it.contains("Python - Not demonstrated") })
    }

    private fun setupMockDataForContextualPrompt() {
        val mockUserStats = UserStats(level = 4, totalXp = 1200, currentStreak = 10, skillsProgress = emptyMap())
        val mockProfile = mockk<UserProfile> {
            every { name } returns "Test User"
            every { email } returns "test@example.com"
            every { createdAt } returns Clock.System.now().toEpochMilliseconds()
        }
        val mockChallenges = listOf(
            CompletedChallenge("1", "JavaScript", true, 4, Clock.System.now().toEpochMilliseconds()),
            CompletedChallenge("2", "Python", true, 3, Clock.System.now().toEpochMilliseconds())
        )
        val mockConversations = listOf(
            Conversation("Help with algorithms", "Sure, let's work on algorithms"),
            Conversation("I prefer detailed explanations", "I'll provide detailed explanations")
        )

        every { userStatsManager.getUserStats() } returns mockUserStats
        every { profileRepository.getUserProfile("test-user-123") } returns mockProfile
        every { challengeRepository.getCompletedChallenges("test-user-123") } returns mockChallenges
        every { resumeAnalysisRepository.getAllForUser("test-user-123") } returns emptyList()
        every { interviewRepository.getSessionsForUser("test-user-123") } returns emptyList()
        every { memoryRepository.getAllConversations() } returns mockConversations
        every { memoryRepository.getRecentMemory(any()) } returns emptyList()
    }
}

// Mock data classes for testing
data class UserStats(
    val level: Int,
    val totalXp: Int,
    val currentStreak: Int,
    val skillsProgress: Map<String, Int>
)

data class UserProfile(
    val name: String,
    val email: String,
    val createdAt: Long
)

data class CompletedChallenge(
    val id: String,
    val topic: String,
    val isCorrect: Boolean,
    val difficulty: Int,
    val completedAt: Long
)
