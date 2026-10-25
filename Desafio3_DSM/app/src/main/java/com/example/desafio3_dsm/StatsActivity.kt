package com.example.desafio3_dsm

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3_dsm.database.DatabaseHelper
import com.example.desafio3_dsm.databinding.ActivityStatsBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        loadStatistics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.tvStatsYearTitle.text = "Estadísticas del Año $currentYear"

        val stats = dbHelper.getStatsForYear(user.uid, currentYear)

        // 1. Conteo por género
        val totalFinished = stats.countNovel + stats.countSciFi + stats.countHistory + stats.countSelfHelp
        val maxCount = maxOf(1, totalFinished)

        binding.tvCountNovel.text = "${stats.countNovel} ${if (stats.countNovel == 1) "libro" else "libros"}"
        binding.pbNovel.progress = ((stats.countNovel.toDouble() / maxCount) * 100).toInt()

        binding.tvCountSciFi.text = "${stats.countSciFi} ${if (stats.countSciFi == 1) "libro" else "libros"}"
        binding.pbSciFi.progress = ((stats.countSciFi.toDouble() / maxCount) * 100).toInt()

        binding.tvCountHistory.text = "${stats.countHistory} ${if (stats.countHistory == 1) "libro" else "libros"}"
        binding.pbHistory.progress = ((stats.countHistory.toDouble() / maxCount) * 100).toInt()

        binding.tvCountSelfHelp.text = "${stats.countSelfHelp} ${if (stats.countSelfHelp == 1) "libro" else "libros"}"
        binding.pbSelfHelp.progress = ((stats.countSelfHelp.toDouble() / maxCount) * 100).toInt()

        // 2. Género más leído y total páginas leídas en el año
        if (stats.mostReadGenreCount > 0) {
            binding.tvTopGenre.text = stats.mostReadGenre
            binding.tvTopGenreCount.text = "${stats.mostReadGenreCount} ${if (stats.mostReadGenreCount == 1) "libro terminado" else "libros terminados"}"
        } else {
            binding.tvTopGenre.text = "Sin registros"
            binding.tvTopGenreCount.text = "0 libros terminados"
        }

        binding.tvTotalPagesYear.text = stats.totalPagesReadInYear.toString()

        // 3. Libro mejor calificado del año (desempate: terminado primero)
        val best = stats.bestRatedBook
        if (best != null) {
            binding.layoutBestBook.visibility = View.VISIBLE
            binding.tvNoBestBook.visibility = View.GONE

            binding.tvBestBookTitle.text = best.title
            binding.tvBestBookAuthor.text = "por ${best.author}"
            binding.tvBestBookGenre.text = best.genre
            binding.tvBestBookRating.text = "★ ${String.format(Locale.getDefault(), "%.1f", best.rating)}"

            val (textColor, bgColor) = when (best.genre) {
                "Novela" -> Pair(R.color.genre_novel, R.color.genre_novel_bg)
                "Ciencia ficción" -> Pair(R.color.genre_scifi, R.color.genre_scifi_bg)
                "Historia" -> Pair(R.color.genre_history, R.color.genre_history_bg)
                "Desarrollo personal" -> Pair(R.color.genre_selfhelp, R.color.genre_selfhelp_bg)
                else -> Pair(R.color.primary_accent, R.color.primary_accent_light)
            }
            binding.tvBestBookGenre.setTextColor(getColor(textColor))
            binding.tvBestBookGenre.backgroundTintList = getColorStateList(bgColor)

            if (best.finishDate != null && best.finishDate!! > 0L) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.tvBestBookDate.text = "Terminado el: ${sdf.format(Date(best.finishDate!!))}"
            } else {
                binding.tvBestBookDate.text = "Terminado en $currentYear"
            }
        } else {
            binding.layoutBestBook.visibility = View.GONE
            binding.tvNoBestBook.visibility = View.VISIBLE
        }
    }
}
