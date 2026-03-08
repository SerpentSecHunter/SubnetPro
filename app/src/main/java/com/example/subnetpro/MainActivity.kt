package com.example.subnetpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

// ── THEME COLORS ─────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary       = Color(0xFF4FC3F7),
    secondary     = Color(0xFF81C784),
    background    = Color(0xFF0D1117),
    surface       = Color(0xFF161B22),
    onBackground  = Color(0xFFE6EDF3),
    onSurface     = Color(0xFFE6EDF3),
    error         = Color(0xFFFF7B72)
)

private val LightColors = lightColorScheme(
    primary       = Color(0xFF0277BD),
    secondary     = Color(0xFF2E7D32),
    background    = Color(0xFFF5F7FA),
    surface       = Color(0xFFFFFFFF),
    onBackground  = Color(0xFF1C1C1E),
    onSurface     = Color(0xFF1C1C1E),
    error         = Color(0xFFD32F2F)
)

// ── ACTIVITY ─────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context       = LocalContext.current
            val themePref     = remember { ThemePreference(context) }
            val themeMode     by themePref.themeFlow.collectAsStateWithLifecycle(initialValue = "auto")
            val scope         = rememberCoroutineScope()
            val isSystemDark  = isSystemInDarkTheme()
            val useDark       = when (themeMode) {
                "dark"  -> true
                "light" -> false
                else    -> isSystemDark
            }
            MaterialTheme(colorScheme = if (useDark) DarkColors else LightColors) {
                SubnetProApp(
                    themeMode   = themeMode,
                    onThemeChange = { scope.launch { themePref.saveTheme(it) } }
                )
            }
        }
    }
}

// ── ENUM HALAMAN ─────────────────────────────────────────────

enum class AppPage { IPv4, IPv6, TABLE, VLSM }

// ── ROOT APP ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubnetProApp(themeMode: String, onThemeChange: (String) -> Unit) {
    var currentPage by remember { mutableStateOf(AppPage.IPv4) }
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("SubnetPro", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                when (themeMode) {
                                    "dark"  -> Icons.Default.DarkMode
                                    "light" -> Icons.Default.LightMode
                                    else    -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Tema"
                            )
                        }
                        DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("🌙 Gelap") },
                                onClick = { onThemeChange("dark"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("☀️ Terang") },
                                onClick = { onThemeChange("light"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("🔄 Otomatis") },
                                onClick = { onThemeChange("auto"); showThemeMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(AppPage.IPv4, Icons.Default.NetworkCheck, "IPv4"),
                    Triple(AppPage.IPv6, Icons.Default.Dns,          "IPv6"),
                    Triple(AppPage.TABLE, Icons.Default.TableChart,  "Tabel"),
                    Triple(AppPage.VLSM, Icons.Default.AccountTree,  "VLSM")
                ).forEach { (page, icon, label) ->
                    NavigationBarItem(
                        selected = currentPage == page,
                        onClick  = { currentPage = page },
                        icon     = { Icon(icon, contentDescription = label) },
                        label    = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                AppPage.IPv4  -> IPv4Page()
                AppPage.IPv6  -> IPv6Page()
                AppPage.TABLE -> SubnetTablePage()
                AppPage.VLSM  -> VlsmPage()
            }
        }
    }
}

// ── IPv4 PAGE ─────────────────────────────────────────────────

@Composable
fun IPv4Page() {
    var ipInput     by remember { mutableStateOf("192.168.1.0") }
    var cidrInput   by remember { mutableStateOf("24") }
    var maskInput   by remember { mutableStateOf("") }
    var useMask     by remember { mutableStateOf(false) }
    var result      by remember { mutableStateOf<IPv4Result?>(null) }
    var errorMsg    by remember { mutableStateOf("") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kalkulator IPv4", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputField("Alamat IP", ipInput, { ipInput = it; errorMsg = "" },
                        "Contoh: 192.168.1.0", KeyboardType.Ascii)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useMask, onCheckedChange = { useMask = it })
                        Spacer(Modifier.width(8.dp))
                        Text(if (useMask) "Gunakan Subnet Mask" else "Gunakan CIDR",
                            fontSize = 13.sp)
                    }

                    if (useMask) {
                        InputField("Subnet Mask", maskInput, { maskInput = it; errorMsg = "" },
                            "Contoh: 255.255.255.0", KeyboardType.Ascii)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("/", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary)
                            InputField("CIDR", cidrInput, { cidrInput = it; errorMsg = "" },
                                "0-32", KeyboardType.Number,
                                modifier = Modifier.weight(1f))
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val cidr = if (useMask) {
                                    IPv4Calculator.subnetMaskToCidr(maskInput)
                                } else cidrInput.trim().toInt()
                                result   = IPv4Calculator.calculate(ipInput, cidr)
                                errorMsg = ""
                            } catch (e: Exception) {
                                errorMsg = "Error: ${e.message}"
                                result   = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🔍 Hitung", fontWeight = FontWeight.Bold) }
                }
            }
        }

        result?.let { r ->
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hasil", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary)
                        ResultRow("Alamat IP",        "${r.ipAddress}/${r.cidr}")
                        ResultRow("Network Address",  r.networkAddress)
                        ResultRow("Broadcast",        r.broadcastAddress)
                        ResultRow("Subnet Mask",      r.subnetMask)
                        ResultRow("Wildcard Mask",    r.wildcardMask)
                        ResultRow("CIDR Notation",    "/${r.cidr}")
                        ResultRow("Host Pertama",     r.firstHost)
                        ResultRow("Host Terakhir",    r.lastHost)
                        ResultRow("Total Host",       r.totalHosts.toString())
                        ResultRow("Host Tersedia",    r.usableHosts.toString())
                        ResultRow("Kelas IP",         r.ipClass)
                        ResultRow("Tipe IP",          r.ipType)
                    }
                }
            }

            // Binary representation
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Representasi Biner", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary)
                        BinaryRow("IP",      r.ipAddress)
                        BinaryRow("Network", r.networkAddress)
                        BinaryRow("Mask",    r.subnetMask)
                    }
                }
            }
        }
    }
}

// ── IPv6 PAGE ─────────────────────────────────────────────────

@Composable
fun IPv6Page() {
    var ipInput   by remember { mutableStateOf("2001:db8::") }
    var prefInput by remember { mutableStateOf("32") }
    var result    by remember { mutableStateOf<IPv6Result?>(null) }
    var errorMsg  by remember { mutableStateOf("") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kalkulator IPv6", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputField("Alamat IPv6", ipInput, { ipInput = it; errorMsg = "" },
                        "Contoh: 2001:db8::", KeyboardType.Ascii)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("/", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary)
                        InputField("Prefix Length", prefInput, { prefInput = it; errorMsg = "" },
                            "0-128", KeyboardType.Number, modifier = Modifier.weight(1f))
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                result   = IPv6Calculator.calculate(ipInput, prefInput.trim().toInt())
                                errorMsg = ""
                            } catch (e: Exception) {
                                errorMsg = "Error: ${e.message}"
                                result   = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🔍 Hitung", fontWeight = FontWeight.Bold) }
                }
            }
        }

        result?.let { r ->
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hasil", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary)
                        ResultRow("Alamat IPv6",           r.ipAddress)
                        ResultRow("Prefix Length",         "/${r.prefixLength}")
                        ResultRow("Network (Full)",        r.networkFull)
                        ResultRow("Network (Compressed)",  r.networkCompressed)
                        ResultRow("Host Pertama",          r.firstHost)
                        ResultRow("Host Terakhir",         r.lastHost)
                        ResultRow("Total Host",            r.totalHosts)
                    }
                }
            }
        }
    }
}

// ── SUBNET TABLE PAGE ─────────────────────────────────────────

@Composable
fun SubnetTablePage() {
    var netInput  by remember { mutableStateOf("192.168.0.0") }
    var cidrInput by remember { mutableStateOf("26") }
    var rows      by remember { mutableStateOf<List<SubnetTableRow>>(emptyList()) }
    var errorMsg  by remember { mutableStateOf("") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tabel Subnet", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputField("Network Address", netInput, { netInput = it; errorMsg = "" },
                        "Contoh: 192.168.0.0", KeyboardType.Ascii)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("/", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary)
                        InputField("CIDR", cidrInput, { cidrInput = it; errorMsg = "" },
                            "0-32", KeyboardType.Number, modifier = Modifier.weight(1f))
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                rows     = IPv4Calculator.generateSubnetTable(netInput, cidrInput.trim().toInt())
                                errorMsg = ""
                            } catch (e: Exception) {
                                errorMsg = "Error: ${e.message}"
                                rows     = emptyList()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📊 Generate Tabel", fontWeight = FontWeight.Bold) }
                }
            }
        }

        if (rows.isNotEmpty()) {
            item {
                Text("${rows.size} subnet ditemukan", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary)
            }
            items(rows) { row ->
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Subnet #${row.no}", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        ResultRow("Network",    row.networkAddress)
                        ResultRow("Broadcast",  row.broadcastAddress)
                        ResultRow("Host Awal",  row.firstHost)
                        ResultRow("Host Akhir", row.lastHost)
                        ResultRow("Host",       row.usableHosts.toString())
                    }
                }
            }
        }
    }
}

// ── VLSM PAGE ─────────────────────────────────────────────────

@Composable
fun VlsmPage() {
    var netInput     by remember { mutableStateOf("192.168.1.0") }
    var departments  by remember { mutableStateOf(listOf(
        VlsmDepartment("Marketing", 50),
        VlsmDepartment("IT", 30),
        VlsmDepartment("HRD", 10)
    ))}
    var results      by remember { mutableStateOf<List<VlsmResult>>(emptyList()) }
    var errorMsg     by remember { mutableStateOf("") }
    var newDeptName  by remember { mutableStateOf("") }
    var newDeptHosts by remember { mutableStateOf("") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("VLSM Calculator", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        item {
            SectionCard {
                InputField("Network Address", netInput, { netInput = it; errorMsg = "" },
                    "Contoh: 192.168.1.0", KeyboardType.Ascii)
            }
        }

        item {
            Text("Daftar Departemen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(departments) { dept ->
            SectionCard {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(dept.name, fontWeight = FontWeight.Medium)
                        Text("${dept.hostsNeeded} host dibutuhkan",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = {
                        departments = departments.filter { it != dept }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tambah Departemen", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    InputField("Nama Departemen", newDeptName, { newDeptName = it },
                        "Contoh: Finance", KeyboardType.Text)
                    InputField("Jumlah Host", newDeptHosts, { newDeptHosts = it },
                        "Contoh: 25", KeyboardType.Number)
                    OutlinedButton(
                        onClick = {
                            try {
                                val hosts = newDeptHosts.trim().toInt()
                                require(newDeptName.isNotBlank()) { "Nama tidak boleh kosong" }
                                require(hosts > 0) { "Host harus lebih dari 0" }
                                departments  = departments + VlsmDepartment(newDeptName.trim(), hosts)
                                newDeptName  = ""
                                newDeptHosts = ""
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Input tidak valid"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Tambah") }
                }
            }
        }

        item {
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Button(
                onClick = {
                    try {
                        require(departments.isNotEmpty()) { "Tambahkan minimal 1 departemen" }
                        results  = IPv4Calculator.calculateVlsm(netInput, departments)
                        errorMsg = ""
                    } catch (e: Exception) {
                        errorMsg = "Error: ${e.message}"
                        results  = emptyList()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("⚡ Hitung VLSM", fontWeight = FontWeight.Bold) }
        }

        if (results.isNotEmpty()) {
            item {
                Text("Hasil Alokasi VLSM", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            items(results) { r ->
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(r.department, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        ResultRow("Host Dibutuhkan",  r.hostsNeeded.toString())
                        ResultRow("Network Address",  "${r.networkAddress}/${r.cidr}")
                        ResultRow("Subnet Mask",      r.subnetMask)
                        ResultRow("Broadcast",        r.broadcastAddress)
                        ResultRow("Host Pertama",     r.firstHost)
                        ResultRow("Host Terakhir",    r.lastHost)
                        ResultRow("Host Tersedia",    r.usableHosts.toString())
                    }
                }
            }
        }
    }
}

// ── REUSABLE COMPOSABLES ──────────────────────────────────────

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun InputField(
    label    : String,
    value    : String,
    onValue  : (String) -> Unit,
    hint     : String,
    keyboard : KeyboardType = KeyboardType.Text,
    modifier : Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValue,
        label         = { Text(label) },
        placeholder   = { Text(hint, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine    = true,
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(8.dp)
    )
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(
            text     = label,
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text       = value,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(0.55f),
            textAlign  = TextAlign.End
        )
    }
}

@Composable
fun BinaryRow(label: String, ip: String) {
    val parts  = ip.split(".")
    val binary = parts.joinToString(".") { it.toInt().toString(2).padStart(8, '0') }
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(binary, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.secondary)
    }
}