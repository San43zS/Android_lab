package com.example.mobile_lab_android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mobile_lab_android.databinding.ActivityProductDetailBinding
import com.example.mobile_lab_android.models.ProductModel
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import androidx.activity.viewModels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.set
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_lab_android.databinding.ItemImageSliderBinding
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class ProductDetailActivity : AppCompatActivity() {
    private var currentRating = 5
    private lateinit var binding: ActivityProductDetailBinding

    private val authViewModel: Auth by viewModels()
    val context: Context = this
    private val productViewModel: Product by viewModels { ProductFactory(authViewModel = authViewModel, context) }

    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stars = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                currentRating = index + 1
                updateStars(stars, currentRating)
            }
        }

        updateStars(stars, currentRating)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val productName = intent.getStringExtra("product_name") ?: "Неизвестно"
        val productDescription = intent.getStringExtra("product_description") ?: "Описание отсутствует"
        val productImages = intent.getStringArrayListExtra("product_images") ?: ArrayList()
        productId = intent.getStringExtra("product_id") // Убедитесь, что передаете ID товара

        binding.tvProductName.text = productName
        binding.tvProductDescription.text = productDescription

        setupImageSlider(productImages)

        loadProductFavoriteStatus()

        binding.btnFavorite.setOnClickListener {
            productId?.let { id ->
                productViewModel.toggleFavorite(ProductModel(id = id, name = productName, description = productDescription))

                binding.btnFavorite.text = if (binding.btnFavorite.text == "Добавить в избранное") {
                    "Удалить из избранного"
                } else {
                    "Добавить в избранное"
                }
            }
        }

        productViewModel.products.observe(this, { products ->
            val product = products.find { it.id == productId }
            product?.let {
                if (it.isFavorite) {
                    binding.btnFavorite.text = "Удалить из избранного"
                } else {
                    binding.btnFavorite.text = "Добавить в избранное"
                }
            }
        })

        binding.btnSubmitReview.setOnClickListener {
            val reviewText = binding.etReview.text.toString().trim()

            if (reviewText.isNotEmpty()) {
                productId?.let{ id ->
                    addReview(id, currentRating, reviewText)
                    binding.etReview.setText("")
                    currentRating = 5
                    updateStars(stars, currentRating)
                    updateReviews()
                }

            } else {
                Toast.makeText(this, "Отзыв не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        updateReviews()
    }

    private fun updateStars(stars: List<ImageView>, rating: Int) {
        stars.forEachIndexed { index, imageView ->
            if (index < rating) {
                imageView.setImageResource(R.drawable.ic_star_filled)
            } else {
                imageView.setImageResource(R.drawable.ic_star_border)
            }
        }
    }


    private fun addReview(productId: String, rating: Int, comment: String) {
        val userId = authViewModel.userId.value
        if (userId == null) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        getUserName(userId) { userNameResp ->
            val userName = userNameResp ?: "Аноним"

            val reviewData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "rating" to rating,
                "comment" to comment,
                "timestamp" to System.currentTimeMillis()
            )

            val db = FirebaseFirestore.getInstance()

            db.collection("products")
                .document(productId)
                .collection("reviews")
                .add(reviewData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Отзыв успешно добавлен", Toast.LENGTH_SHORT).show()
                    updateReviews()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка добавления отзыва: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupImageSlider(images: List<String>) {
        val adapter = ImageSliderAdapter(images)
        binding.viewPager.adapter = adapter
    }

    private fun getUserName(userId: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userName = document.getString("name")
                callback(userName)
            }
            .addOnFailureListener { exception ->
                callback(null)
            }
    }

    private fun loadProductFavoriteStatus() {
        productId?.let {
            val favoriteProductIds = productViewModel.products.value?.filter { it.isFavorite }?.map { it.id }
            if (favoriteProductIds?.contains(it) == true) {
                binding.btnFavorite.text = "Удалить из избранного"
            } else {
                binding.btnFavorite.text = "Добавить в избранное"
            }
        }
    }

    private fun updateReviews() {
        //binding.progressBar.visibility = View.VISIBLE

        productId?.let { id ->
            // Получаем отзывы из Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("products")
                .document(id)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    // Преобразуем данные в список отзывов
                    val reviews = snapshot.documents.mapNotNull { document ->
                        Log.d("Firestore", "Document data: ${document.data}")
                        document.toObject(Review::class.java)?.copy(id = document.id)
                    }

                    //                   doc.toObject(ProductModel::class.java)?.copy(
                    //                        id = doc.id,
                    //                        isFavorite = favoriteProductIds.contains(doc.id)
                    //                    )

                    // Устанавливаем адаптер для RecyclerView
                    val reviewAdapter = ReviewAdapter(reviews)
                    binding.recyclerViewReviews.adapter = reviewAdapter

                    // Скрываем индикатор загрузки
                    //binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { exception ->
                    // Обработка ошибки
                    Toast.makeText(
                        context,
                        "Ошибка загрузки: ${exception.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Скрываем индикатор загрузки
                    //binding.progressBar.visibility = View.GONE
                }
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
}

class ImageSliderAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImageSliderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSliderViewHolder {
        val binding = ItemImageSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageSliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Picasso.get().load(imageUrl).into(holder.binding.imageView)
    }

    override fun getItemCount(): Int = imageUrls.size

    class ImageSliderViewHolder(val binding: ItemImageSliderBinding) : RecyclerView.ViewHolder(binding.root)
}

data class Review(
    @DocumentId val id: String? = null,
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    //val timestamp: Long = 0L
)
class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)

        // Добавляем ссылки на звёздочки
        private val star1: ImageView = itemView.findViewById(R.id.star1)
        private val star2: ImageView = itemView.findViewById(R.id.star2)
        private val star3: ImageView = itemView.findViewById(R.id.star3)
        private val star4: ImageView = itemView.findViewById(R.id.star4)
        private val star5: ImageView = itemView.findViewById(R.id.star5)

        fun bind(review: Review) {
            tvUserName.text = review.userName
            tvComment.text = review.comment

            // Обновляем звёздочки в зависимости от оценки
            setStars(review.rating)
        }

        // Функция для отображения звёздочек
        private fun setStars(rating: Int) {
            val stars = listOf(star1, star2, star3, star4, star5)

            // Обновляем каждую звёздочку в зависимости от оценки
            for (i in stars.indices) {
                if (i < rating) {
                    stars[i].setImageResource(R.drawable.ic_star_filled) // Закрашенная звезда
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_border) // Пустая звезда
                }
            }
        }
    }
}