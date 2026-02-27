package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.github.achmadqomarudin.composechart.line.LineChart
import com.github.achmadqomarudin.composechart.line.LineChartData
import com.github.achmadqomarudin.composechart.line.LineDataSet

val LocalDate.weekOfYear: Int
    get() {
        val weekFields = WeekFields.of(Locale.getDefault())
        return this.get(weekFields.weekOfWeekBasedYear())
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    FitnessTrackerApp(
                        modifier = Modifier.padding(it),
                        context = applicationContext
                    )
                }
            }
        }
    }
}

@Composable
fun FitnessTrackerApp(modifier: Modifier = Modifier, context: Context) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var fitnessDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var masturbationDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    
    // 加载数据
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("fitness_tracker", Context.MODE_PRIVATE)
        val fitnessString = prefs.getString("fitness_dates", "")
        val masturbationString = prefs.getString("masturbation_dates", "")
        
        fitnessDates = fitnessString?.split(",")?.filter { it.isNotEmpty() }?.map { LocalDate.parse(it) }?.toSet() ?: emptySet()
        masturbationDates = masturbationString?.split(",")?.filter { it.isNotEmpty() }?.map { LocalDate.parse(it) }?.toSet() ?: emptySet()
    }
    
    // 保存数据
    LaunchedEffect(fitnessDates, masturbationDates) {
        val prefs = context.getSharedPreferences("fitness_tracker", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("fitness_dates", fitnessDates.joinToString(","))
        editor.putString("masturbation_dates", masturbationDates.joinToString(","))
        editor.apply()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "健身和自慰频率记录",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val currentMonth = remember { YearMonth.now() }
        val startMonth = remember { currentMonth.minusMonths(12) }
        val endMonth = remember { currentMonth.plusMonths(12) }
        val calendarState = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth
        )
        
        HorizontalCalendar(
            state = calendarState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            dayContent = {
                DayView(
                    day = it,
                    isSelected = selectedDate == it.date,
                    isFitness = fitnessDates.contains(it.date),
                    isMasturbation = masturbationDates.contains(it.date),
                    onClick = {
                        selectedDate = it.date
                    }
                )
            },
            monthHeader = { month ->
                MonthHeader(month = month)
            }
        )
        
        selectedDate?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        fitnessDates = if (fitnessDates.contains(it)) {
                            fitnessDates - it
                        } else {
                            fitnessDates + it
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fitnessDates.contains(it)) Color.Green else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("标记健身")
                }
                Button(
                    onClick = {
                        masturbationDates = if (masturbationDates.contains(it)) {
                            masturbationDates - it
                        } else {
                            masturbationDates + it
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (masturbationDates.contains(it)) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("标记自慰")
                }
            }
        }
        
        StatisticsSection(
            fitnessDates = fitnessDates,
            masturbationDates = masturbationDates
        )
    }
}

@Composable
fun DayView(
    day: CalendarDay,
    isSelected: Boolean,
    isFitness: Boolean,
    isMasturbation: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isFitness -> Color.Green
                        isMasturbation -> Color.Red
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
        )
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isFitness || isMasturbation -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun MonthHeader(month: YearMonth) {
    Text(
        text = "${month.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${month.year}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun StatisticsSection(
    fitnessDates: Set<LocalDate>,
    masturbationDates: Set<LocalDate>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val currentWeek = LocalDate.now().let { it.minusDays(it.dayOfWeek.value.toLong() - 1) }
        val currentMonth = YearMonth.now()
        val currentYear = currentMonth.year
        
        val weeklyFitness = fitnessDates.count { it.isAfter(currentWeek.minusDays(1)) }
        val weeklyMasturbation = masturbationDates.count { it.isAfter(currentWeek.minusDays(1)) }
        
        val monthlyFitness = fitnessDates.count { YearMonth.from(it) == currentMonth }
        val monthlyMasturbation = masturbationDates.count { YearMonth.from(it) == currentMonth }
        
        val yearlyFitness = fitnessDates.count { it.year == currentYear }
        val yearlyMasturbation = masturbationDates.count { it.year == currentYear }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(title = "本周", fitness = weeklyFitness, masturbation = weeklyMasturbation)
            StatCard(title = "本月", fitness = monthlyFitness, masturbation = monthlyMasturbation)
            StatCard(title = "本年", fitness = yearlyFitness, masturbation = yearlyMasturbation)
        }
        
        Text(
            text = "频率趋势",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        val last6Weeks = mutableListOf<LocalDate>()
        for (i in 5 downTo 0) {
            last6Weeks.add(currentWeek.minusWeeks(i.toLong()))
        }
        
        val weeklyFitnessData = last6Weeks.map { weekStart ->
            fitnessDates.count { it.isAfter(weekStart.minusDays(1)) && it.isBefore(weekStart.plusWeeks(1)) }
        }
        
        val weeklyMasturbationData = last6Weeks.map { weekStart ->
            masturbationDates.count { it.isAfter(weekStart.minusDays(1)) && it.isBefore(weekStart.plusWeeks(1)) }
        }
        
        val weekLabels = last6Weeks.map { "W${it.weekOfYear}" }
        
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            data = LineChartData(
                lineDataSets = listOf(
                    LineDataSet(
                        data = weeklyFitnessData.map { it.toFloat() },
                        color = Color.Green,
                        label = "健身",
                        lineWidth = 2f
                    ),
                    LineDataSet(
                        data = weeklyMasturbationData.map { it.toFloat() },
                        color = Color.Red,
                        label = "自慰",
                        lineWidth = 2f
                    )
                ),
                xAxisLabels = weekLabels
            )
        )
    }
}

@Composable
fun StatCard(
    title: String,
    fitness: Int,
    masturbation: Int
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp),
        elevation = CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "健身: $fitness", color = Color.Green)
            Text(text = "自慰: $masturbation", color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessTrackerAppPreview() {
    MyApplicationTheme {
        // 预览时使用mock context
        val mockContext = androidx.compose.ui.platform.LocalContext.current
        FitnessTrackerApp(context = mockContext)
    }
}