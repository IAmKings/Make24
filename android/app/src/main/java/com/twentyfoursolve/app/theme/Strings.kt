package com.twentyfoursolve.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.twentyfoursolve.core.model.Language

/**
 * Provide localized strings with dynamic language switching.
 * Matches the web demo's i18n translations.
 */
typealias Strings = Map<String, String>

val LocalStringProvider: ProvidableCompositionLocal<Strings> =
    compositionLocalOf { englishStrings }

val LocalLanguage = compositionLocalOf { Language.ZH }

private val englishStrings = mapOf(
    "appName" to "24 SOLVE",
    "tagline" to "The Game of Logic",
    "startGame" to "Start Game",
    "practiceMode" to "Practice Mode",
    "rules" to "Rules",
    "stats" to "Stats",
    "settings" to "Settings",
    "audioSettings" to "Audio, Difficulty & Account",
    "quickTip" to "Quick Tip",
    "tipContent" to "Combine numbers to make 24 using +, -, ×, and ÷. Every card must be used exactly once!",
    "howToPlay" to "How to Play",
    "masterNumbers" to "Master the numbers and hit the magic 24.",
    "rule1Title" to "1. Use Four Numbers",
    "rule1Desc" to "Each round gives you four random numbers. You must use every number exactly once.",
    "rule2Title" to "2. Get Exactly 24",
    "rule2Desc" to "Combine the numbers using math to reach 24. No more, no less.",
    "rule3Title" to "3. The Operators",
    "rule3Desc" to "Use basic arithmetic: Addition, Subtraction, Multiplication, and Division. Parentheses are your friends!",
    "opAdd" to "Addition",
    "opAddDesc" to "a + b — combine two numbers",
    "opSub" to "Subtraction",
    "opSubDesc" to "a − b — take the difference",
    "opMul" to "Multiplication",
    "opMulDesc" to "a × b — multiply two numbers",
    "opDiv" to "Division",
    "opDivDesc" to "a ÷ b — split into equal parts",
    "exampleTitle" to "Example Challenge",
    "backToHome" to "Back to Home",
    "performance" to "Performance",
    "kineticStats" to "Your kinetic playground stats.",
    "winRate" to "Win Rate",
    "totalGames" to "Total Games",
    "avgTime" to "Avg Time",
    "currentStreak" to "Current Streak",
    "activity" to "Activity",
    "last7Days" to "Last 7 Days",
    "topSolves" to "Top Solves",
    "playingHeader" to "Playing 24",
    "timeRemaining" to "Time Remaining",
    "currentScore" to "Score",
    "combineCards" to "Combine cards to reach 24",
    "undo" to "Undo",
    "reset" to "Reset",
    "success" to "Success!",
    "timesUp" to "Time's Up!",
    "masteredLevel" to "You've mastered this level.",
    "failedLevel" to "The logic was just too deep this time.",
    "nextRound" to "Next Round",
    "shareResult" to "Share Result",
    "exitMenu" to "Exit to Menu",
    "exitConfirmTitle" to "Exit Game",
    "exitConfirmText" to "Are you sure you want to exit? Current progress will be lost.",
    "exitConfirm" to "Exit",
    "cancel" to "Cancel",
    "practice" to "Practice",
    "practiceDesc" to "Hone your math skills at your own pace. No timers, just logic.",
    "selectIntensity" to "Select Intensity",
    "easy" to "Easy",
    "easyDesc" to "Numbers 1–6 — simple combinations for beginners.",
    "medium" to "Medium",
    "mediumDesc" to "Numbers 1–10 — trickier combinations and order.",
    "hard" to "Hard",
    "hardDesc" to "Numbers 1–13 (classic deck) — complex solves, often with division.",
    "targetSet" to "Target Set",
    "startPractice" to "Start Practice",
    "selected" to "Selected",
    "play" to "Play",
    "timeTaken" to "Time taken",
    "scoreLabel" to "Score",
    "soundEnabled" to "Sound Effects",
    "soundEnabledDesc" to "Play sound effects during gameplay",
    "difficultyPreference" to "Default Difficulty",
    "languageLabel" to "Language",
    "noData" to "No games played yet. Start playing to see your stats!"
)

private val chineseStrings = mapOf(
    "appName" to "24点大作战",
    "tagline" to "逻辑的终极挑战",
    "startGame" to "开始游戏",
    "practiceMode" to "练习模式",
    "rules" to "游戏规则",
    "stats" to "统计数据",
    "settings" to "设置",
    "audioSettings" to "音频、难度与账户",
    "quickTip" to "温馨提示",
    "tipContent" to "使用 +、-、×、÷ 将四个数字组合成 24。每个数字必须且只能使用一次！",
    "howToPlay" to "玩法介绍",
    "masterNumbers" to "掌握数字规律，挑战神奇的 24。",
    "rule1Title" to "1. 使用四个数字",
    "rule1Desc" to "每轮会给出四个随机数字。你必须使用掉每一个数字。",
    "rule2Title" to "2. 得到 24",
    "rule2Desc" to "通过运算得到 24。不多也不少。",
    "rule3Title" to "3. 运算符号",
    "rule3Desc" to "使用基础运算：加、减、乘、除。括号是你的好帮手！",
    "opAdd" to "加法",
    "opAddDesc" to "a + b — 将两个数相加",
    "opSub" to "减法",
    "opSubDesc" to "a − b — 求两个数的差",
    "opMul" to "乘法",
    "opMulDesc" to "a × b — 将两个数相乘",
    "opDiv" to "除法",
    "opDivDesc" to "a ÷ b — 将两个数相除",
    "exampleTitle" to "示例挑战",
    "backToHome" to "返回主页",
    "performance" to "战绩统计",
    "kineticStats" to "你的逻辑竞技场数据。",
    "winRate" to "胜率",
    "totalGames" to "游戏场次",
    "avgTime" to "平均用时",
    "currentStreak" to "当前连胜",
    "activity" to "活跃度",
    "last7Days" to "最近 7 天",
    "topSolves" to "历史最佳",
    "playingHeader" to "正在挑战 24点",
    "timeRemaining" to "剩余时间",
    "currentScore" to "当前得分",
    "combineCards" to "组合卡片以达到 24",
    "undo" to "撤销",
    "reset" to "重置",
    "success" to "成功！",
    "timesUp" to "时间到！",
    "masteredLevel" to "你已成功解决这道难题。",
    "failedLevel" to "这次的逻辑挑战有点深奥。",
    "nextRound" to "下一关",
    "shareResult" to "分享结果",
    "exitMenu" to "退出到菜单",
    "exitConfirmTitle" to "退出本局",
    "exitConfirmText" to "确定要退出吗？本局进度将丢失。",
    "exitConfirm" to "退出",
    "cancel" to "取消",
    "practice" to "练习模式",
    "practiceDesc" to "按你自己的步调磨练数学技巧。没有计时，只有逻辑。",
    "selectIntensity" to "选择难度",
    "easy" to "简单",
    "easyDesc" to "数字 1-6，基础组合即可凑出 24，适合新手。",
    "medium" to "中等",
    "mediumDesc" to "数字 1-10，需要更巧妙的组合与运算顺序。",
    "hard" to "困难",
    "hardDesc" to "数字 1-13（经典牌面），解法复杂，常需除法与分数策略。",
    "targetSet" to "目标数值",
    "startPractice" to "开始练习",
    "selected" to "已选择",
    "play" to "挑战模式",
    "timeTaken" to "用时",
    "scoreLabel" to "得分",
    "soundEnabled" to "音效",
    "soundEnabledDesc" to "游戏过程中播放音效",
    "difficultyPreference" to "默认难度",
    "languageLabel" to "语言",
    "noData" to "还没有游戏记录。开始游戏后可查看统计！"
)

fun stringsForLanguage(language: Language): Strings {
    return when (language) {
        Language.ZH -> chineseStrings
        Language.EN -> englishStrings
    }
}

@Composable
fun StringProvider(
    language: Language,
    content: @Composable () -> Unit
) {
    val strings = remember(language) { stringsForLanguage(language) }
    CompositionLocalProvider(
        LocalStringProvider provides strings,
        LocalLanguage provides language
    ) {
        content()
    }
}