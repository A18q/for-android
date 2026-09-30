package chat.stoat.screens.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.composables.generic.AnyLink
import chat.stoat.composables.generic.Weblink
import chat.stoat.core.model.data.STOAT_MARKETING
import com.chuckerteam.chucker.api.Chucker

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginGreetingScreen(navController: NavController) {
    val context = LocalContext.current
    var catTaps by remember { mutableIntStateOf(0) }
    var showBoringButton by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 20.dp, horizontal = 24.dp)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.stoat_logo_white),
                contentDescription = "Stoat",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(80.dp)
                    .combinedClickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = null,
                        onClick = {
                            if (catTaps < 9) {
                                catTaps++
                            } else {
                                Toast.makeText(context, "🐈", Toast.LENGTH_SHORT).show()
                                catTaps = 0
                            }
                        },
                        onLongClick = {
                            if (BuildConfig.DEBUG) showBoringButton = !showBoringButton
                        }
                    )
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome back!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2F3F5),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Stoat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF949BA4),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("login/login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_login_page_button"),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2))
            ) {
                Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("register/greeting") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_signup_page_button"),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF383A40),
                    contentColor = Color(0xFFDBDEE1)
                )
            ) {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(showBoringButton) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { navController.navigate("login2/init") }) {
                    Text("Try new login experience (beta)", color = Color(0xFFDBDEE1))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 12.sp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Weblink(text = stringResource(R.string.terms_of_service), url = "$STOAT_MARKETING/terms")
                    Text(" · ")
                    Weblink(text = stringResource(R.string.privacy_policy), url = "$STOAT_MARKETING/privacy")
                    Text(" · ")
                    Weblink(text = stringResource(R.string.community_guidelines), url = "$STOAT_MARKETING/aup")
                }
                
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AnyLink(
                        text = "Debug: Chucker",
                        action = {
                            Chucker.getLaunchIntent(context).apply {
                                context.startActivity(this)
                            }
                        }
                    )
                }
            }
        }
    }
}
