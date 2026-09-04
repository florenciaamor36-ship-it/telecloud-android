package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import android.widget.VideoView
import android.widget.MediaController
import android.net.Uri
import android.media.MediaPlayer
import android.content.Intent
import com.example.data.*
import com.example.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Paleta de Colores M3 Dark Elegante
val TelegramBlue = Color(0xFFD0E4FF)
val TelegramLightBlue = Color(0xFF1D1E21)
val DarkBackground = Color(0xFF141517)
val CardBackground = Color(0xFF222428)
val CardBorderColor = Color(0xFF34383F)
val StatusGreen = Color(0xFF4CAF50)
val StatusOrange = Color(0xFFFFA000)
val StatusPurple = Color(0xFFAB47BC)
val TelegramBrandBlue = Color(0xFF229ED9)

enum class AppTab(val title: String, val icon: ImageVector) {
    GALLERY("Galería", Icons.Default.Collections),
    FOLDERS("Explorador", Icons.Default.FolderOpen),
    CLEANER("Limpiar", Icons.Default.DeleteSweep),
    CLOUD("Nube", Icons.Default.CloudSync),
    INFO("Ayuda & Ley", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAppUi(viewModel: BackupViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val uploadLogs by viewModel.uploadLogsState.collectAsState()
    val watchedFolders by viewModel.watchedFoldersState.collectAsState()
    val galleryItems by viewModel.galleryItemsState.collectAsState()
    val cleanerCandidates by viewModel.cleanerCandidatesState.collectAsState()
    val freedSpaceBytes by viewModel.freedSpaceBytes.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val showHiddenAndTemp by viewModel.showHiddenAndTemp.collectAsState()

    val currentBrowsingPath by viewModel.currentBrowsingPath.collectAsState()
    val directoryNodes by viewModel.directoryNodesState.collectAsState()
    val isBrowsingLoading by viewModel.isBrowsingLoading.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.GALLERY) }
    var selectedMediaItem by remember { mutableStateOf<GalleryMediaItem?>(null) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showCleanConfirmDialog by remember { mutableStateOf(false) }

    val isUserLoggedIn = authState is TdApi.AuthorizationStateReady || settings.activePhoneNumber != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramBrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = when (currentTab) {
                                    AppTab.GALLERY -> "Galería y Archivos"
                                    AppTab.FOLDERS -> "Explorador del Teléfono"
                                    AppTab.CLEANER -> "Liberador de Espacio"
                                    AppTab.CLOUD -> "Nube Telegram"
                                    AppTab.INFO -> "Guía, Términos & Créditos"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isUserLoggedIn) "Telegram Conectado" else "Modo Local",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUserLoggedIn) StatusGreen else StatusOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentTab = AppTab.INFO },
                        modifier = Modifier.testTag("help_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Guía y Términos",
                            tint = if (currentTab == AppTab.INFO) TelegramBlue else Color.LightGray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshGalleryAndCleaner() },
                        modifier = Modifier.testTag("refresh_action")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = Color.LightGray)
                    }
                    if (!isUserLoggedIn) {
                        FilledTonalButton(
                            onClick = { currentTab = AppTab.CLOUD },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TelegramBrandBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Conectar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = { currentTab = AppTab.CLOUD }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.LightGray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00315E),
                            selectedTextColor = TelegramBlue,
                            indicatorColor = TelegramBlue,
                            unselectedIconColor = Color(0xFF8E9099),
                            unselectedTextColor = Color(0xFF8E9099)
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.GALLERY -> GalleryScreen(
                    items = galleryItems,
                    showHiddenAndTemp = showHiddenAndTemp,
                    isLoggedIn = isUserLoggedIn,
                    onToggleHidden = { viewModel.toggleShowHiddenAndTemp() },
                    onItemClick = { selectedMediaItem = it },
                    onBackupClick = { viewModel.backupItemNow(it) },
                    onConnectTelegram = { currentTab = AppTab.CLOUD }
                )
                AppTab.FOLDERS -> DeviceFilesystemScreen(
                    currentPath = currentBrowsingPath,
                    directoryNodes = directoryNodes,
                    isLoading = isBrowsingLoading,
                    watchedFolders = watchedFolders,
                    showHiddenAndTemp = showHiddenAndTemp,
                    onBrowseTo = { viewModel.browseTo(it) },
                    onBrowseUp = { viewModel.browseUp() },
                    onToggleHidden = { viewModel.toggleShowHiddenAndTemp() },
                    onToggleWatchPath = { viewModel.toggleWatchFolderFromBrowser(it) },
                    onToggleFolder = { id, en -> viewModel.toggleFolderEnabled(id, en) },
                    onDeleteWatchedFolder = { viewModel.deleteWatchedFolder(it) },
                    onAddCustomPathClick = { showAddFolderDialog = true }
                )
                AppTab.CLEANER -> StorageCleanerScreen(
                    candidates = cleanerCandidates,
                    freedSpaceBytes = freedSpaceBytes,
                    isCleaning = isCleaning,
                    onToggleCandidate = { viewModel.toggleCleanerCandidate(it) },
                    onSelectAll = { viewModel.selectAllCleanerCandidates(it) },
                    onCleanClick = { showCleanConfirmDialog = true }
                )
                AppTab.CLOUD -> CloudSettingsScreen(
                    viewModel = viewModel,
                    settings = settings,
                    uploadLogs = uploadLogs,
                    authState = authState,
                    isConnected = isConnected
                )
                AppTab.INFO -> HelpAndLegalScreen()
            }
        }
    }

    // Diálogo con detalles de archivo
    selectedMediaItem?.let { item ->
        MediaItemDetailDialog(
            item = item,
            isLoggedIn = isUserLoggedIn,
            onDismiss = { selectedMediaItem = null },
            onBackupNow = {
                viewModel.backupItemNow(item)
                selectedMediaItem = null
            }
        )
    }

    // Diálogo para añadir ruta manual
    if (showAddFolderDialog) {
        AddCustomFolderDialog(
            currentPath = currentBrowsingPath,
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { path, name ->
                viewModel.addCustomFolder(path, name)
                showAddFolderDialog = false
            }
        )
    }

    // Diálogo de confirmación para liberar espacio real del teléfono
    if (showCleanConfirmDialog) {
        val selectedCandidates = cleanerCandidates.filter { it.isSelected }
        val totalToClean = selectedCandidates.sumOf { it.fileSizeBytes }
        val count = selectedCandidates.size
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "¿Liberar $count archivos del teléfono?",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se borrarán permanentemente ${formatFileSize(totalToClean)} del almacenamiento físico de tu móvil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tus copias en la nube de Telegram permanecerán seguras e intactas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5D6A7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cleanSelectedFiles()
                        showCleanConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Borrar del Móvil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = CardBackground
        )
    }
}

// =========================================================================
// 1. PANTALLA DE GALERÍA (FOTOS, VIDEOS, DOCUMENTOS, OCULTOS Y TEMPORALES)
// =========================================================================

@Composable
fun GalleryScreen(
    items: List<GalleryMediaItem>,
    showHiddenAndTemp: Boolean,
    isLoggedIn: Boolean,
    onToggleHidden: () -> Unit,
    onItemClick: (GalleryMediaItem) -> Unit,
    onBackupClick: (GalleryMediaItem) -> Unit,
    onConnectTelegram: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos", "Fotos", "Videos", "Documentos", "Audios", "Ocultos", "Temporales")

    val filteredItems = remember(items, selectedCategory) {
        when (selectedCategory) {
            "Fotos" -> items.filter { it.mediaType == MediaType.IMAGE }
            "Videos" -> items.filter { it.mediaType == MediaType.VIDEO }
            "Documentos" -> items.filter { it.mediaType == MediaType.DOCUMENT }
            "Audios" -> items.filter { it.mediaType == MediaType.AUDIO }
            "Ocultos" -> items.filter { it.isHidden }
            "Temporales" -> items.filter { it.isTemporary }
            else -> items
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner informativo si Telegram no está conectado
        if (!isLoggedIn) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, Color(0xFF455A64)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = TelegramBrandBlue, modifier = Modifier.size(18.dp))
                        Text(
                            "Conecta tu cuenta para respaldar a la nube.",
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onConnectTelegram,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Conectar", color = TelegramBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Filtros de categoría con SCROLL HORIZONTAL (evita todo desbordamiento)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TelegramBlue,
                        selectedLabelColor = Color(0xFF00315E),
                        containerColor = CardBackground,
                        labelColor = Color(0xFFC2C6CF)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) TelegramBlue else CardBorderColor),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Barra con contador e interruptor de Ocultos/Temporales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredItems.size} archivos encontrados",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleHidden() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (showHiddenAndTemp) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (showHiddenAndTemp) TelegramBlue else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showHiddenAndTemp) "Ocultos activos" else "Ocultos pausados",
                    fontSize = 11.sp,
                    color = if (showHiddenAndTemp) TelegramBlue else Color.Gray
                )
            }
        }

        // Cuadrícula de fotos y archivos
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No se encontraron archivos", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ve a la pestaña Explorador para seleccionar carpetas del almacenamiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.filePath }) { item ->
                    GalleryMediaCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onBackupClick = { onBackupClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryMediaCard(
    item: GalleryMediaItem,
    onClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Contenedor visual del elemento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        when {
                            item.isHidden -> Color(0xFF2E1C38)
                            item.isTemporary -> Color(0xFF3E2723)
                            item.mediaType == MediaType.IMAGE -> Color(0xFF132838)
                            item.mediaType == MediaType.VIDEO -> Color(0xFF2C1628)
                            item.mediaType == MediaType.DOCUMENT -> Color(0xFF332014)
                            item.mediaType == MediaType.AUDIO -> Color(0xFF122C24)
                            else -> Color(0xFF1F2229)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.mediaType == MediaType.IMAGE || item.mediaType == MediaType.VIDEO) {
                    AsyncImage(
                        model = item.contentUri ?: File(item.filePath),
                        contentDescription = "Vista previa de ${item.fileName}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when {
                            item.isHidden -> Icons.Default.VisibilityOff
                            item.isTemporary -> Icons.Default.HourglassEmpty
                            item.mediaType == MediaType.DOCUMENT -> Icons.Default.Description
                            item.mediaType == MediaType.AUDIO -> Icons.Default.AudioFile
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Badges en la parte superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de tipo especial
                    if (item.isHidden) {
                        Surface(
                            color = StatusPurple.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Oculto", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else if (item.isTemporary) {
                        Surface(
                            color = Color(0xFFE65100).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Temp", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Estado de Nube
                    when (item.backupStatus) {
                        "BACKED_UP" -> {
                            Surface(
                                color = StatusGreen.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Nube", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "UPLOADING" -> {
                            Surface(
                                color = TelegramBrandBlue.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Subiendo", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                        else -> {
                            Surface(
                                color = Color(0xFF333333).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Móvil", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }

            // Información del archivo con prevención estricta de desbordamiento
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = TelegramBlue,
                        maxLines = 1
                    )
                    Text(
                        text = item.folderName.substringBefore(" ("),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF909194),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2. EXPLORADOR COMPLETO DEL TELÉFONO (FILE SYSTEM BROWSER & CARPETAS)
// =========================================================================

@Composable
fun DeviceFilesystemScreen(
    currentPath: String,
    directoryNodes: List<DeviceDirectoryNode>,
    isLoading: Boolean,
    watchedFolders: List<WatchedFolder>,
    showHiddenAndTemp: Boolean,
    onBrowseTo: (String) -> Unit,
    onBrowseUp: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleWatchPath: (String) -> Unit,
    onToggleFolder: (Long, Boolean) -> Unit,
    onDeleteWatchedFolder: (Long) -> Unit,
    onAddCustomPathClick: () -> Unit
) {
    var viewMode by remember { mutableStateOf(0) } // 0 = Explorador de Archivos del Teléfono, 1 = Carpetas en Respaldo Activo

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de Modo
        TabRow(
            selectedTabIndex = viewMode,
            containerColor = DarkBackground,
            contentColor = TelegramBlue,
            divider = { Divider(color = CardBorderColor) }
        ) {
            Tab(
                selected = viewMode == 0,
                onClick = { viewMode = 0 },
                text = { Text("Explorador de Disco", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = viewMode == 1,
                onClick = { viewMode = 1 },
                text = { Text("Respaldos Activos (${watchedFolders.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (viewMode == 0) {
            // MODO EXPLORADOR DE ARCHIVOS REAL DEL DISPOSITIVO
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de navegación de rutas (Breadcrumbs & Acciones)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onBrowseUp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Subir nivel", tint = TelegramBlue)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ruta actual:", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Botón de observar carpeta actual
                        FilledTonalButton(
                            onClick = { onToggleWatchPath(currentPath) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TelegramBrandBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Respaldar Aquí", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Barra de filtros: Ocultos y Temporales
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${directoryNodes.size} elementos",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleHidden() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = showHiddenAndTemp,
                            onCheckedChange = { onToggleHidden() },
                            colors = CheckboxDefaults.colors(checkedColor = TelegramBlue, checkmarkColor = Color(0xFF00315E)),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver ocultos y temp", fontSize = 11.sp, color = Color.White)
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelegramBrandBlue)
                    }
                } else if (directoryNodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Carpeta vacía o sin permisos de lectura.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    val filteredNodes = if (showHiddenAndTemp) {
                        directoryNodes
                    } else {
                        directoryNodes.filter { !it.isHidden && !it.isTemporary }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(filteredNodes, key = { it.path }) { node ->
                            DeviceDirectoryNodeRow(
                                node = node,
                                onFolderClick = { if (node.isDirectory) onBrowseTo(node.path) },
                                onToggleWatch = { onToggleWatchPath(node.path) }
                            )
                        }
                    }
                }
            }
        } else {
            // MODO CARPETAS OBSERVADAS EN RESPALDO ACTIVO
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Button(
                        onClick = onAddCustomPathClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue, contentColor = Color(0xFF00315E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Ruta Manual", fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text(
                        "CARPETAS EN OBSERVACIÓN (${watchedFolders.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }

                items(watchedFolders, key = { it.id }) { folder ->
                    WatchedFolderItemCard(
                        folder = folder,
                        onToggle = { onToggleFolder(folder.id, it) },
                        onDelete = if (folder.isCustom) { { onDeleteWatchedFolder(folder.id) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceDirectoryNodeRow(
    node: DeviceDirectoryNode,
    onFolderClick: () -> Unit,
    onToggleWatch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (node.isWatched) StatusGreen.copy(alpha = 0.5f) else CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = node.isDirectory, onClick = onFolderClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono principal con distintivo
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            node.isDirectory && node.isHidden -> Color(0xFF4A148C)
                            node.isDirectory && node.isTemporary -> Color(0xFFBF360C)
                            node.isDirectory -> Color(0xFF004D40)
                            node.isHidden -> Color(0xFF311B92)
                            node.isTemporary -> Color(0xFF4E342E)
                            else -> Color(0xFF263238)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        node.isDirectory -> Icons.Default.Folder
                        node.isHidden -> Icons.Default.VisibilityOff
                        node.isTemporary -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (node.isHidden) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = StatusPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Oculto", fontSize = 9.sp, color = StatusPurple, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    if (node.isTemporary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = StatusOrange.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Temp", fontSize = 9.sp, color = StatusOrange, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (node.isDirectory) "${node.fileCount} elementos" else formatFileSize(node.sizeBytes),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            // Acción para carpetas: respaldar / observar
            if (node.isDirectory) {
                IconButton(
                    onClick = onToggleWatch,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (node.isWatched) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = "Sincronizar",
                        tint = if (node.isWatched) StatusGreen else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WatchedFolderItemCard(
    folder: WatchedFolder,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (folder.isEnabled) CardBackground else Color(0xFF1D1E21)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (folder.isEnabled) CardBorderColor else Color(0xFF2C2D30)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (folder.isEnabled) TelegramBlue else Color.Gray,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (folder.isEnabled) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folder.folderPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF909194),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFFEF5350))
                }
            }

            Switch(
                checked = folder.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// =========================================================================
// 3. LIMPIADOR SEGURO DE ALMACENAMIENTO (SIN DESBORDAMIENTOS)
// =========================================================================

@Composable
fun StorageCleanerScreen(
    candidates: List<CleanerCandidate>,
    freedSpaceBytes: Long,
    isCleaning: Boolean,
    onToggleCandidate: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onCleanClick: () -> Unit
) {
    val totalReclaimableBytes = remember(candidates) {
        candidates.filter { it.isSelected }.sumOf { it.fileSizeBytes }
    }
    val allSelected = candidates.isNotEmpty() && candidates.all { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Tarjeta Resumen y Botón de Limpieza
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formatFileSize(totalReclaimableBytes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF5350),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Listo para liberar", fontSize = 11.sp, color = Color(0xFF909194), maxLines = 1)
                    }

                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = CardBorderColor
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formatFileSize(freedSpaceBytes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TelegramBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Espacio ya liberado", fontSize = 11.sp, color = Color(0xFF909194), maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Seguro: Solo se borran archivos ya respaldados en Telegram.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5D6A7),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón Adaptativo sin desbordamiento
                Button(
                    onClick = onCleanClick,
                    enabled = candidates.any { it.isSelected } && !isCleaning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCleaning) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Liberar ${formatFileSize(totalReclaimableBytes)} del Móvil",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controles de selección y lista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ARCHIVOS CANDIDATOS (${candidates.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (candidates.isNotEmpty()) {
                TextButton(
                    onClick = { onSelectAll(!allSelected) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (allSelected) "Deseleccionar todo" else "Seleccionar todo",
                        color = TelegramBlue,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (candidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Almacenamiento Optimizado", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No hay archivos duplicados que borrar. Los archivos respaldados aparecerán aquí.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(candidates, key = { it.filePath }) { candidate ->
                    CleanerCandidateRow(
                        candidate = candidate,
                        onToggle = { onToggleCandidate(candidate.filePath) }
                    )
                }
            }
        }
    }
}

@Composable
fun CleanerCandidateRow(
    candidate: CleanerCandidate,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (candidate.isSelected) CardBackground else Color(0xFF1D1E21)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (candidate.isSelected) TelegramBlue.copy(alpha = 0.5f) else Color(0xFF333333)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = candidate.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TelegramBlue,
                    checkmarkColor = Color(0xFF00315E)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${candidate.folderType} • Respaldado en Telegram",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatFileSize(candidate.fileSizeBytes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TelegramBlue,
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// 4. CONFIGURACIÓN DE NUBE (100% REAL - SIN SIMULADORES NI TEXTOS FICTICIOS)
// =========================================================================

@Composable
fun CloudSettingsScreen(
    viewModel: BackupViewModel,
    settings: BackupSettings,
    uploadLogs: List<UploadLog>,
    authState: TdApi.AuthorizationState,
    isConnected: Boolean
) {
    var phoneInput by remember { mutableStateOf(settings.activePhoneNumber ?: "") }
    var codeInput by remember { mutableStateOf("") }
    val isReady = authState is TdApi.AuthorizationStateReady || settings.activePhoneNumber != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tarjeta de Sesión Telegram Real
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBrandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sesión Telegram TDLib", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Text(
                                    text = if (isReady) "Conectado • ${settings.activePhoneNumber ?: "Activo"}" else "Desconectado",
                                    fontSize = 11.sp,
                                    color = if (isReady) StatusGreen else StatusOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isReady) {
                            OutlinedButton(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Salir", fontSize = 11.sp)
                            }
                        }
                    }

                    if (isReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CardBorderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF415A77))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TelegramBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Destino: Mensajes Guardados", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("Tus archivos se guardan directamente en tu propio chat privado 'Mensajes Guardados' con almacenamiento ilimitado.", color = Color(0xFFC2C6CF), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!isReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CardBorderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (authState is TdApi.AuthorizationStateWaitCode) {
                            Text("Código de confirmación de Telegram:", fontSize = 12.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { if (it.length <= 6) codeInput = it },
                                    label = { Text("Código") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { viewModel.submitCode(codeInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue, contentColor = Color(0xFF00315E)),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("Validar", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text("Ingresa tu número con código de país:", fontSize = 12.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                placeholder = { Text("+54 9 11 1234 5678") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { if (phoneInput.isNotBlank()) viewModel.setPhoneNumber(phoneInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBrandBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enviar Código por Telegram", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Reglas de Sincronización Real
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reglas de Sincronización", fontWeight = FontWeight.Bold, color = Color.White)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Respaldo automático en segundo plano", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text("Monitorea las carpetas del teléfono en tiempo real", color = Color(0xFF909194), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.isAutoBackupEnabled,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(isAutoBackupEnabled = it)) }
                        )
                    }

                    Divider(color = CardBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Subir solo por Wi-Fi", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text("Ahorra tus datos móviles", color = Color(0xFF909194), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.wifiOnly,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(wifiOnly = it)) }
                        )
                    }

                    Divider(color = CardBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Solo mientras carga la batería", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text("Evita el consumo de batería en movimiento", color = Color(0xFF909194), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.batteryChargingOnly,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(batteryChargingOnly = it)) }
                        )
                    }
                }
            }
        }

        // Historial de Logs de Subida
        item {
            Text(
                "HISTORIAL DE SUBIDAS REALES (${uploadLogs.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
        }

        if (uploadLogs.isEmpty()) {
            item {
                Text("No hay registros de subida todavía.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            items(uploadLogs.take(20), key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.fileName,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${log.fileType} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Surface(
                            color = when (log.status) {
                                "COMPLETED" -> StatusGreen.copy(alpha = 0.2f)
                                "UPLOADING" -> TelegramBrandBlue.copy(alpha = 0.2f)
                                else -> Color(0xFFD32F2F).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = log.status,
                                color = when (log.status) {
                                    "COMPLETED" -> StatusGreen
                                    "UPLOADING" -> TelegramBrandBlue
                                    else -> Color(0xFFEF5350)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pie de autoría oficial
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Creado y Desarrollado por",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        "La Clave Argentina & Tienda SSH",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                }
            }
        }
    }
}

// =========================================================================
// DIÁLOGOS AUXILIARES
// =========================================================================

@Composable
fun MediaItemDetailDialog(
    item: GalleryMediaItem,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onBackupNow: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.fileName,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Media Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.mediaType) {
                        MediaType.IMAGE -> {
                            AsyncImage(
                                model = item.contentUri ?: File(item.filePath),
                                contentDescription = "Vista previa de la imagen",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        MediaType.VIDEO -> {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val mediaController = MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        setVideoURI(item.contentUri?.let(Uri::parse) ?: Uri.fromFile(File(item.filePath)))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                }
                            )
                        }
                        MediaType.AUDIO -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = TelegramBrandBlue,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                if (isPlaying) {
                                                    mediaPlayer?.pause()
                                                    isPlaying = false
                                                } else {
                                                    if (mediaPlayer == null) {
                                                        mediaPlayer = MediaPlayer().apply {
                                                            setDataSource(item.filePath)
                                                            prepare()
                                                            setOnCompletionListener {
                                                                isPlaying = false
                                                            }
                                                        }
                                                    }
                                                    mediaPlayer?.start()
                                                    isPlaying = true
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier
                                            .background(TelegramBrandBlue, CircleShape)
                                            .size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isPlaying) "Reproduciendo audio..." else "Toca para reproducir",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = when (item.mediaType) {
                                        MediaType.DOCUMENT -> Icons.Default.Description
                                        MediaType.TEMPORARY -> Icons.Default.FolderZip
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Sin vista previa para este archivo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // File Details Metadata
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("Ubicación", item.folderName)
                    DetailRow("Tamaño", formatFileSize(item.sizeBytes))
                    DetailRow("Tipo", item.mediaType.name)
                    DetailRow("Ruta", item.filePath)
                    DetailRow(
                        "Estado Nube",
                        if (item.isBackedUp) "✅ Respaldado en Telegram" else "📱 Solo en teléfono"
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open in System Player Fallback
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val file = File(item.filePath)
                                val uri = Uri.fromFile(file)
                                val mimeType = when (item.mediaType) {
                                    MediaType.IMAGE -> "image/*"
                                    MediaType.VIDEO -> "video/*"
                                    MediaType.AUDIO -> "audio/*"
                                    else -> "*/*"
                                }
                                setDataAndType(uri, mimeType)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Abrir con otra app",
                        tint = Color.LightGray
                    )
                }

                if (!item.isBackedUp) {
                    Button(
                        onClick = onBackupNow,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue, contentColor = Color(0xFF00315E))
                    ) {
                        Text("Subir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        },
        containerColor = CardBackground
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color(0xFF909194), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AddCustomFolderDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (path: String, name: String) -> Unit
) {
    var path by remember { mutableStateOf(currentPath) }
    var name by remember { mutableStateOf(File(currentPath).name.ifBlank { "Mi Carpeta" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Ruta del Teléfono", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ingresa la ruta absoluta de cualquier carpeta que quieras observar y respaldar:",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Ruta en el Almacenamiento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Descriptivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (path.isNotBlank()) onConfirm(path, name) },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue, contentColor = Color(0xFF00315E))
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = CardBackground
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
