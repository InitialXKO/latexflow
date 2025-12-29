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
                                when (button.primaryActionType) {
                                    org.geogebra.keyboard.base.ActionType.CUSTOM -> {

                                        when (button.resourceName) {
                                            "BACKSPACE_DELETE" -> onSymbolSelected("BACKSPACE")
                                            "LEFT_ARROW" -> onSymbolSelected("LEFT")
                                            "RIGHT_ARROW" -> onSymbolSelected("RIGHT")
                                            "RETURN_ENTER" -> onSymbolSelected("ENTER")
                                        }
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
                                containerColor = if (button.background == org.geogebra.keyboard.base.Background.STANDARD) Color.White else Color(0xFFDADCE0),
                                contentColor = Color.Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = getButtonLabel(button),
                                fontSize = if (button.resourceType == ResourceType.TEXT) 18.sp else 14.sp
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
            // Map GeoGebra constants to readable labels if needed
            when (button.resourceName) {
                "BACKSPACE_DELETE" -> "⌫"
                "RETURN_ENTER" -> "↵"
                "LEFT_ARROW" -> "←"
                "RIGHT_ARROW" -> "→"
                "POWA2" -> "x²"
                "POWAB" -> "xⁿ"
                "ROOT" -> "√"
                "FRACTION" -> "÷"
                else -> button.resourceName
            }
        }
        else -> button.resourceName
    }
}
