package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpAndLegalScreen() {
    var selectedSection by remember { mutableStateOf(0) } // 0 = Guía Paso a Paso, 1 = Términos y Cláusulas, 2 = Créditos

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de Sección
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = DarkBackground,
            contentColor = TelegramBlue,
            divider = { Divider(color = CardBorderColor) }
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Guía Paso a Paso", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Términos y Ley", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                icon = { Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                text = { Text("Desarrolladores", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedSection) {
            0 -> UserStepByStepGuideSection()
            1 -> TermsAndLegalDisclaimerSection()
            2 -> CreatorsCreditsSection()
        }
    }
}

@Composable
fun UserStepByStepGuideSection() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF415A77))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TelegramBrandBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Manual de Uso TeleCloud", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("Aprende a respaldar cualquier carpeta y liberar espacio seguro.", color = Color(0xFFC2C6CF), fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            StepCard(
                stepNumber = "1",
                icon = Icons.Default.CloudSync,
                iconColor = TelegramBrandBlue,
                title = "Vincular tu Nube Personal de Telegram",
                description = "Ve a la pestaña 'Nube'. Ingresa tu número telefónico internacional con código de país (ej: +54 9 11 ...). Recibirás un mensaje en tu Telegram oficial con un código de 5 dígitos. Ingrésalo en TeleCloud para activar la sesión segura cifrada.",
                tip = "Tus archivos se guardan directamente en tu propio chat privado de 'Mensajes Guardados' de Telegram, con almacenamiento en la nube ilimitado y gratuito."
            )
        }

        item {
            StepCard(
                stepNumber = "2",
                icon = Icons.Default.FolderOpen,
                iconColor = StatusOrange,
                title = "Elegir cualquier Carpeta del Teléfono",
                description = "Ve a la pestaña 'Explorador'. Tienes acceso completo al almacenamiento de tu móvil (/storage/emulated/0). Navega por cualquier subcarpeta (DCIM de la cámara, WhatsApp Media, Descargas, etc.). Presiona el botón 'Respaldar Aquí' para que esa carpeta comience a vigilarse automáticamente.",
                tip = "Puedes activar la casilla 'Ver ocultos y temp' para ver carpetas ocultas que comienzan con punto o cachés temporales."
            )
        }

        item {
            StepCard(
                stepNumber = "3",
                icon = Icons.Default.Sync,
                iconColor = StatusGreen,
                title = "Configurar Respaldos Automáticos",
                description = "En la pestaña 'Nube', activa el interruptor 'Respaldo automático en segundo plano'. Puedes configurar reglas de ahorro de batería o activar 'Subir solo por Wi-Fi' para no gastar tus datos móviles.",
                tip = "Cada foto, video o documento nuevo que se cree en las carpetas vigiladas se subirá de forma transparente a tu Telegram."
            )
        }

        item {
            StepCard(
                stepNumber = "4",
                icon = Icons.Default.Collections,
                iconColor = StatusPurple,
                title = "Ver y Filtrar la Galería Unificada",
                description = "En la pestaña 'Galería', podrás ver todos los archivos encontrados clasificados por Fotos, Videos, Documentos, Audios, Ocultos y Temporales. Si tocas cualquier archivo verás su estado: si ya está respaldado en la nube de Telegram o si solo existe en la memoria local.",
                tip = "Si un archivo aún no se subió, puedes tocarlo y presionar 'Subir a Telegram Ahora'."
            )
        }

        item {
            StepCard(
                stepNumber = "5",
                icon = Icons.Default.DeleteSweep,
                iconColor = Color(0xFFEF5350),
                title = "Liberar Espacio Seguro del Almacenamiento",
                description = "Ve a la pestaña 'Limpiar'. El sistema inteligente detectará solo los archivos que YA ESTÁN CONFIRMADOS Y RESPALDADOS en Telegram o archivos temporales inservibles. Selecciona los que quieras y toca 'Liberar Espacio'.",
                tip = "Se borrarán del almacenamiento físico del móvil liberando memoria real, pero tus copias en la nube de Telegram quedarán 100% intactas para siempre."
            )
        }
    }
}

@Composable
fun StepCard(
    stepNumber: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    tip: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepNumber, color = Color(0xFF00315E), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }

            Text(description, color = Color(0xFFECEFF1), fontSize = 12.sp, lineHeight = 18.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tip, fontSize = 11.sp, color = Color(0xFFFFD54F), lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TermsAndLegalDisclaimerSection() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF370000)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFB71C1C))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Aviso Legal Importante", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Términos de servicio, exención total de responsabilidad y políticas de uso.", color = Color(0xFFFFCDD2), fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            LegalClauseCard(
                clauseNumber = "Cláusula 1",
                title = "Exención Total de Responsabilidad por Uso Indebido",
                content = "La aplicación TeleCloud es una herramienta de gestión de almacenamiento y respaldo personal. Los desarrolladores, creadores y titulares de la aplicación (La Clave Argentina y Tienda SSH) NO SE HACEN RESPONSABLES bajo ninguna circunstancia por el mal uso, uso ilícito, ilegal o indebido que el usuario final haga de esta aplicación o de los servidores de terceros (incluidos los servidores de Telegram)."
            )
        }

        item {
            LegalClauseCard(
                clauseNumber = "Cláusula 2",
                title = "Responsabilidad Exclusiva sobre el Contenido Respaldado",
                content = "El usuario final es el único y exclusivo responsable de todos y cada uno de los archivos, fotos, videos, documentos, software o cualquier otro material que decida respaldar, transmitir, cargar, descargar o sincronizar mediante TeleCloud. Queda estrictamente prohibido utilizar esta aplicación para almacenar o distribuir material protegido por derechos de autor sin autorización, contenido ilegal, difamatorio o que infrinja las leyes locales o internacionales."
            )
        }

        item {
            LegalClauseCard(
                clauseNumber = "Cláusula 3",
                title = "Exoneración por Eliminación de Archivos y Pérdida de Datos",
                content = "La función de 'Liberar Espacio' y limpieza borra permanentemente archivos físicos del almacenamiento local del dispositivo a solicitud expresa y confirmada del usuario. Aunque la aplicación verifica previamente el estado de respaldo, los desarrolladores (La Clave Argentina y Tienda SSH) NO asumirán ninguna responsabilidad por pérdidas de datos, corrupciones de almacenamiento, fallas del sistema operativo o eliminaciones accidentales provocadas directa o indirectamente por el usuario."
            )
        }

        item {
            LegalClauseCard(
                clauseNumber = "Cláusula 4",
                title = "Independencia de la Plataforma Telegram",
                content = "TeleCloud opera como un cliente local mediante la biblioteca oficial TDLib (Telegram Database Library). Esta aplicación no está afiliada, respaldada ni patrocinada oficialmente por Telegram FZ-LLC. El uso de la API de Telegram está sujeto a los Términos de Servicio de Telegram. Cualquier suspensión, bloqueo o sanción de cuenta por parte de Telegram debido a infracciones del usuario es ajena y exime por completo a los creadores de esta aplicación."
            )
        }

        item {
            LegalClauseCard(
                clauseNumber = "Cláusula 5",
                title = "Aceptación Expresa del Usuario",
                content = "El uso, descarga o ejecución de TeleCloud implica la aceptación incondicional y plena de todas las presentes cláusulas legales, términos y condiciones de exención de responsabilidad."
            )
        }
    }
}

@Composable
fun LegalClauseCard(clauseNumber: String, title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFD32F2F).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        clauseNumber,
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
            Text(content, color = Color(0xFFB0BEC5), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun CreatorsCreditsSection() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TelegramBrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        item {
            Text("TeleCloud", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Nube de Respaldo & Limpiador de Almacenamiento Móvil", fontSize = 12.sp, color = TelegramBlue, textAlign = TextAlign.Center)
        }

        item {
            Divider(color = CardBorderColor, modifier = Modifier.padding(vertical = 4.dp))
        }

        item {
            Text(
                "CRÉDITOS Y DESARROLLO OFICIAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
        }

        item {
            CreatorCard(
                name = "La Clave Argentina",
                role = "Creadores & Desarrolladores Principales",
                tagline = "Innovación tecnológica y soluciones digitales seguras.",
                badgeColor = Color(0xFF00897B),
                icon = Icons.Default.Security
            )
        }

        item {
            CreatorCard(
                name = "Tienda SSH",
                role = "Creadores & Desarrolladores Asociados",
                tagline = "Infraestructura, redes y optimización de servidores.",
                badgeColor = Color(0xFF1E88E5),
                icon = Icons.Default.Dns
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Versión 2.0.0 • Compilación de Producción", fontSize = 11.sp, color = Color.Gray)
                    Text("Todos los derechos reservados © 2026", fontSize = 11.sp, color = Color.Gray)
                    Text("Desarrollado con pasión por La Clave Argentina y Tienda SSH", fontSize = 12.sp, color = TelegramBlue, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun CreatorCard(
    name: String,
    role: String,
    tagline: String,
    badgeColor: Color,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text(role, fontSize = 11.sp, color = TelegramBlue, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(tagline, fontSize = 11.sp, color = Color(0xFF909194), lineHeight = 15.sp)
            }
        }
    }
}
