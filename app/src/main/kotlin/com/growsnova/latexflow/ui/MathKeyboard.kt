package com.growsnova.latexflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geogebra.keyboard.base.Keyboard
import org.geogebra.keyboard.base.impl.DefaultKeyboardFactory
import org.geogebra.keyboard.base.model.WeightedButton
import org.geogebra.keyboard.base.ResourceType

@Composable
fun MathKeyboard(
    modifier: Modifier = Modifier,
    onSymbolSelected: (String) -> Unit
) {
    val factory = remember { DefaultKeyboardFactory() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val keyboards = remember {
        listOf(
            factory.createMathKeyboard(),
            factory.createFunctionsKeyboard(),
            factory.createGreekKeyboard(),
            factory.createSpecialSymbolsKeyboard()
        )
    }
    
    val currentKeyboard = keyboards[selectedTabIndex]
    val model = currentKeyboard.model
    val tabs = listOf("123", "f(x)", "ABC", "!#?")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F3F4)) // GeoGebra light gray background
            .padding(4.dp)
    ) {
        // Tab Selector
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title, fontSize = 14.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keyboard Rows
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            model.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.buttons.forEach { button ->
                        val weightedButton = button as? WeightedButton
                        val weight = weightedButton?.weight ?: 1.0f
                        
                        Button(
                            onClick = { 
                                val actionName = button.primaryActionName
                                val resourceName = button.resourceName
                                
                                when (button.primaryActionType) {
                                    org.geogebra.keyboard.base.ActionType.CUSTOM -> {
                                        when (resourceName) {
                                            "BACKSPACE_DELETE" -> onSymbolSelected("BACKSPACE")
                                            "LEFT_ARROW" -> onSymbolSelected("LEFT")
                                            "RIGHT_ARROW" -> onSymbolSelected("RIGHT")
                                            "RETURN_ENTER" -> onSymbolSelected("ENTER")
                                            else -> onSymbolSelected(actionName)
                                        }
                                    }
                                    org.geogebra.keyboard.base.ActionType.INPUT -> {
                                        // Map internal names to LaTeX
                                        val latex = when (actionName) {
                                            "sin" -> "\\sin("
                                            "cos" -> "\\cos("
                                            "tan" -> "\\tan("
                                            "asin" -> "\\arcsin("
                                            "acos" -> "\\arccos("
                                            "atan" -> "\\arctan("
                                            "log10" -> "\\log_{10}("
                                            "ln" -> "\\ln("
                                            "sqrt", "ROOT" -> "\\sqrt{"
                                            "POWA2" -> "^2"
                                            "POWAB" -> "^"
                                            "pi", "PI" -> "\\pi"
                                            "euler", "EULER" -> "e"
                                            "GEQ" -> "\\ge"
                                            "LEQ" -> "\\le"
                                            "NOT_EQUAL_TO" -> "\\ne"
                                            "infinity", "INFINITY" -> "\\infty"
                                            "abs", "ABS" -> "| "
                                            "degree", "DEGREE" -> "^{\\circ}"
                                            else -> actionName
                                        }
                                        onSymbolSelected(latex)
                                    }
                                    else -> onSymbolSelected(actionName)
                                }
                            },
                            modifier = Modifier
                                .weight(weight)
                                .height(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (button.background) {
                                    org.geogebra.keyboard.base.Background.STANDARD -> Color.White
                                    org.geogebra.keyboard.base.Background.FUNCTIONAL -> Color(0xFFDADCE0)
                                    else -> Color.White
                                },
                                contentColor = Color.Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            val label = getButtonLabel(button)
                            Text(
                                text = label,
                                fontSize = if (label.length > 2) 14.sp else 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getButtonLabel(button: org.geogebra.keyboard.base.Button): String {
    return when (button.resourceType) {
        ResourceType.TEXT -> button.resourceName
        ResourceType.DEFINED_CONSTANT -> {
            when (button.resourceName) {
                "BACKSPACE_DELETE" -> "⌫"
                "RETURN_ENTER" -> "↵"
                "LEFT_ARROW" -> "←"
                "RIGHT_ARROW" -> "→"
                "POWA2" -> "x²"
                "POWAB" -> "xⁿ"
                "ROOT" -> "√"
                "FRACTION" -> "÷"
                "PI" -> "π"
                "EULER" -> "e"
                "GEQ" -> "≥"
                "LEQ" -> "≤"
                "NOT_EQUAL_TO" -> "≠"
                "INFINITY" -> "∞"
                "ABS" -> "|x|"
                "DEGREE" -> "°"
                else -> button.resourceName
            }
        }
        ResourceType.TRANSLATION_MENU_KEY, ResourceType.TRANSLATION_COMMAND_KEY -> {
            // Map common translation keys
            when (button.resourceName) {
                "asin" -> "sin⁻¹"
                "acos" -> "cos⁻¹"
                "atan" -> "tan⁻¹"
                "log10" -> "log₁₀"
                else -> button.resourceName
            }
        }
        else -> button.resourceName
    }
}
