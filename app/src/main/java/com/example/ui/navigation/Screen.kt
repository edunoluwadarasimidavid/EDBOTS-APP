package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object ConnectBot : Screen("connect_bot")
    object BotManagement : Screen("bot_management")
    object Commands : Screen("commands")
    object AutoReply : Screen("auto_reply")
    object AiConfig : Screen("ai_config")
    object Groups : Screen("groups")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
}
