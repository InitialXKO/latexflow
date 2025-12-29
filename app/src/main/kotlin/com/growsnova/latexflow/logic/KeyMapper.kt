package com.growsnova.latexflow.logic

/**
 * HID 键盘动作
 */
data class HidAction(
    val scanCode: Int,
    val modifiers: Int = 0
)

/**
 * LaTeX 字符到 HID 键码的映射引擎
 */
class KeyMapper {
    companion object {
        const val MODIFIER_NONE = 0
        const val MODIFIER_SHIFT = 0x02
        
        // 扩展版的 HID ScanCodes (US Layout)
        private val charToHid = mapOf(
            ' ' to HidAction(0x2C),
            '\\' to HidAction(0x31),
            '{' to HidAction(0x2F, MODIFIER_SHIFT),
            '}' to HidAction(0x30, MODIFIER_SHIFT),
            '_' to HidAction(0x2D, MODIFIER_SHIFT),
            '^' to HidAction(0x1E, MODIFIER_SHIFT),
            '(' to HidAction(0x26, MODIFIER_SHIFT),
            ')' to HidAction(0x27, MODIFIER_SHIFT),
            '[' to HidAction(0x2F),
            ']' to HidAction(0x30),
            '+' to HidAction(0x2E, MODIFIER_SHIFT),
            '-' to HidAction(0x2D),
            '=' to HidAction(0x2E),
            '/' to HidAction(0x38),
            '*' to HidAction(0x25, MODIFIER_SHIFT),
            ',' to HidAction(0x36),
            '.' to HidAction(0x37),
            ':' to HidAction(0x33, MODIFIER_SHIFT),
            ';' to HidAction(0x33),
            '\'' to HidAction(0x34),
            '"' to HidAction(0x34, MODIFIER_SHIFT),
            '<' to HidAction(0x36, MODIFIER_SHIFT),
            '>' to HidAction(0x37, MODIFIER_SHIFT),
            '?' to HidAction(0x38, MODIFIER_SHIFT),
            '|' to HidAction(0x31, MODIFIER_SHIFT),
            '~' to HidAction(0x35, MODIFIER_SHIFT),
            '`' to HidAction(0x35),
            '!' to HidAction(0x1E, MODIFIER_SHIFT),
            '@' to HidAction(0x1F, MODIFIER_SHIFT),
            '#' to HidAction(0x20, MODIFIER_SHIFT),
            '$' to HidAction(0x21, MODIFIER_SHIFT),
            '%' to HidAction(0x22, MODIFIER_SHIFT),
            '&' to HidAction(0x25, MODIFIER_SHIFT),
        ).toMutableMap().apply {
            // 添加 A-Z
            for (i in 0..25) {
                put('a' + i, HidAction(0x04 + i))
                put('A' + i, HidAction(0x04 + i, MODIFIER_SHIFT))
            }
            // 添加 0-9
            put('1', HidAction(0x1E))
            put('2', HidAction(0x1F))
            put('3', HidAction(0x20))
            put('4', HidAction(0x21))
            put('5', HidAction(0x22))
            put('6', HidAction(0x23))
            put('7', HidAction(0x24))
            put('8', HidAction(0x25))
            put('9', HidAction(0x26))
            put('0', HidAction(0x27))
        }
    }

    private val unicodeToLatex = mapOf(
        'α' to "\\alpha", 'β' to "\\beta", 'γ' to "\\gamma", 'δ' to "\\delta",
        'θ' to "\\theta", 'π' to "\\pi", 'σ' to "\\sigma", 'ω' to "\\omega",
        'Σ' to "\\sum", 'Δ' to "\\Delta", 'Φ' to "\\Phi", 'Ω' to "\\Omega",
        '∫' to "\\int", '≈' to "\\approx", '≠' to "\\neq", '≤' to "\\le", '≥' to "\\ge",
        '±' to "\\pm", '∞' to "\\infty", '×' to "\\times", '÷' to "\\div"
    )

    /**
     * 将 LaTeX 字符串解析为 HID 动作序列
     */
    fun mapToActions(latex: String): List<HidAction> {
        val actions = mutableListOf<HidAction>()
        
        // Pre-process string to convert Unicode symbols to LaTeX commands
        var processedLatex = latex
        unicodeToLatex.forEach { (unicode, replacement) ->
            processedLatex = processedLatex.replace(unicode.toString(), replacement)
        }

        processedLatex.forEach { char ->
            charToHid[char]?.let {
                actions.add(it)
            } ?: run {
                // 如果是未知字符，暂不处理或记录日志

            }
        }
        return actions
    }
}
