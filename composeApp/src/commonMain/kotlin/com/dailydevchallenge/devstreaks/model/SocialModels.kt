package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Social and collaborative features models
 */

/**
 * User social profile for community features
 */
@Serializable
data class SocialProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val level: Int = 1,
    val totalXp: Int = 0,
    val streak: Int = 0,
    val skillBadges: List<SkillBadge> = emptyList(),
    val achievements: List<SocialAchievement> = emptyList(),
    val joinedAt: Instant,
    val lastActiveAt: Instant,
    val isPublic: Boolean = true,
    val mentorStatus: MentorStatus = MentorStatus.LEARNER,
    val reputation: Int = 0
)

/**
 * Skill badges earned by users
 */
@Serializable
data class SkillBadge(
    val id: String,
    val skillName: String,
    val level: BadgeLevel,
    val earnedAt: Instant,
    val description: String
)

@Serializable
enum class BadgeLevel {
    BRONZE, SILVER, GOLD, PLATINUM
}

/**
 * User social achievements
 */
@Serializable
data class SocialAchievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val earnedAt: Instant,
    val rarity: AchievementRarity
)

@Serializable
enum class AchievementRarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
}

/**
 * Mentor status levels
 */
@Serializable
enum class MentorStatus {
    LEARNER,
    PEER_MENTOR,
    EXPERT_MENTOR,
    COMMUNITY_LEADER
}

/**
 * Leaderboard entry
 */
@Serializable
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val points: Int,
    val rank: Int,
    val change: RankChange = RankChange.SAME,
    val streak: Int = 0,
    val level: Int = 1,
    val lastActiveAt: Instant
)

@Serializable
enum class RankChange {
    UP, DOWN, SAME, NEW
}

/**
 * Different types of leaderboards
 */
@Serializable
enum class LeaderboardType {
    GLOBAL_XP,
    WEEKLY_XP,
    MONTHLY_XP,
    SKILL_SPECIFIC,
    PROJECT_COMPLETION,
    LEARNING_STREAK,
    PEER_REVIEW_SCORE,
    TOTAL_CHALLENGES_COMPLETED,
    STREAK_DAYS,
    PEER_REVIEWS_GIVEN,
    SKILL_BADGES_EARNED
}

/**
 * Collaborative challenge that users can work on together
 */
@Serializable
data class CollaborativeChallenge(
    val id: String,
    val title: String,
    val description: String,
    val category: CollaborationCategory,
    val difficulty: DifficultyLevel,
    val maxParticipants: Int,
    val currentParticipants: List<String> = emptyList(),
    val status: CollaborativeChallengeStatus,
    val createdBy: String,
    val createdAt: Instant,
    val startDate: Instant,
    val endDate: Instant,
    val requirements: List<String>,
    val deliverables: List<String>,
    val xpReward: Int,
    val estimatedDurationHours: Int,
    val theme: String,
    val teamFormationStrategy: TeamStrategy = TeamStrategy.OPEN_JOIN
)

@Serializable
enum class CollaborationCategory {
    CODE_REVIEW,
    PAIR_PROGRAMMING,
    TEAM_PROJECT,
    KNOWLEDGE_SHARING,
    MENTORING,
    STUDY_GROUP
}

@Serializable
enum class CollaborativeChallengeStatus {
    OPEN_FOR_REGISTRATION,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Serializable
enum class TeamStrategy {
    OPEN_JOIN,
    SKILL_MATCHED,
    RANDOM_ASSIGNMENT,
    INVITE_ONLY
}

/**
 * User participation in collaborative challenges
 */
@Serializable
data class CollaborationParticipation(
    val id: String,
    val userId: String,
    val challengeId: String,
    val role: ParticipantRole,
    val joinedAt: Instant,
    val contribution: String = "",
    val isActive: Boolean = true,
    val teamId: String? = null
)

@Serializable
enum class ParticipantRole {
    PARTICIPANT,
    MENTOR,
    TEAM_LEADER,
    ORGANIZER
}

/**
 * Peer review for code submissions
 */
@Serializable
data class CodeReview(
    val id: String,
    val submissionId: String,
    val submissionTitle: String = "",
    val reviewerId: String,
    val reviewerName: String = "",
    val submitterId: String = "",
    val codeSnippet: String? = null,
    val language: String? = null,
    val overallScore: Float = 0.0f, // 0.0 to 5.0
    val overallRating: ReviewRating = ReviewRating.GOOD,
    val feedback: String,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList(),
    val codeQualityScore: Float = 0.0f,
    val functionalityScore: Float = 0.0f,
    val styleScore: Float = 0.0f,
    val isHelpful: Boolean? = null, // Author's feedback
    val helpfulCount: Int = 0,
    val createdAt: Instant,
    val reviewedAt: Instant = createdAt,
    val generatedByLLM: Boolean = false,
    val structuredFeedback: StructuredFeedback? = null
)

@Serializable
enum class ReviewRating {
    EXCELLENT,
    GOOD,
    NEEDS_WORK,
    FAIR
}

/**
 * Structured feedback from LLM analysis
 */
@Serializable
data class StructuredFeedback(
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val bestPractices: List<String> = emptyList()
)

/**
 * Peer feedback summary used by social features
 */
@Serializable
data class PeerFeedback(
    val id: String,
    val submissionId: String,
    val reviewerId: String,
    val overallRating: Double,
    val strengths: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList(),
    val encouragement: String = "",
    val createdAt: Instant
)

/**
 * Community challenge for collaborative learning
 */
@Serializable
data class CommunityChallenge(
    val id: String,
    val title: String,
    val description: String,
    val trendingTopic: String = "",
    val difficulty: DifficultyLevel,
    val skillAreas: List<String> = emptyList(),
    val durationDays: Int = 7,
    val estimatedTimeMinutes: Int = 120,
    val participantCount: Int = 0,
    val overallProgress: Float = 0.0f,
    val teamProgress: List<TeamProgress> = emptyList(),
    val adaptiveTasks: List<AdaptiveTask> = emptyList(),
    val startDate: Instant,
    val endDate: Instant,
    val xpReward: Int = 100,
    val createdAt: Instant,
    val generatedByLLM: Boolean = false,
    val theme: String
)

/**
 * Team progress within a challenge
 */
@Serializable
data class TeamProgress(
    val teamId: String,
    val teamName: String,
    val progress: Float = 0.0f,
    val memberCount: Int = 0
)

/**
 * Adaptive task generated by LLM based on user weak areas
 */
@Serializable
data class AdaptiveTask(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val xpReward: Int = 0,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val skillArea: String = "",
    val estimatedMinutes: Int = 30
)

/**
 * Study group for collaborative learning
 */
@Serializable
data class StudyGroup(
    val id: String,
    val name: String,
    val description: String,
    val focusSkills: List<String>,
    val targetLevel: DifficultyLevel,
    val maxMembers: Int = 10,
    val memberIds: List<String> = emptyList(), // Changed from currentMembers
    val createdBy: String,
    val createdAt: Instant,
    val meetingSchedule: MeetingSchedule? = null,
    val isActive: Boolean = true
)

/**
 * Meeting schedule for study groups
 */
@Serializable
data class MeetingSchedule(
    val frequency: MeetingFrequency,
    val timeUtc: String, // HH:MM format
    val durationMinutes: Int = 60,
    val dayOfWeek: Int? = null // 1-7, Monday to Sunday
)

@Serializable
enum class MeetingFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    AD_HOC
}

/**
 * Social interaction between users
 */
@Serializable
data class SocialInteraction(
    val id: String,
    val fromUserId: String,
    val toUserId: String? = null,
    val targetId: String, // Can be challenge, review, etc.
    val targetType: InteractionTargetType,
    val type: InteractionType,
    val content: String? = null,
    val createdAt: Instant
)

@Serializable
enum class InteractionTargetType {
    USER,
    CHALLENGE,
    REVIEW,
    STUDY_GROUP,
    PROJECT
}

@Serializable
enum class InteractionType {
    LIKE,
    COMMENT,
    SHARE,
    KUDOS,
    FOLLOW,
    MENTION
}
