package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.EdBotsTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BotViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as EdBotsApplication).container

        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(appContainer.authRepository)
            )
            val botViewModel: BotViewModel = viewModel(
                factory = BotViewModel.Factory(appContainer.botRepository, appContainer.preferences)
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(appContainer.preferences, appContainer.edbotsClient)
            )

            val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val botUiState by botViewModel.uiState.collectAsStateWithLifecycle()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val commands by botViewModel.commands.collectAsStateWithLifecycle()
            val autoReplies by botViewModel.autoReplies.collectAsStateWithLifecycle()
            val aiConfig by botViewModel.aiConfig.collectAsStateWithLifecycle()
            val groupSettings by botViewModel.groupSettings.collectAsStateWithLifecycle()

            val navController = rememberNavController()

            // Automatically navigate to Login when logged out from any screen
            LaunchedEffect(authUiState.isLoggedIn) {
                val currentRoute = navController.currentDestination?.route
                if (!authUiState.isLoggedIn && currentRoute != null &&
                    currentRoute != Screen.Login.route &&
                    currentRoute != Screen.Register.route &&
                    currentRoute != Screen.Splash.route
                ) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            EdBotsTheme(darkTheme = settingsUiState.darkTheme) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            isLoggedIn = authUiState.isLoggedIn,
                            onNavigateNext = { loggedIn ->
                                val target = if (loggedIn) Screen.Dashboard.route else Screen.Login.route
                                navController.navigate(target) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Login.route) {
                        LoginScreen(
                            uiState = authUiState,
                            onLogin = { email, pass ->
                                authViewModel.login(email, pass)
                            },
                            onNavigateToRegister = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.Register.route)
                            },
                            onClearError = { authViewModel.clearMessages() }
                        )

                        LaunchedEffect(authUiState.isLoggedIn) {
                            if (authUiState.isLoggedIn) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    }

                    composable(Screen.Register.route) {
                        RegisterScreen(
                            uiState = authUiState,
                            onRegister = { name, email, pass, confirmPass ->
                                authViewModel.register(name, email, pass, confirmPass)
                            },
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            },
                            onClearError = { authViewModel.clearMessages() }
                        )

                        LaunchedEffect(authUiState.isLoggedIn) {
                            if (authUiState.isLoggedIn) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            }
                        }
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            uiState = botUiState,
                            onRefresh = { botViewModel.refreshBotStatus() },
                            onStartBot = { botViewModel.performBotAction("start") },
                            onStopBot = { botViewModel.performBotAction("stop") },
                            onNavigateToConnect = { navController.navigate(Screen.ConnectBot.route) },
                            onNavigateToManagement = { navController.navigate(Screen.BotManagement.route) },
                            onNavigateToCommands = { navController.navigate(Screen.Commands.route) },
                            onNavigateToAutoReply = { navController.navigate(Screen.AutoReply.route) },
                            onNavigateToAi = { navController.navigate(Screen.AiConfig.route) },
                            onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                            onWatchBonusAd = { botViewModel.watchBonusAd() },
                            onClearError = { botViewModel.clearMessages() },
                            onClearSuccess = { botViewModel.clearMessages() }
                        )
                    }

                    composable(Screen.ConnectBot.route) {
                        ConnectBotScreen(
                            uiState = botUiState,
                            onBack = { navController.popBackStack() },
                            onRequestPhoneCode = { phone -> botViewModel.requestPairingCode(phone) },
                            onResetPairing = { botViewModel.resetPairing() },
                            onSetPairingToken = { token -> botViewModel.setPairingToken(token) },
                            onRefreshStatus = { botViewModel.checkPairingStatus() },
                            onClearError = { botViewModel.clearMessages() }
                        )
                    }

                    composable(Screen.BotManagement.route) {
                        BotManagementScreen(
                            uiState = botUiState,
                            onBack = { navController.popBackStack() },
                            onRefresh = { botViewModel.refreshBotStatus() },
                            onStartBot = { botViewModel.performBotAction("start") },
                            onStopBot = { botViewModel.performBotAction("stop") },
                            onRestartBot = { botViewModel.performBotAction("restart") },
                            onDisconnectBot = { botViewModel.performBotAction("logout") },
                            onClearError = { botViewModel.clearMessages() }
                        )
                    }

                    composable(Screen.Commands.route) {
                        CommandsScreen(
                            commands = commands,
                            onToggleCommand = { id, enabled -> botViewModel.toggleCommand(id, enabled) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.AutoReply.route) {
                        AutoReplyScreen(
                            rules = autoReplies,
                            activeChatId = botUiState.activeChatId.ifBlank { null },
                            onSetActiveChatId = { botViewModel.setActiveChatId(it) },
                            onAddRule = { trigger, response, matchType ->
                                botViewModel.addAutoReply(trigger, response, matchType)
                            },
                            onDeleteRule = { id -> botViewModel.deleteAutoReply(id) },
                            onToggleRule = { id, enabled -> botViewModel.toggleAutoReply(id, enabled) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.AiConfig.route) {
                        // Pull the server-synced AI settings (enabled + personality).
                        LaunchedEffect(Unit) { botViewModel.refreshAiConfig() }
                        AiScreen(
                            currentConfig = aiConfig,
                            onSaveConfig = { config ->
                                botViewModel.updateAiConfig(config)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Groups.route) {
                        // Load real WhatsApp groups from GET /api/groups.
                        LaunchedEffect(Unit) { botViewModel.fetchGroups() }
                        GroupsScreen(
                            currentSettings = groupSettings,
                            groups = botUiState.groups,
                            selectedGroupId = botUiState.activeChatId.ifBlank { null },
                            onFetchGroups = { botViewModel.fetchGroups() },
                            onSelectGroup = {
                                botViewModel.setActiveChatId(it)
                                botViewModel.loadGroupSettings(it)
                            },
                            onSaveSettings = { settings ->
                                val groupId = botUiState.activeChatId
                                if (groupId.isNotBlank()) {
                                    botViewModel.saveGroupSettings(groupId, settings)
                                } else {
                                    botViewModel.updateGroupSettings(settings)
                                }
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            uiState = settingsUiState,
                            onUpdateApiBaseUrl = { settingsViewModel.updateApiBaseUrl(it) },
                            onUpdateApiKey = { settingsViewModel.updateApiKey(it) },
                            onUpdatePairingToken = {
                                settingsViewModel.updatePairingToken(it)
                                botViewModel.setPairingToken(it)
                            },
                            onUpdateAppwriteEndpoint = { settingsViewModel.updateAppwriteEndpoint(it) },
                            onUpdateAppwriteProjectId = { settingsViewModel.updateAppwriteProjectId(it) },
                            onUpdateAppwriteDatabaseId = { settingsViewModel.updateAppwriteDatabaseId(it) },
                            onUpdateAppwriteCollectionId = { settingsViewModel.updateAppwriteCollectionId(it) },
                            onToggleDarkTheme = { settingsViewModel.setDarkTheme(it) },
                            onTestConnection = { settingsViewModel.testConnection() },
                            onClearTestResult = { settingsViewModel.clearTestResult() },
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            user = authUiState.user,
                            premiumStatus = authUiState.premiumStatus,
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
