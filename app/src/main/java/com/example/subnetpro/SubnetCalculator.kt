package com.example.subnetpro

import kotlin.math.pow

// ── DATA CLASSES ────────────────────────────────────────────

data class IPv4Result(
    val ipAddress      : String,
    val networkAddress : String,
    val broadcastAddress: String,
    val subnetMask     : String,
    val wildcardMask   : String,
    val cidr           : Int,
    val firstHost      : String,
    val lastHost       : String,
    val totalHosts     : Long,
    val usableHosts    : Long,
    val ipClass        : String,
    val ipType         : String
)

data class IPv6Result(
    val ipAddress       : String,
    val networkFull     : String,
    val networkCompressed: String,
    val prefixLength    : Int,
    val firstHost       : String,
    val lastHost        : String,
    val totalHosts      : String
)

data class SubnetTableRow(
    val no             : Int,
    val networkAddress : String,
    val broadcastAddress: String,
    val firstHost      : String,
    val lastHost       : String,
    val usableHosts    : Long
)

data class VlsmDepartment(
    val name       : String,
    val hostsNeeded: Int
)

data class VlsmResult(
    val department    : String,
    val hostsNeeded   : Int,
    val networkAddress: String,
    val broadcastAddress: String,
    val subnetMask    : String,
    val cidr          : Int,
    val firstHost     : String,
    val lastHost      : String,
    val usableHosts   : Long
)

// ── IPv4 CALCULATOR ─────────────────────────────────────────

object IPv4Calculator {

    fun calculate(ip: String, cidr: Int): IPv4Result {
        require(cidr in 0..32) { "CIDR harus antara 0-32" }
        val parts = ip.trim().split(".")
        require(parts.size == 4) { "Format IP tidak valid" }

        val ipInt = parts.map { it.toInt() }.fold(0L) { acc, v ->
            require(v in 0..255) { "Nilai oktet tidak valid" }
            (acc shl 8) or v.toLong()
        }

        val maskInt  = if (cidr == 0) 0L else (0xFFFFFFFFL shl (32 - cidr)) and 0xFFFFFFFFL
        val netInt   = ipInt and maskInt
        val bcastInt = netInt or (maskInt.inv() and 0xFFFFFFFFL)
        val wildInt  = maskInt.inv() and 0xFFFFFFFFL

        fun longToIp(l: Long) = "${(l shr 24) and 0xFF}.${(l shr 16) and 0xFF}.${(l shr 8) and 0xFF}.${l and 0xFF}"

        val totalHosts  = 2.0.pow(32 - cidr).toLong()
        val usableHosts = if (cidr >= 31) totalHosts else (totalHosts - 2).coerceAtLeast(0)
        val firstHost   = if (cidr >= 31) longToIp(netInt) else longToIp(netInt + 1)
        val lastHost    = if (cidr >= 31) longToIp(bcastInt) else longToIp(bcastInt - 1)

        val firstOctet = parts[0].toInt()
        val ipClass = when {
            firstOctet in 1..126   -> "A"
            firstOctet == 127      -> "Loopback"
            firstOctet in 128..191 -> "B"
            firstOctet in 192..223 -> "C"
            firstOctet in 224..239 -> "D (Multicast)"
            else                   -> "E (Reserved)"
        }
        val ipType = when {
            firstOctet == 10                                   -> "Private"
            firstOctet == 172 && parts[1].toInt() in 16..31   -> "Private"
            firstOctet == 192 && parts[1].toInt() == 168       -> "Private"
            firstOctet == 127                                  -> "Loopback"
            else                                               -> "Public"
        }

        return IPv4Result(
            ipAddress       = ip.trim(),
            networkAddress  = longToIp(netInt),
            broadcastAddress= longToIp(bcastInt),
            subnetMask      = longToIp(maskInt),
            wildcardMask    = longToIp(wildInt),
            cidr            = cidr,
            firstHost       = firstHost,
            lastHost        = lastHost,
            totalHosts      = totalHosts,
            usableHosts     = usableHosts,
            ipClass         = ipClass,
            ipType         = ipType
        )
    }

    fun subnetMaskToCidr(mask: String): Int {
        val parts = mask.trim().split(".")
        require(parts.size == 4) { "Format subnet mask tidak valid" }
        val maskInt = parts.map { it.toInt() }.fold(0L) { acc, v -> (acc shl 8) or v.toLong() }
        return java.lang.Long.bitCount(maskInt).toInt()
    }

    fun generateSubnetTable(network: String, cidr: Int): List<SubnetTableRow> {
        val baseResult = calculate(network, cidr)
        val rows       = mutableListOf<SubnetTableRow>()
        val parts      = baseResult.networkAddress.split(".").map { it.toInt() }
        var netInt     = parts.fold(0L) { acc, v -> (acc shl 8) or v.toLong() }
        val maskInt    = if (cidr == 0) 0L else (0xFFFFFFFFL shl (32 - cidr)) and 0xFFFFFFFFL
        val blockSize  = (maskInt.inv() and 0xFFFFFFFFL) + 1
        val maxSubnets = minOf((0xFFFFFFFFL / blockSize).toInt(), 256)

        fun longToIp(l: Long) = "${(l shr 24) and 0xFF}.${(l shr 16) and 0xFF}.${(l shr 8) and 0xFF}.${l and 0xFF}"

        for (i in 0 until maxSubnets) {
            val bcast      = netInt or (maskInt.inv() and 0xFFFFFFFFL)
            val usable     = if (cidr >= 31) blockSize else (blockSize - 2).coerceAtLeast(0)
            val firstHost  = if (cidr >= 31) longToIp(netInt) else longToIp(netInt + 1)
            val lastHost   = if (cidr >= 31) longToIp(bcast) else longToIp(bcast - 1)
            rows.add(SubnetTableRow(i + 1, longToIp(netInt), longToIp(bcast), firstHost, lastHost, usable))
            netInt = (bcast + 1) and 0xFFFFFFFFL
            if (netInt == 0L) break
        }
        return rows
    }

    fun calculateVlsm(network: String, departments: List<VlsmDepartment>): List<VlsmResult> {
        val sorted  = departments.sortedByDescending { it.hostsNeeded }
        val results = mutableListOf<VlsmResult>()
        val parts   = network.trim().split(".").map { it.toInt() }
        var netInt  = parts.fold(0L) { acc, v -> (acc shl 8) or v.toLong() }

        fun longToIp(l: Long) = "${(l shr 24) and 0xFF}.${(l shr 16) and 0xFF}.${(l shr 8) and 0xFF}.${l and 0xFF}"
        fun ceilLog2(n: Int): Int { var c = 0; var x = 1; while (x < n + 2) { x *= 2; c++ }; return c }

        for (dept in sorted) {
            val bits      = ceilLog2(dept.hostsNeeded)
            val cidr      = 32 - bits
            val maskInt   = if (cidr == 0) 0L else (0xFFFFFFFFL shl bits) and 0xFFFFFFFFL
            val bcast     = netInt or (maskInt.inv() and 0xFFFFFFFFL)
            val usable    = (2.0.pow(bits) - 2).toLong().coerceAtLeast(0)
            results.add(VlsmResult(
                department     = dept.name,
                hostsNeeded    = dept.hostsNeeded,
                networkAddress = longToIp(netInt),
                broadcastAddress = longToIp(bcast),
                subnetMask     = longToIp(maskInt),
                cidr           = cidr,
                firstHost      = longToIp(netInt + 1),
                lastHost       = longToIp(bcast - 1),
                usableHosts    = usable
            ))
            netInt = (bcast + 1) and 0xFFFFFFFFL
        }
        return results
    }
}

// ── IPv6 CALCULATOR ─────────────────────────────────────────

object IPv6Calculator {

    fun calculate(ip: String, prefix: Int): IPv6Result {
        require(prefix in 0..128) { "Prefix harus antara 0-128" }
        val expanded = expandIPv6(ip.trim())
        val groups   = expanded.split(":").map { it.toInt(16) }
        require(groups.size == 8) { "Format IPv6 tidak valid" }

        val ipBits   = groups.flatMap { g -> (15 downTo 0).map { (g shr it) and 1 } }
        val netBits  = ipBits.mapIndexed { i, b -> if (i < prefix) b else 0 }
        val lastBits = ipBits.mapIndexed { i, b -> if (i < prefix) b else 1 }

        fun bitsToIpv6(bits: List<Int>): String {
            return (0 until 8).joinToString(":") { g ->
                val value = (0 until 16).fold(0) { acc, b -> (acc shl 1) or bits[g * 16 + b] }
                value.toString(16).padStart(4, '0')
            }
        }

        fun compress(ip6: String): String {
            var result = ip6
                .replace(Regex("\\b0+([0-9a-f]+)"), "$1")
            val longestZero = Regex("(?:^|:)(?:0:)+0").findAll(result)
                .maxByOrNull { it.value.length }?.value ?: ""
            if (longestZero.isNotEmpty()) {
                result = result.replaceFirst(longestZero, "::")
                    .replace(":::", "::")
            }
            return result
        }

        val netFull   = bitsToIpv6(netBits)
        val lastFull  = bitsToIpv6(lastBits)
        val totalBits = 128 - prefix
        val totalStr  = if (totalBits >= 64) "2^$totalBits (sangat besar)" else (1L shl totalBits).toString()

        return IPv6Result(
            ipAddress        = ip.trim(),
            networkFull      = netFull,
            networkCompressed= compress(netFull),
            prefixLength     = prefix,
            firstHost        = compress(netFull),
            lastHost         = compress(lastFull),
            totalHosts       = totalStr
        )
    }

    private fun expandIPv6(ip: String): String {
        val parts = ip.split("::")
        return if (parts.size == 2) {
            val left  = if (parts[0].isEmpty()) emptyList() else parts[0].split(":")
            val right = if (parts[1].isEmpty()) emptyList() else parts[1].split(":")
            val mid   = List(8 - left.size - right.size) { "0000" }
            (left + mid + right).joinToString(":") { it.padStart(4, '0') }
        } else {
            ip.split(":").joinToString(":") { it.padStart(4, '0') }
        }
    }
}