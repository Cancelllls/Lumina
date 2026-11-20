package com.cancellls.lumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.ui.glass.ListIconCatalog
import com.cancellls.lumina.ui.theme.LocalLuminaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEditorDialog(
    existing: ReminderListItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: Long) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val colors = LocalLuminaColors.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: ListIconCatalog.iconKeys.first()) }
    var colorHex by remember { mutableLongStateOf(existing?.colorHex ?: ListIconCatalog.colorHexes.first()) }

    val selectedColor = Color(colorHex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.sheetBg,
            contentColor = colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.rimTopLeft.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(selectedColor.copy(alpha = 0.24f))
                            .border(1.5.dp, selectedColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ListIconCatalog.iconForKey(iconKey),
                            contentDescription = null,
                            tint = selectedColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (existing == null) "New List" else "Edit List",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    placeholder = { Text("e.g. Fitness, Errands, Reading") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ListIconCatalog.colorHexes.forEach { hex ->
                        val c = Color(hex)
                        val isSelected = hex == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ListIconCatalog.iconKeys.forEach { key ->
                        val isSelected = key == iconKey
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) selectedColor.copy(alpha = 0.25f)
                                    else colors.glassSurfaceTop.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) selectedColor else colors.rimBottomRight,
                                    shape = CircleShape
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ListIconCatalog.iconForKey(key),
                                contentDescription = key,
                                tint = if (isSelected) selectedColor else colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existing != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(existing.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Delete")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onSave(name, iconKey, colorHex) },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
