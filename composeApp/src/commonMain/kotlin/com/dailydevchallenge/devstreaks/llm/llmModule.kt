package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.network.getHttpClient
import org.koin.dsl.module

val llmModule = module {

    single { getHttpClient() }
    single<LLMService> {
        GeminiLLMService(
            get()
        )
    }
    single<AIFeedbackService> {
        AIFeedbackService(get(), get())
    }
}
