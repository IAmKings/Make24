package com.make24.solver

/*
 * 精简任意精度有符号整数（纯 Kotlin，跨平台，无 java.* 依赖）。
 *
 * 仅用于 fallback 路径（任意 Int 输入、自定义大目标、标准路径溢出时）。
 * 采用 32-bit limb 小端数组；运算基于 Long 中间量（无 128 位需求）。
 * 除法定使用 Knuth Algorithm D（TAOCP 4.3.1）。
 *
 * 数值范围：fallback 的中间值上界约 (2^31)^8 = 2^248（8 个 limb），
 * 远小于实现的安全上限（IntArray 长度）。性能足够单次 solve 使用。
 */

internal class BigNum(limbs0: IntArray, negative0: Boolean) {
    val limbs: IntArray
    /** 归一化：零永远为正（保证 equals/toString 稳定）。 */
    val negative: Boolean

    init {
        limbs = limbs0
        negative = negative0 && !(limbs.size == 1 && limbs[0] == 0)
    }

    val isZero: Boolean get() = limbs.size == 1 && limbs[0] == 0
    val sign: Int get() = if (isZero) 0 else if (negative) -1 else 1

    operator fun plus(o: BigNum): BigNum {
        if (isZero) return o
        if (o.isZero) return this
        return if (negative == o.negative) {
            BigNum(addMag(limbs, o.limbs), negative)
        } else {
            val c = cmpMag(limbs, o.limbs)
            if (c >= 0) BigNum(subMag(limbs, o.limbs), negative)
            else BigNum(subMag(o.limbs, limbs), o.negative)
        }
    }

    operator fun minus(o: BigNum): BigNum = plus(o.negate())

    operator fun unaryMinus(): BigNum = BigNum(limbs.copyOf(), !negative)

    operator fun times(o: BigNum): BigNum = BigNum(mulMag(limbs, o.limbs), negative xor o.negative)

    operator fun div(o: BigNum): BigNum {
        if (o.isZero) throw ArithmeticException("division by zero")
        return divMod(this, o).first
    }

    operator fun rem(o: BigNum): BigNum {
        if (o.isZero) throw ArithmeticException("division by zero")
        return divMod(this, o).second
    }

    fun abs(): BigNum = if (negative) BigNum(limbs.copyOf(), false) else this

    fun negate(): BigNum = BigNum(limbs.copyOf(), !negative)

    operator fun compareTo(o: BigNum): Int {
        if (isZero && o.isZero) return 0
        if (negative != o.negative) return if (negative) -1 else 1
        val c = cmpMag(limbs, o.limbs)
        return if (negative) -c else c
    }

    fun gcd(o: BigNum): BigNum {
        var a = abs()
        var b = o.abs()
        while (!b.isZero) {
            val t = a % b
            a = b
            b = t
        }
        return a
    }

    override fun equals(other: Any?): Boolean =
        other is BigNum && negative == other.negative && limbs.contentEquals(other.limbs)

    override fun hashCode(): Int = 31 * limbs.contentHashCode() + negative.hashCode()

    override fun toString(): String {
        if (isZero) return "0"
        var mag = limbs
        val parts = ArrayList<String>()
        while (!(mag.size == 1 && mag[0] == 0)) {
            val (q, r) = divModSmall(mag, DECIMAL_BASE)
            var rs = r.toString()
            while (rs.length < 9) rs = "0$rs"
            parts.add(rs)
            mag = q
        }
        // 块按低到高收集，翻转后最高块在前；每块 9 位补零，最高块去掉前导零。
        val digits = parts.reversed().joinToString("").trimStart('0')
        return if (negative) "-$digits" else digits
    }

    companion object {
        val ZERO: BigNum = BigNum(intArrayOf(0), false)
        val ONE: BigNum = BigNum(intArrayOf(1), false)
        private const val DECIMAL_BASE = 1_000_000_000L

        fun fromLong(v: Long): BigNum {
            if (v == 0L) return ZERO
            if (v == Long.MIN_VALUE) return BigNum(intArrayOf(0, 0x80000000.toInt()), true)
            val neg = v < 0
            val x = if (neg) -v else v
            val lo = (x and 0xFFFFFFFFL).toInt()
            val hi = (x ushr 32).toInt()
            val mag = if (hi == 0) intArrayOf(lo) else intArrayOf(lo, hi)
            return BigNum(mag, neg)
        }

        fun fromString(s: String): BigNum {
            var neg = false
            var str = s
            if (str.startsWith("-")) {
                neg = true
                str = str.substring(1)
            }
            var mag = intArrayOf(0)
            for (ch in str) {
                mag = mulSmallMag(mag, 10)
                mag = addSmallMag(mag, (ch - '0').toLong())
            }
            return BigNum(mag, neg)
        }
    }
}

private const val BASE = 1L shl 32

// ---------------- 大小（无符号幅度）辅助 ----------------

private fun trim(a: IntArray): IntArray {
    var len = a.size
    while (len > 1 && a[len - 1] == 0) len--
    return if (len == a.size) a else a.copyOf(len)
}

private fun cmpMag(a: IntArray, b: IntArray): Int {
    if (a.size != b.size) return if (a.size > b.size) 1 else -1
    for (i in a.size - 1 downTo 0) {
        val av = a[i].toLong() and 0xFFFFFFFFL
        val bv = b[i].toLong() and 0xFFFFFFFFL
        if (av != bv) return if (av > bv) 1 else -1
    }
    return 0
}

private fun addMag(a: IntArray, b: IntArray): IntArray {
    val max = maxOf(a.size, b.size)
    val r = IntArray(max + 1)
    var carry = 0L
    for (i in 0 until max) {
        val av = if (i < a.size) a[i].toLong() and 0xFFFFFFFFL else 0L
        val bv = if (i < b.size) b[i].toLong() and 0xFFFFFFFFL else 0L
        val s = av + bv + carry
        r[i] = s.toInt()
        carry = s ushr 32
    }
    if (carry != 0L) r[max] = carry.toInt()
    return trim(r)
}

/** |a| - |b|，要求 |a| >= |b|。 */
private fun subMag(a: IntArray, b: IntArray): IntArray {
    val r = IntArray(a.size)
    var borrow = 0L
    for (i in a.indices) {
        val av = a[i].toLong() and 0xFFFFFFFFL
        val bv = if (i < b.size) b[i].toLong() and 0xFFFFFFFFL else 0L
        var s = av - bv - borrow
        if (s < 0) {
            s += BASE
            borrow = 1
        } else {
            borrow = 0
        }
        r[i] = s.toInt()
    }
    return trim(r)
}

private fun mulMag(a: IntArray, b: IntArray): IntArray {
    val out = IntArray(a.size + b.size)
    for (i in a.indices) {
        val av = a[i].toLong() and 0xFFFFFFFFL
        if (av == 0L) continue
        var carry = 0L
        for (j in b.indices) {
            val cur = out[i + j].toLong() and 0xFFFFFFFFL
            val p = av * (b[j].toLong() and 0xFFFFFFFFL) + cur + carry
            out[i + j] = p.toInt()
            carry = p ushr 32
        }
        out[i + b.size] = carry.toInt()
    }
    return trim(out)
}

// ---------------- 小乘法 / 小加法（fromString 用） ----------------

private fun mulSmallMag(a: IntArray, d: Long): IntArray {
    val out = IntArray(a.size + 1)
    var carry = 0L
    for (i in a.indices) {
        val p = (a[i].toLong() and 0xFFFFFFFFL) * d + carry
        out[i] = p.toInt()
        carry = p ushr 32
    }
    if (carry != 0L) out[a.size] = carry.toInt()
    return trim(out)
}

private fun addSmallMag(a: IntArray, d: Long): IntArray {
    val out = a.copyOf()
    var carry = d
    for (i in out.indices) {
        val s = (out[i].toLong() and 0xFFFFFFFFL) + carry
        out[i] = s.toInt()
        carry = s ushr 32
        if (carry == 0L) break
    }
    if (carry != 0L) {
        val r = out.copyOf(out.size + 1)
        r[out.size] = carry.toInt()
        return r
    }
    return out
}

// ---------------- 除法：Knuth Algorithm D ----------------

/** 16-bit 块逐位除法：64 位无符号 (hi32, lo32) / d，d < 2^32。 */
private fun divMod64(hi32: Long, lo32: Long, d: Long): Pair<Long, Long> {
    var rem = 0L
    var q = 0L
    val chunks = longArrayOf(
        lo32 and 0xFFFFL,
        (lo32 ushr 16) and 0xFFFFL,
        hi32 and 0xFFFFL,
        (hi32 ushr 16) and 0xFFFFL,
    )
    for (k in 3 downTo 0) {
        val cur = (rem shl 16) or chunks[k]
        q = (q shl 16) or (cur / d)
        rem = cur % d
    }
    return q to rem
}

/** 32×32 -> 64 的乘法高 32 位（两个操作数均 < 2^32）。 */
private fun mulHi(x: Long, y: Long): Long {
    val xLo = x and 0xFFFFL
    val xHi = x ushr 16
    val yLo = y and 0xFFFFL
    val yHi = y ushr 16
    val ll = xLo * yLo
    val lh = xLo * yHi + (ll ushr 16)
    val hl = xHi * yLo + (lh and 0xFFFFL)
    val hh = xHi * yHi + (lh ushr 16) + (hl ushr 16)
    return hh and 0xFFFFFFFFL
}

/** 比较 x*y（<2^64）与 (hi, lo)（hi 高 32 位，lo 低 32 位）。 */
private fun cmpMulVs(x: Long, y: Long, hi: Long, lo: Long): Int {
    val pLo = (x * y) and 0xFFFFFFFFL
    val pHi = mulHi(x, y)
    if (pHi != hi) return if (pHi > hi) 1 else -1
    if (pLo != lo) return if (pLo > lo) 1 else -1
    return 0
}

private fun shiftLeftMag(a: IntArray, bits: Int): IntArray {
    if (bits == 0) return a.copyOf()
    val out = IntArray(a.size + 1)
    var carry = 0L
    for (i in a.indices) {
        val v = (a[i].toLong() and 0xFFFFFFFFL) shl bits
        out[i] = (v and 0xFFFFFFFFL or carry).toInt()
        carry = v ushr 32
    }
    if (carry != 0L) out[a.size] = carry.toInt()
    return trim(out)
}

private fun shiftRightMag(a: IntArray, bits: Int): IntArray {
    if (bits == 0) return a.copyOf()
    val out = IntArray(a.size)
    var carry = 0L
    for (i in a.size - 1 downTo 0) {
        val v = a[i].toLong() and 0xFFFFFFFFL
        out[i] = ((v ushr bits) or carry).toInt()
        carry = (v shl (32 - bits)) and 0xFFFFFFFFL
    }
    return trim(out)
}

/** |u| / |v|，返回 (商, 余数)，均为幅度。v 非零。 */
private fun divModMag(uIn: IntArray, vIn: IntArray): Pair<IntArray, IntArray> {
    val c = cmpMag(uIn, vIn)
    if (c < 0) return intArrayOf(0) to uIn.copyOf()
    if (c == 0) return intArrayOf(1) to intArrayOf(0)

    val n = vIn.size
    val m = uIn.size - n
    val shift = clz32(vIn[n - 1])
    val v = if (shift == 0) vIn.copyOf() else shiftLeftMag(vIn, shift)
    // Knuth D 需要 u 至少 m+n+1 个 limb（最高位可为 0），shift 后可能被 trim 掉，这里补齐。
    val uRaw = if (shift == 0) uIn.copyOf() else shiftLeftMag(uIn, shift)
    val u = if (uRaw.size < m + n + 1) uRaw.copyOf(m + n + 1) else uRaw
    val q = IntArray(m + 1)
    val vn1 = v[n - 1].toLong() and 0xFFFFFFFFL
    val vn2 = if (n >= 2) v[n - 2].toLong() and 0xFFFFFFFFL else 0L

    for (j in m downTo 0) {
        val ujn = if (j + n < u.size) u[j + n].toLong() and 0xFFFFFFFFL else 0L
        val ujn1 = u[j + n - 1].toLong() and 0xFFFFFFFFL
        var qhat: Long
        var rhat: Long
        if (ujn == vn1) {
            // 避免 (ujn*BASE+ujn1)/vn1 溢出：分子 >= BASE*vn1，商 >= BASE，必为 BASE-1
            qhat = BASE - 1
            rhat = ujn1 + vn1
        } else {
            val (qq, rr) = divMod64(ujn, ujn1, vn1)
            qhat = qq
            rhat = rr
        }
        // 校正（TAOCP D3）
        while (qhat >= BASE ||
            (n >= 2 && cmpMulVs(qhat, vn2, rhat, u[j + n - 2].toLong() and 0xFFFFFFFFL) > 0)
        ) {
            qhat--
            rhat += vn1
            if (rhat >= BASE) break
        }
        // 乘减：u[j..j+n] -= qhat * v（D4）
        var k = 0L
        for (i in 0 until n) {
            val p = qhat * (v[i].toLong() and 0xFFFFFFFFL) + k
            k = p ushr 32
            var t = (u[j + i].toLong() and 0xFFFFFFFFL) - (p and 0xFFFFFFFFL)
            if (t < 0) {
                t += BASE
                k += 1
            }
            u[j + i] = t.toInt()
        }
        var tn = (u[j + n].toLong() and 0xFFFFFFFFL) - k
        if (tn < 0) {
            // add-back（D6）
            tn += BASE
            var carry = 0L
            for (i in 0 until n) {
                val s = (u[j + i].toLong() and 0xFFFFFFFFL) + (v[i].toLong() and 0xFFFFFFFFL) + carry
                u[j + i] = s.toInt()
                carry = s ushr 32
            }
            u[j + n] = (tn and 0xFFFFFFFFL).plus(carry).toInt()
            qhat--
        } else {
            u[j + n] = tn.toInt()
        }
        q[j] = qhat.toInt()
    }
    val rRaw = if (n < u.size) u.copyOfRange(0, n) else u.copyOf()
    val r = if (shift == 0) trim(rRaw) else trim(shiftRightMag(rRaw, shift))
    return trim(q) to r
}

private fun divMod(u: BigNum, v: BigNum): Pair<BigNum, BigNum> {
    if (v.isZero) throw ArithmeticException("division by zero")
    if (u.isZero) return BigNum.ZERO to BigNum.ZERO
    val qNeg = u.negative xor v.negative
    val (qm, rm) = divModMag(u.limbs, v.limbs)
    return BigNum(qm, qNeg) to BigNum(rm, u.negative)
}

private fun divModSmall(a: IntArray, d: Long): Pair<IntArray, Long> {
    val r = IntArray(a.size)
    var rem = 0L
    for (i in a.size - 1 downTo 0) {
        val (q, rr) = divMod64(rem, a[i].toLong() and 0xFFFFFFFFL, d)
        r[i] = q.toInt()
        rem = rr
    }
    return trim(r) to rem
}

/** 32 位整数的前导零位数（手写，避免依赖实验性 stdlib API）。 */
private fun clz32(x0: Int): Int {
    var v = x0
    var n = 0
    if (v and 0xFFFF0000.toInt() == 0) {
        n += 16
        v = v shl 16
    }
    if (v and 0xFF000000.toInt() == 0) {
        n += 8
        v = v shl 8
    }
    if (v and 0xF0000000.toInt() == 0) {
        n += 4
        v = v shl 4
    }
    if (v and 0xC0000000.toInt() == 0) {
        n += 2
        v = v shl 2
    }
    if (v and 0x80000000.toInt() == 0) {
        n += 1
    }
    return n
}
