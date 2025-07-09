package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.serialization.Serializable

@Serializable
class GeminiTextParserV2 {

    fun parseTrack(text: String): ChallengePathResponse {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var index = 0
        var trackTitle = ""

        if (lines[index].startsWith("Track:")) {
            trackTitle = lines[index].removePrefix("Track:").trim()
            index++
        } else if (!lines[index].startsWith("Day")) {
            trackTitle = lines[index++]
        } else {
        // Skip any invalid header lines (e.g., `{`)
        while (index < lines.size && !lines[index].startsWith("Track:") && !lines[index].startsWith("Day")) {
            index++
        }
        if (index < lines.size && lines[index].startsWith("Track:")) {
            trackTitle = lines[index].removePrefix("Track:").trim()
            index++
        }
    }


    val days = mutableListOf<ChallengeTask>()

        while (index < lines.size) {
            if (lines[index].matches(Regex("""Day \d+:?"""))) {
                val dayNumber = Regex("""Day\s+(\d+):?""")
                    .find(lines[index])?.groupValues?.get(1)?.toIntOrNull() ?: continue
                index++

                var title = ""
                var type = ""
                var xp = 0
                val contentBuilder = StringBuilder()
                val checklist = mutableListOf<String>()
                var whyItMatters: String? = null
                var bonus: String? = null
                var tip: String? = null
                var aiBreakdown: String? = null
                var videoUrl: String? = null
                var codeExample: String? = null
                val challenges = mutableListOf<ChallengeActivity>()

                while (index < lines.size && !lines[index].startsWith("Day")) {
                    val line = lines[index]

                    when {
                        line.startsWith("Title:") -> title = line.removePrefix("Title:").trim()
                        line.startsWith("Type:") -> type = line.removePrefix("Type:").trim()
                        line.startsWith("XP:") -> xp = line.removePrefix("XP:").trim().toIntOrNull() ?: 0
                        line.startsWith("Checklist:") -> {
                            index++
                            while (index < lines.size && lines[index].startsWith("-")) {
                                checklist.add(lines[index].removePrefix("-").trim())
                                index++
                            }
                            continue
                        }
                        line.startsWith("Why It Matters:") -> whyItMatters = line.removePrefix("Why It Matters:").trim()
                        line.startsWith("Tip:") -> tip = line.removePrefix("Tip:").trim()
                        line.startsWith("Bonus:") -> bonus = line.removePrefix("Bonus:").trim()
                        line.startsWith("AI Breakdown:") -> aiBreakdown = line.removePrefix("AI Breakdown:").trim()
                        line.startsWith("Video URL:") -> videoUrl = line.removePrefix("Video URL:").trim()
                        line.startsWith("Code Example:") -> {
                            index++
                            val code = StringBuilder()
                            while (index < lines.size && !lines[index].startsWith(">>>")) {
                                code.appendLine(lines[index])
                                index++
                            }
                            codeExample = code.toString().trim()
                        }
                        line.startsWith("Challenges:") -> {
                            index++
                            while (index < lines.size && lines[index].matches(Regex("""\d+\.\s+Type:.*"""))) {
                                val result = parseChallenge(lines, index)
                                if (result != null) {
                                    challenges.add(result.first)
                                    index = result.second
                                } else index++
                            }
                            continue
                        }
                        else -> contentBuilder.appendLine(line)
                    }

                    index++
                }

                days.add(
                    ChallengeTask(
                        id = "day_$dayNumber",
                        pathId = "pathId",
                        day = dayNumber,
                        title = title,
                        type = type,
                        content = contentBuilder.toString().trim(),
                        xp = xp,
                        checklist = checklist,
                        whyItMatters = whyItMatters,
                        bonus = bonus,
                        tip = tip,
                        aiBreakdown = aiBreakdown,
                        videoUrl = videoUrl,
                        codeExample = codeExample,
                        challenges = challenges
                    )
                )
            } else {
                index++
            }
        }

        return ChallengePathResponse(
            track = trackTitle,
            days = days
        )
    }

    private fun parseChallenge(lines: List<String>, start: Int): Pair<ChallengeActivity, Int>? {
        var index = start

        val typeLine = lines.getOrNull(index++) ?: return null
        val titleLine = lines.getOrNull(index++) ?: return null

        val type = typeLine.removePrefix("Type:").trim().uppercase()
        val title = titleLine.removePrefix("Title:").trim()
        val activityType = ActivityType.valueOf(type)

        var prompt = ""
        var options: List<String>? = null
        var correctAnswer: String? = null
        var starterCode: String? = null
        var solutionCode: String? = null
        var explanation: String? = null

        while (index < lines.size && !lines[index].matches(Regex("""\d+\..*"""))) {
            val line = lines[index]
            when {
                line.startsWith("Description:") || line.startsWith("Question:") -> prompt = line.substringAfter(":").trim()
                line.startsWith("Options:") -> {
                    val opts = mutableListOf<String>()
                    while (index + 1 < lines.size && lines[index + 1].matches(Regex("""[A-D],?.*"""))) {
                        opts.add(lines[++index].substringAfter(",").trim())
                    }
                    options = opts
                }
                line.startsWith("Answer:") -> correctAnswer = line.removePrefix("Answer:").trim()
                line.startsWith("Starter Code:") -> {
                    index++
                    val code = StringBuilder()
                    while (index < lines.size && !lines[index].startsWith(">>>")) {
                        code.appendLine(lines[index])
                        index++
                    }
                    starterCode = code.toString().trim()
                }
                line.startsWith("Solution:") -> {
                    index++
                    val solution = StringBuilder()
                    while (index < lines.size && !lines[index].startsWith(">>>")) {
                        solution.appendLine(lines[index])
                        index++
                    }
                    solutionCode = solution.toString().trim()
                }
                else -> index++
            }
            index++
        }

        return ChallengeActivity(
            id = "Id" + generateUUID(),
            type = activityType,
            prompt = title + ": " + prompt,
            options = options,
            correctAnswer = correctAnswer,
            starterCode = starterCode,
            solutionCode = solutionCode
        ) to index
    }
}

