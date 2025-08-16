# DevStreaks Adaptive Intelligence Integration - Testing Guide

## Overview
This guide validates the integration of Phase 2 features: Dynamic Difficulty Adjustment, Personalized AI Coaching, Weak Area Detection, and Adaptive XP Scaling.

## Pre-Testing Setup

### 1. Database Schema Updates Required
Add these tables to your SQLite schema:

```sql
-- Performance metrics tracking
CREATE TABLE IF NOT EXISTS performance_metrics (
    id TEXT PRIMARY KEY,
    userId TEXT NOT NULL,
    taskId TEXT NOT NULL,
    startTime INTEGER NOT NULL,
    endTime INTEGER NOT NULL,
    durationMillis INTEGER NOT NULL,
    attempts INTEGER NOT NULL,
    hintsUsed INTEGER NOT NULL,
    completed INTEGER NOT NULL, -- 0 or 1
    errorCount INTEGER NOT NULL,
    difficultyLevel INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);

-- Challenge completion details
CREATE TABLE IF NOT EXISTS challenge_completion (
    id TEXT PRIMARY KEY,
    userId TEXT NOT NULL,
    taskId TEXT NOT NULL,
    completedAt INTEGER NOT NULL,
    timeSpent INTEGER NOT NULL,
    attempts INTEGER NOT NULL,
    hintsUsed INTEGER NOT NULL
);

-- Weak area tracking
CREATE TABLE IF NOT EXISTS weak_areas (
    id TEXT PRIMARY KEY,
    userId TEXT NOT NULL,
    skillArea TEXT NOT NULL,
    severityScore REAL NOT NULL,
    detectedAt INTEGER NOT NULL,
    lastUpdated INTEGER NOT NULL,
    improvementTrend REAL DEFAULT 0.0
);
```

### 2. Required SQL Queries to Add
Add these queries to your `ChallengePathQueries.sq` file:

```sql
-- Performance metrics queries
insertPerformanceMetrics:
INSERT INTO performance_metrics VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

selectRecentPerformanceMetrics:
SELECT * FROM performance_metrics 
WHERE userId = ? 
ORDER BY timestamp DESC 
LIMIT ?;

-- Challenge completion queries
insertChallengeCompletion:
INSERT INTO challenge_completion VALUES (?, ?, ?, ?, ?, ?, ?);

-- Task selection queries
selectAllTasks:
SELECT * FROM challenge_tasks;

selectTasksForDifficulty:
SELECT * FROM challenge_tasks WHERE xp BETWEEN ? AND ?;
```

## Testing Scenarios

### 1. Adaptive Difficulty Adjustment Testing

#### Test Case 1: New User - Default Difficulty
```kotlin
// Expected: User starts at MEDIUM difficulty
@Test
fun testNewUserDefaultDifficulty() {
    val userId = "test_user_new"
    val config = adaptiveOrchestrator.getAdaptiveChallengeConfig(userId)
    assertEquals(DifficultyLevel.MEDIUM, config.recommendedDifficulty)
    assertTrue(config.xpMultiplier >= 1.0)
}
```

#### Test Case 2: High Performance - Difficulty Increase
```kotlin
// Simulate successful challenge completions
@Test
fun testHighPerformanceDifficultyIncrease() {
    val userId = "test_user_high_perf"
    
    // Simulate 5 successful completions with minimal attempts
    repeat(5) {
        val metrics = PerformanceMetrics(
            id = generateUUID(),
            userId = userId,
            taskId = "task_$it",
            startTime = Clock.System.now().toEpochMilliseconds() - 60000,
            endTime = Clock.System.now().toEpochMilliseconds(),
            durationMillis = 60000,
            attempts = 1,
            hintsUsed = 0,
            completed = true,
            errorCount = 0,
            difficultyLevel = DifficultyLevel.MEDIUM
        )
        repository.savePerformanceMetrics(metrics)
    }
    
    val config = adaptiveOrchestrator.getAdaptiveChallengeConfig(userId)
    assertTrue(config.recommendedDifficulty.value > DifficultyLevel.MEDIUM.value)
}
```

#### Test Case 3: Poor Performance - Difficulty Decrease
```kotlin
@Test
fun testPoorPerformanceDifficultyDecrease() {
    val userId = "test_user_struggling"
    
    // Simulate struggling with challenges
    repeat(3) {
        val metrics = PerformanceMetrics(
            id = generateUUID(),
            userId = userId,
            taskId = "task_$it",
            startTime = Clock.System.now().toEpochMilliseconds() - 600000,
            endTime = Clock.System.now().toEpochMilliseconds(),
            durationMillis = 600000,
            attempts = 6,
            hintsUsed = 4,
            completed = false,
            errorCount = 8,
            difficultyLevel = DifficultyLevel.MEDIUM
        )
        repository.savePerformanceMetrics(metrics)
    }
    
    val config = adaptiveOrchestrator.getAdaptiveChallengeConfig(userId)
    assertTrue(config.recommendedDifficulty.value < DifficultyLevel.MEDIUM.value)
}
```

### 2. Real-Time Weak Area Detection Testing

#### Test Case 4: Immediate Struggle Detection
```kotlin
@Test
fun testImmediateStruggleDetection() {
    val userId = "test_user_struggle"
    val taskId = "test_task"
    
    val metrics = PerformanceMetrics(
        id = generateUUID(),
        userId = userId,
        taskId = taskId,
        startTime = Clock.System.now().toEpochMilliseconds() - 300000,
        endTime = Clock.System.now().toEpochMilliseconds(),
        durationMillis = 300000,
        attempts = 4, // Above struggle threshold
        hintsUsed = 2,
        completed = false,
        errorCount = 3,
        difficultyLevel = DifficultyLevel.MEDIUM
    )
    
    val insights = realTimeMonitoringService.processRealTimePerformance(userId, taskId, metrics)
    assertTrue(insights.any { it.pattern == "IMMEDIATE_STRUGGLE" })
}
```

#### Test Case 5: Time Pressure Detection
```kotlin
@Test
fun testTimePressureDetection() {
    val userId = "test_user_time_pressure"
    val taskId = "test_task"
    
    val metrics = PerformanceMetrics(
        id = generateUUID(),
        userId = userId,
        taskId = taskId,
        startTime = Clock.System.now().toEpochMilliseconds() - 700000, // 11+ minutes
        endTime = Clock.System.now().toEpochMilliseconds(),
        durationMillis = 700000,
        attempts = 3,
        hintsUsed = 1,
        completed = false,
        errorCount = 2,
        difficultyLevel = DifficultyLevel.MEDIUM
    )
    
    val insights = realTimeMonitoringService.processRealTimePerformance(userId, taskId, metrics)
    assertTrue(insights.any { it.pattern == "TIME_PRESSURE" })
}
```

### 3. Personalized AI Coaching Testing

#### Test Case 6: Context-Aware Coaching
```kotlin
@Test
fun testContextAwareCoaching() {
    val userId = "test_user_coaching"
    val learningProfile = LearningProfile(
        primarySkill = "Kotlin",
        experience = "Beginner",
        goals = listOf("Learn Android Development")
    )
    
    // Setup weak areas
    val weakAreas = listOf(
        WeakArea(
            id = generateUUID(),
            userId = userId,
            skillArea = "Algorithms",
            severityScore = 0.8,
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
    )
    
    val response = personalizedCoachingService.generatePersonalizedResponse(
        userMessage = "I'm struggling with this challenge",
        userId = userId,
        learningProfile = learningProfile
    )
    
    assertTrue(response.response.isNotEmpty())
    assertTrue(response.recommendations.isNotEmpty())
    assertNotNull(response.motivationalBoost)
}
```

### 4. Adaptive XP Scaling Testing

#### Test Case 7: XP Multiplier Calculation
```kotlin
@Test
fun testAdaptiveXPScaling() {
    val userId = "test_user_xp"
    
    // Setup challenging scenario (working on weak areas)
    val config = AdaptiveChallengeConfig(
        userId = userId,
        recommendedDifficulty = DifficultyLevel.HARD,
        weakAreas = listOf(/* weak areas */),
        focusAreas = listOf("Algorithms"),
        xpMultiplier = 1.7, // Expected for hard difficulty + weak area work
        // ... other fields
    )
    
    val baseXP = 100
    val expectedXP = (baseXP * config.xpMultiplier).toInt()
    assertEquals(170, expectedXP)
}
```

### 5. Integration Testing - Full Adaptive Flow

#### Test Case 8: Complete Adaptive Challenge Flow
```kotlin
@Test
fun testCompleteAdaptiveFlow() {
    val userId = "test_user_complete"
    val viewModel = AdaptiveChallengeViewModel(
        adaptiveOrchestrator, repository, profilePreferences
    )
    
    // Initialize adaptive challenge
    viewModel.initializeAdaptiveChallenge(userId, "Kotlin", DifficultyLevel.MEDIUM)
    
    // Verify challenge loaded
    assertNotNull(viewModel.currentChallenge.value)
    assertNotNull(viewModel.adaptiveConfig.value)
    
    // Simulate attempt
    viewModel.processAttempt(userId, "test answer", false)
    
    // Verify real-time response generated
    assertNotNull(viewModel.realTimeResponse.value)
    assertTrue(viewModel.currentAttempts.value > 0)
    
    // Simulate successful completion
    viewModel.processAttempt(userId, "correct answer", true)
    
    // Verify completion handling
    assertTrue(viewModel.earnedXP.value > 0)
}
```

## Manual Testing Scenarios

### Scenario 1: New User Onboarding
1. Create a new user account
2. Complete onboarding with skill preferences
3. Navigate to adaptive challenge screen
4. **Expected**: Medium difficulty challenge with introductory guidance

### Scenario 2: Struggling User Experience
1. Use existing user account
2. Intentionally fail challenges 3-4 times each
3. Use multiple hints per challenge
4. **Expected**: 
   - Difficulty automatically decreases
   - Encouraging coaching messages appear
   - Weak areas detected and highlighted
   - Adaptive hints become more detailed

### Scenario 3: High Performer Experience
1. Complete challenges quickly with minimal attempts
2. Solve 5+ challenges successfully
3. **Expected**:
   - Difficulty increases automatically
   - XP multipliers increase
   - Challenging coaching messages
   - Advanced recommendations

### Scenario 4: Real-Time Adaptation
1. Start a challenge and make 3+ attempts without success
2. **Expected**: Real-time insights appear with helpful hints
3. Spend 10+ minutes on one challenge
4. **Expected**: Time pressure patterns detected with guidance

### Scenario 5: Personalized Coaching
1. Click "Ask AI Coach" button
2. Type specific questions about challenges
3. **Expected**: Context-aware responses mentioning your weak areas
4. Test different question types (technical, motivational, strategic)

## Performance Testing

### Memory Usage
- Monitor memory usage during adaptive calculations
- Ensure smooth UI performance with real-time updates
- Test with multiple concurrent users

### Response Times
- Adaptive configuration loading: < 2 seconds
- Real-time insight generation: < 1 second
- Personalized coaching responses: < 3 seconds

## UI/UX Testing

### Adaptive Challenge Screen
1. **Difficulty Indicators**: Verify color-coded difficulty chips
2. **XP Multipliers**: Confirm bonus indicators display correctly
3. **Real-time Feedback**: Test insight panels expand/collapse
4. **Weak Area Highlights**: Verify color coding for severity levels
5. **Motivational Messages**: Confirm appropriate timing and content

### HomeViewModel Integration
1. **Adaptive Insights**: Check insights appear on home screen
2. **Task Adaptation**: Verify today's tasks adjust to user level
3. **Coaching Integration**: Test dev coach insights are personalized

## Error Handling Testing

### Database Errors
```kotlin
@Test
fun testDatabaseErrorHandling() {
    // Simulate database unavailability
    // Verify graceful fallback to default behavior
}
```

### Network Errors
```kotlin
@Test
fun testNetworkErrorHandling() {
    // Simulate network failures during coaching requests
    // Verify offline capabilities
}
```

## Success Criteria

### ✅ Adaptive Difficulty
- [ ] New users start at appropriate difficulty
- [ ] Difficulty adjusts based on performance (3+ data points)
- [ ] Smooth transitions between difficulty levels
- [ ] No abrupt difficulty changes

### ✅ Real-Time Monitoring
- [ ] Struggle patterns detected within 3 attempts
- [ ] Time pressure identified after 10 minutes
- [ ] Immediate feedback appears within 1 second
- [ ] Insights are contextually relevant

### ✅ Personalized Coaching
- [ ] Responses reference user's weak areas
- [ ] Coaching style adapts to user performance
- [ ] Recommendations are actionable
- [ ] Motivational content is appropriate

### ✅ XP Scaling
- [ ] XP multipliers reflect difficulty and weak area work
- [ ] Bonus XP awarded for persistence and improvement
- [ ] Total XP calculations are accurate
- [ ] XP progression feels rewarding

### ✅ UI Integration
- [ ] All adaptive features visible in UI
- [ ] Real-time updates work smoothly
- [ ] Color coding is semantically meaningful
- [ ] Information hierarchy is clear

## Troubleshooting Common Issues

### Issue: Difficulty not adjusting
- Check if user has minimum 3 completed challenges
- Verify performance metrics are being saved correctly
- Ensure baseline metrics are properly configured

### Issue: Weak areas not detected
- Confirm user has attempted diverse challenge types
- Check if skill area inference is working correctly
- Verify weak area thresholds are appropriate

### Issue: Coaching responses generic
- Ensure user profile is complete
- Check if weak area context is being passed correctly
- Verify LLM service is functioning properly

### Issue: XP multipliers not applying
- Check adaptive configuration calculation
- Verify XP scaling logic in repository
- Ensure UI displays updated XP values

## Deployment Checklist

- [ ] Database schema updated in production
- [ ] All new services registered in DI container
- [ ] Feature flags enabled for adaptive intelligence
- [ ] Performance monitoring in place
- [ ] Error tracking configured
- [ ] User analytics events added
- [ ] A/B testing setup (if applicable)

This comprehensive testing approach ensures that all Phase 2 adaptive intelligence features work correctly and provide a seamless, personalized learning experience for DevStreaks users.
