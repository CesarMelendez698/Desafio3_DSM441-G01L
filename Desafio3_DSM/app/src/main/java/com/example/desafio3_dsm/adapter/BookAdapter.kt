package com.example.desafio3_dsm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio3_dsm.R
import com.example.desafio3_dsm.databinding.ItemBookBinding
import com.example.desafio3_dsm.model.Book
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookAdapter(
    private var books: List<Book>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    fun updateBooks(newBooks: List<Book>) {
        this.books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            val context = itemView.context

            binding.tvBookTitle.text = book.title
            binding.tvBookAuthor.text = book.author
            binding.tvBookGenre.text = book.genre

            // Estilo por género
            when (book.genre) {
                "Novela" -> {
                    binding.layoutBookSpine.setBackgroundColor(ContextCompat.getColor(context, R.color.genre_novel_bg))
                    binding.ivGenreIcon.setImageResource(R.drawable.ic_genre_novel)
                    binding.ivGenreIcon.setColorFilter(ContextCompat.getColor(context, R.color.genre_novel))
                    binding.tvBookGenre.setTextColor(ContextCompat.getColor(context, R.color.genre_novel))
                    binding.tvBookGenre.backgroundTintList = ContextCompat.getColorStateList(context, R.color.genre_novel_bg)
                }
                "Ciencia ficción" -> {
                    binding.layoutBookSpine.setBackgroundColor(ContextCompat.getColor(context, R.color.genre_scifi_bg))
                    binding.ivGenreIcon.setImageResource(R.drawable.ic_genre_scifi)
                    binding.ivGenreIcon.setColorFilter(ContextCompat.getColor(context, R.color.genre_scifi))
                    binding.tvBookGenre.setTextColor(ContextCompat.getColor(context, R.color.genre_scifi))
                    binding.tvBookGenre.backgroundTintList = ContextCompat.getColorStateList(context, R.color.genre_scifi_bg)
                }
                "Historia" -> {
                    binding.layoutBookSpine.setBackgroundColor(ContextCompat.getColor(context, R.color.genre_history_bg))
                    binding.ivGenreIcon.setImageResource(R.drawable.ic_genre_history)
                    binding.ivGenreIcon.setColorFilter(ContextCompat.getColor(context, R.color.genre_history))
                    binding.tvBookGenre.setTextColor(ContextCompat.getColor(context, R.color.genre_history))
                    binding.tvBookGenre.backgroundTintList = ContextCompat.getColorStateList(context, R.color.genre_history_bg)
                }
                "Desarrollo personal" -> {
                    binding.layoutBookSpine.setBackgroundColor(ContextCompat.getColor(context, R.color.genre_selfhelp_bg))
                    binding.ivGenreIcon.setImageResource(R.drawable.ic_genre_selfhelp)
                    binding.ivGenreIcon.setColorFilter(ContextCompat.getColor(context, R.color.genre_selfhelp))
                    binding.tvBookGenre.setTextColor(ContextCompat.getColor(context, R.color.genre_selfhelp))
                    binding.tvBookGenre.backgroundTintList = ContextCompat.getColorStateList(context, R.color.genre_selfhelp_bg)
                }
                else -> {
                    binding.layoutBookSpine.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_accent_light))
                    binding.ivGenreIcon.setImageResource(R.drawable.ic_book)
                    binding.ivGenreIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_accent))
                    binding.tvBookGenre.setTextColor(ContextCompat.getColor(context, R.color.primary_accent))
                    binding.tvBookGenre.backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_accent_light)
                }
            }

            // Estado y color
            binding.tvBookStatus.text = book.status
            when (book.status) {
                "Terminado" -> {
                    binding.tvBookStatus.setBackgroundResource(R.drawable.badge_status_finished)
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.status_finished))
                }
                "Leyendo" -> {
                    binding.tvBookStatus.setBackgroundResource(R.drawable.badge_status_reading)
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.status_reading))
                }
                else -> {
                    binding.tvBookStatus.setBackgroundResource(R.drawable.badge_status_to_read)
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.status_to_read))
                }
            }

            // Progreso
            val percentage = book.progressPercentage
            binding.tvPagesProgress.text = "${book.readPages} de ${book.totalPages} págs"
            binding.tvPercentage.text = "$percentage%"
            binding.pbBookProgress.progress = percentage

            // Calificación (solo si el libro está terminado)
            if (book.isFinished) {
                binding.layoutRating.visibility = View.VISIBLE
                binding.tvRatingText.text = "${String.format(Locale.getDefault(), "%.1f", book.rating)} ★"
                if (book.finishDate != null && book.finishDate!! > 0L) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvFinishDate.text = "• ${sdf.format(Date(book.finishDate!!))}"
                    binding.tvFinishDate.visibility = View.VISIBLE
                } else {
                    binding.tvFinishDate.visibility = View.GONE
                }
            } else {
                binding.layoutRating.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onBookClick(book)
            }
        }
    }
}
