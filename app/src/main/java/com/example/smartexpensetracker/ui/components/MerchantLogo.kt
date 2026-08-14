package com.example.smartexpensetracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.R

@Composable
fun MerchantLogo(
    merchant: String,
    categoryEmoji: String,
    isIncome: Boolean,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val upper = merchant.uppercase()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Official Bank Logos
            upper.contains("TMB") || upper.contains("TAMILNAD") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_tmb),
                    contentDescription = "TMB Bank",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("SBI") || upper.contains("STATE BANK") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_sbi),
                    contentDescription = "SBI",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("HDFC") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_hdfc),
                    contentDescription = "HDFC",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("ICICI") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_icici),
                    contentDescription = "ICICI",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("AXIS") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_axis),
                    contentDescription = "Axis Bank",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("KOTAK") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_kotak),
                    contentDescription = "Kotak Bank",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("BARODA") || upper.contains("BOB") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_bob),
                    contentDescription = "Bank of Baroda",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("CANARA") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_canara),
                    contentDescription = "Canara Bank",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("PNB") || upper.contains("PUNJAB") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_pnb),
                    contentDescription = "PNB",
                    modifier = Modifier.fillMaxSize()
                )
            }
            upper.contains("PAYTM") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_bank_paytm),
                    contentDescription = "Paytm Bank",
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 1. Swiggy (Iconic Swiggy Orange)
            upper.contains("SWIGGY") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFC8019)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "S",
                            fontSize = (size.value * 0.45f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // 2. Zomato (Iconic Zomato Crimson Red)
            upper.contains("ZOMATO") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFCB202D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "zomato",
                        fontSize = (size.value * 0.22f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // 3. Paytm (Iconic Paytm Blue & Cyan)
            upper.contains("PAYTM") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF002E6E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "paytm",
                            fontSize = (size.value * 0.24f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00BAF2)
                        )
                    }
                }
            }

            // 4. PhonePe (Iconic PhonePe Indigo Purple)
            upper.contains("PHONEPE") || upper.contains("YBL") || upper.contains("IBL") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF5F259F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "पे",
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 5. Google Pay / GPay (Google Sky Blue Accent)
            upper.contains("GPAY") || upper.contains("GOOGLE PAY") || upper.contains("OKAXIS") || upper.contains("OKHDFCBANK") || upper.contains("OKSBI") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GPay",
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // 6. BharatPe (Navy & Cyan)
            upper.contains("BHARATPE") || upper.contains("FBPE") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BharatPe",
                        fontSize = (size.value * 0.18f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00D2D3)
                    )
                }
            }

            // 7. Amazon (Dark Charcoal & Amazon Amber)
            upper.contains("AMAZON") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF232F3E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "amazon",
                            fontSize = (size.value * 0.20f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF9900)
                        )
                    }
                }
            }

            // 8. Flipkart (Royal Blue & Yellow)
            upper.contains("FLIPKART") || upper.contains("EKART") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2874F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "fk",
                        fontSize = (size.value * 0.40f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE500)
                    )
                }
            }

            // 9. Blinkit (Yellow & Green)
            upper.contains("BLINKIT") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8CB46)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "blinkit",
                        fontSize = (size.value * 0.20f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0C831F)
                    )
                }
            }

            // 10. Zepto (Deep Purple & Neon Pink)
            upper.contains("ZEPTO") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF3B0066)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "zepto",
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF3269)
                    )
                }
            }

            // 11. Uber (Jet Black & Crisp White)
            upper.contains("UBER") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Uber",
                        fontSize = (size.value * 0.28f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // 12. Ola (Lime Green)
            upper.contains("OLA") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF00D775)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OLA",
                        fontSize = (size.value * 0.30f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            // 13. Netflix (Black & Red)
            upper.contains("NETFLIX") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141414)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        fontSize = (size.value * 0.45f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE50914)
                    )
                }
            }

            // 14. Spotify (Black & Vibrant Green)
            upper.contains("SPOTIFY") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF191414)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Spotify",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            // 15. Karuppasamy Pandiyan / Personal Income / Salary
            upper.contains("KARUPPASAMY") || isIncome -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00796B), Color(0xFF00B894))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Income",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            // 16. TMB / Bank Transfer / NEFT / IMPS
            upper.contains("TMB") || upper.contains("BANK") || upper.contains("NEFT") || upper.contains("IMPS") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F2027), Color(0xFF203A43))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            // Fallback: Category Emoji on styled container
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryEmoji,
                        fontSize = (size.value * 0.44f).sp
                    )
                }
            }
        }
    }
}
