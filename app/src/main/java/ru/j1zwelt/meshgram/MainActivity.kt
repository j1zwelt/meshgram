package ru.j1zwelt.meshgram

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.google.gson.Gson
import ru.j1zwelt.meshgram.mesh.MeshManager
import ru.j1zwelt.meshgram.model.Chat
import ru.j1zwelt.meshgram.model.Chat.Companion.toChatsList
import ru.j1zwelt.meshgram.model.Message
import ru.j1zwelt.meshgram.model.Message.Companion.toFormattedTime
import ru.j1zwelt.meshgram.model.User
import ru.j1zwelt.meshgram.model.User.Companion.toUser
import ru.j1zwelt.meshgram.ui.theme.MeshgramTheme

class MainActivity : ComponentActivity() {
    private lateinit var chatsList: SnapshotStateList<Chat>
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshgramTheme {
                val context = LocalContext.current
                sharedPref = remember {
                    context.getSharedPreferences("my_prefs", MODE_PRIVATE)
                }
                chatsList = remember {
                    val savedChatsJson = sharedPref.getString("saved_chats", "")
                    savedChatsJson.toChatsList()
                }

                var userInfo by remember {
                    mutableStateOf(
                        sharedPref.getString(
                            "my_profile", null
                        )
                    )
                }

                if (userInfo != null) MeshgramApp(sharedPref, chatsList)
                else Register(sharedPref) {
                    userInfo = it
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPref.edit(true) {
            putString("saved_chats", Gson().toJson(chatsList))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshgramApp(sharedPref: SharedPreferences, chatsList: SnapshotStateList<Chat>) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CHATS) }
    val context = LocalContext.current
    val mesh = remember { MeshManager(context) }

    var currentChat by remember { mutableStateOf<Chat?>(null) }

    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    LaunchedEffect(Unit) {
        enableLauncher.requestDiscoverable()
        mesh.startServer(chatsList, sharedPref) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableLauncher.launch(enableIntent)
        }
        mesh.findDevices(onTurnBluetooth = {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableLauncher.launch(enableIntent)
        }, onTurnLocation = {
            val enableIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            enableLauncher.launch(enableIntent)
        })
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon, contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it })
            }
        }) {
        when (currentDestination) {
            AppDestinations.CHATS -> {
                Chats(chatsList) { currentChat = it }
            }

            AppDestinations.PROFILE -> {
                Profile(sharedPref)
            }
        }
    }

    AnimatedContent(
        currentChat,
        transitionSpec = {
            if (targetState != null) {
                (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it }).togetherWith(
                    fadeOut(
                        tween(100)
                    )
                )
            } else {
                fadeIn(tween(100)).togetherWith(
                    fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it })
            }.using(
                SizeTransform(false) { _, _ -> SnapSpec() })
        },
    ) {
        if (it != null) {
            Chat(mesh, sharedPref, it) {
                currentChat = null
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat), PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chats(chats: SnapshotStateList<Chat>, onChatClick: (chat: Chat) -> Unit) {
    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    var showAddChatDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar({ Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = {
                enableLauncher.requestDiscoverable()
            }) {
                Icon(Icons.Default.Search, stringResource(R.string.start_discoverable))
            }
        })
    }, floatingActionButton = {
        FloatingActionButton({
            showAddChatDialog = true
        }) {
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

    if (showAddChatDialog) AddChat(chats) {
        showAddChatDialog = false
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Card(
        onClick,
        Modifier
            .fillMaxWidth()
            .padding(12.dp, 4.dp),
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
            UserImage(chat.user.name ?: chat.user.userName ?: "", 48.dp)

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text(
                        chat.user.name ?: "@${chat.user.userName}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        chat.messages.lastOrNull()?.time?.toFormattedTime() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    chat.messages.lastOrNull()?.text ?: stringResource(R.string.no_messages),
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
fun AddChat(chats: SnapshotStateList<Chat>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var userName by remember { mutableStateOf("") }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.create_new_chat),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text(stringResource(R.string.username)) },
                leadingIcon = {
                    Text(
                        "@",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                {
                    val chat = Chat(
                        User(null, userName.replace("@", "").trim()), mutableStateListOf()
                    )
                    chats.add(chat)
                    onDismiss()
                }, Modifier.fillMaxWidth(), enabled = userName.isNotEmpty()
            ) {
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}

@Composable
fun Profile(sharedPref: SharedPreferences) {
    val userInfo = sharedPref.getString("my_profile", null).toUser()

    var name by remember { mutableStateOf(userInfo.name ?: "") }
    var userName by remember { mutableStateOf(userInfo.userName ?: "") }
    var oldName by remember { mutableStateOf(userInfo.name ?: "") }
    var oldUserName by remember { mutableStateOf(userInfo.userName ?: "") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(48.dp, 147.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserImage(name, 96.dp)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text(stringResource(R.string.username)) },
            leadingIcon = {
                Text(
                    "@",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            {
                val user = User(name.trim(), userName.replace("@", "").trim())
                sharedPref.edit(true) {
                    putString("my_profile", Gson().toJson(user))
                }

                oldName = name.trim()
                oldUserName = userName.trim()
            },
            Modifier.fillMaxWidth(),
            enabled = (oldName != name || oldUserName != userName) && name.isNotEmpty() && userName.isNotEmpty()
        ) {
            Text(stringResource(R.string.save_changes))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chat(mesh: MeshManager, sharedPref: SharedPreferences, chat: Chat, onDismiss: () -> Unit) {
    val listState = rememberLazyListState(
        if (chat.messages.isNotEmpty()) chat.messages.lastIndex else 0
    )
    var textMessage by remember { mutableStateOf("") }

    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) listState.animateScrollToItem(chat.messages.lastIndex)
    }

    Scaffold(topBar = {
        TopAppBar({ Text(chat.user.name ?: "@${chat.user.userName}") }, actions = {
            IconButton(onClick = {
                // TODO: Меню действий
            }) {
                Icon(
                    Icons.Default.MoreVert, stringResource(R.string.more)
                )
            }
        }, navigationIcon = {
            IconButton(onClick = { onDismiss() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_to_chats)
                )
            }
        })
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    items(chat.messages) {
                        MessageItem(it)
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
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
                        val profile = sharedPref.getString("my_profile", "").toUser()
                        val message = Message(textMessage, true, profile, chat.user.userName)
                        mesh.sendMessage(message)
                        chat.messages.add(message)
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

    BackHandler {
        onDismiss()
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
                    message.time.toFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
        }
    }
}

@Composable
fun Register(sharedPref: SharedPreferences, onSaveJson: (json: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(48.dp, 147.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                { context ->
                    ImageView(context).apply {
                        setImageResource(R.mipmap.ic_launcher_round)
                    }
                }, Modifier.size(147.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.enter_your_details_to_start_chatting),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text(stringResource(R.string.username)) },
                leadingIcon = {
                    Text(
                        "@",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                {
                    val user = User(name.trim(), userName.replace("@", "").trim())
                    sharedPref.edit(true) {
                        putString("my_profile", Gson().toJson(user))
                    }
                    onSaveJson(Gson().toJson(user))
                }, Modifier.fillMaxWidth(), enabled = name.isNotEmpty() && userName.isNotEmpty()
            ) {
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}

@Composable
fun UserImage(userName: String, size: Dp) {
    Box(
        Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        Alignment.Center
    ) {
        Text(
            userName.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

fun ManagedActivityResultLauncher<Intent, ActivityResult>.requestDiscoverable() {
    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0)
    }
    this.launch(discoverableIntent)
}