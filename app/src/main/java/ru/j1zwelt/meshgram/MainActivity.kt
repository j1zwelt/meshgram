package ru.j1zwelt.meshgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.j1zwelt.meshgram.model.Chat
import ru.j1zwelt.meshgram.model.Message
import ru.j1zwelt.meshgram.ui.theme.MeshgramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshgramTheme {
                val chatsList = remember { mutableStateListOf<Chat>() }
                var currentChat by remember { mutableStateOf<Chat?>(null) }

                LaunchedEffect(Unit) {
                    if (chatsList.isEmpty()) {
                        chatsList.add(
                            Chat(
                                "Sergey", mutableStateListOf(
                                    Message("Привет!", false),
                                    Message("Как дела?", false),
                                    Message("Дарова!", true),
                                    Message("Все збс", true)
                                )
                            )
                        )
                        chatsList.add(
                            Chat(
                                "Damir", mutableStateListOf(
                                    Message("Дарова", false),
                                    Message("Как жизнь?", false),
                                    Message("Охаешачки", true),
                                    Message("Миша, все хуйня", true),
                                )
                            )
                        )
                    }
                }

                AnimatedContent(
                    currentChat, transitionSpec = {
                        if (targetState != null) {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it / 4 })
                        } else {
                            slideInHorizontally(initialOffsetX = { -it / 4 }) togetherWith slideOutHorizontally(
                                targetOffsetX = { it })
                        }
                    }) { chat ->
                    Box(Modifier.fillMaxSize()) {
                        Chats(chatsList) {
                            currentChat = it
                        }

                        if (chat != null) {
                            Chat(chat) {
                                currentChat = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chats(chats: SnapshotStateList<Chat>, onChatClick: (chat: Chat) -> Unit) {
    Scaffold(topBar = {
        TopAppBar({ Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.MoreVert, stringResource(R.string.more)
                )
            }
        })
    }, floatingActionButton = {
        FloatingActionButton({}) {
            Icon(Icons.Default.Add, stringResource(R.string.add))
        }
    }) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(chats) {
                ChatItem(it) {
                    onChatClick(it)
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Card(
        onClick,
        Modifier
            .fillMaxWidth()
            .padding(12.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                Alignment.Center
            ) {
                Text(
                    chat.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text(
                        chat.userName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        "16:45",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    chat.messages.lastOrNull()?.text ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chat(chat: Chat, onDismiss: () -> Unit) {
    BackHandler {
        onDismiss()
    }

    val dismissState = rememberSwipeToDismissBoxState()
    if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
        LaunchedEffect(Unit) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            onDismiss()
        }
    }

    var textMessage by remember { mutableStateOf("") }

    SwipeToDismissBox(
        dismissState, enableDismissFromEndToStart = false, backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }) {
        Scaffold(topBar = {
            TopAppBar({ Text(chat.userName) }, navigationIcon = {
                IconButton(onClick = { onDismiss() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_to_chats)
                    )
                }
            }, actions = {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.MoreVert, stringResource(R.string.more)
                    )
                }
            })
        }) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn {
                        items(chat.messages) {
                            MessageItem(it)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        textMessage,
                        onValueChange = { textMessage = it },
                        maxLines = 3,
                        placeholder = { Text(stringResource(R.string.message)) },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        {
                            chat.messages.add(Message(textMessage.trim(), true))
                            textMessage = ""
                        },
                        Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(52.dp),
                        enabled = !textMessage.isEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            stringResource(R.string.send),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    Box(Modifier.fillMaxWidth()) {
        Card(
            Modifier
                .align(if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(16.dp, 4.dp)
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(if (message.isFromMe) 0.15f else 0.2f)
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
            ) {
                Text(message.text)
                Text(
                    "16:45",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
        }
    }
}