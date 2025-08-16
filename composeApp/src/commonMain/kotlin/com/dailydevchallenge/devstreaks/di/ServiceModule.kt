// di/ServiceModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.devstreaks.service.*
import org.koin.dsl.module

val serviceModule = module {
    // Phase 2 Adaptive Intelligence Services
    single { DifficultyCalculator() }
    single { WeakAreaDetectionService(get()) }
    single { PersonalizedAICoachingService(get(), get()) }
    single { RealTimeWeakAreaMonitoringService(get()) }
    single { AdaptiveIntelligenceOrchestrator(get(), get(), get(), get()) }

    // Phase 3 Advanced Learning Engines - LLM-powered services
    single { SkillTreeEngine(get(), get(), get(), get()) }
    single { ProjectBasedLearningEngine(get(), get(), get(), get(), get()) }
    single { SocialEngine(get(), get()) }  // Only needs 2 dependencies based on constructor
    single { PredictiveAnalyticsEngine(get(), get(), get(), get(),get()) }
    single { ContentRecommendationService(get(), get()) }
    single { DifficultyAdjustmentEngine(get(), get()) }
    single { WeakAreaPrioritizationService(get(), get()) }

    // Challenge Generation Services
    single { AdaptiveChallengeGenerator(get()) }
}
