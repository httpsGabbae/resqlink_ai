package com.resqlink.ai.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.ui.emergency.*

/**
 * =========================================================================================
 * TRUSTED CONTACTS SCREEN
 * =========================================================================================
 * Educational Overview:
 * This screen manages the user's circle of trusted emergency contacts (ICE - In Case of
 * Emergency). In safety-critical mobile design, trusted contacts play a vital role in
 * "Fail-Soft" protection:
 *
 * 1. UNVERIFIED INCAPACITATION ESCALATION:
 *    When the AI voice detector or crash sensors identify suspected distress, a 10-second
 *    audible/haptic countdown begins. If the user does NOT cancel it, the system does NOT
 *    auto-call public 911 (which would risk false-alarm swamping of emergency dispatchers).
 *    Instead, an unverified offline SMS alert containing the user's exact GPS coordinates
 *    is sent directly to these configured trusted contacts.
 *
 * 2. 100% OFFLINE PERSISTENCE:
 *    All contact records are stored locally in the device's encrypted SharedPreferences.
 *    No cloud database or external sync is required.
 * =========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: EmergencyViewModel,
    modifier: Modifier = Modifier
) {
    val trustedContactsList by viewModel.trustedContacts.collectAsState()
    var isAddContactDialogVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header: Title, safety subtitle, and Add Contact action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trusted Contacts",
                        fontWeight = FontWeight.Black,
                        color = TextPearl,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Recipients for unverified incapacitation SMS alerts.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = { isAddContactDialogVisible = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safety Protocol Explainer Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NavyCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EmeraldSafe,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fail-Soft Protection: If distress is detected and the 10-second countdown times out without response, an unverified offline SMS with your GPS coordinates and nearest hotline is sent to these contacts.",
                        color = TextPearl,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of trusted contacts or empty-state guidance
            if (trustedContactsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trusted contacts added.\nTap + to add family or guardians.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trustedContactsList, key = { it.id }) { contactItem ->
                        ContactCard(
                            contact = contactItem,
                            onDelete = { viewModel.removeTrustedContact(contactItem.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to input a new trusted contact
    if (isAddContactDialogVisible) {
        AddContactDialog(
            onDismiss = { isAddContactDialogVisible = false },
            onAdd = { contactName, contactPhoneNumber, contactRelationship ->
                viewModel.addTrustedContact(contactName, contactPhoneNumber, contactRelationship)
                isAddContactDialogVisible = false
            }
        )
    }
}

/**
 * Visual card displaying a trusted contact's identity, relationship, and mobile number.
 */
@Composable
fun ContactCard(
    contact: TrustedContact,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CrimsonPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        color = TextPearl,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${contact.relationship} • ${contact.phoneNumber}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Contact",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog for adding a new emergency contact with name, phone, and family relationship.
 */
@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, relationship: String) -> Unit
) {
    var contactNameInput by remember { mutableStateOf("") }
    var contactPhoneNumberInput by remember { mutableStateOf("") }
    var contactRelationshipInput by remember { mutableStateOf("Family") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = TextPearl,
        textContentColor = TextMuted,
        title = { Text("Add Trusted Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = contactNameInput,
                    onValueChange = { contactNameInput = it },
                    label = { Text("Contact Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPearl,
                        unfocusedTextColor = TextPearl,
                        focusedBorderColor = CrimsonGlow,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactPhoneNumberInput,
                    onValueChange = { contactPhoneNumberInput = it },
                    label = { Text("Mobile Number (e.g. 0917xxxxxxx)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPearl,
                        unfocusedTextColor = TextPearl,
                        focusedBorderColor = CrimsonGlow,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactRelationshipInput,
                    onValueChange = { contactRelationshipInput = it },
                    label = { Text("Relationship (e.g. Spouse, Parent, Friend)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPearl,
                        unfocusedTextColor = TextPearl,
                        focusedBorderColor = CrimsonGlow,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contactNameInput.isNotBlank() && contactPhoneNumberInput.isNotBlank()) {
                        onAdd(contactNameInput, contactPhoneNumberInput, contactRelationshipInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Save Contact", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
