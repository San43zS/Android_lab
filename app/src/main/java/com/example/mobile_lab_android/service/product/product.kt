package com.example.mobile_lab_android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_lab_android.models.ProductModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_lab_android.databinding.ItemProductBinding
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities


class ProductFactory(private val authViewModel: Auth, private val context: Context, private val onlyFavorite: Boolean = false) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(Product::class.java)) {
            return Product(authViewModel, context, onlyFavorite) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ProductAdapter(
    private val onFavoriteClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var products: List<ProductModel> = emptyList()

    fun submitList(newList: List<ProductModel>) {
        products = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductModel) {
            binding.productName.text = product.name

            binding.favoriteIcon.setImageResource(
                if (product.isFavorite == true)
                    R.drawable.ic_star_filled
                else
                    R.drawable.ic_star_border
            )

            binding.favoriteIcon.setOnClickListener {
                onFavoriteClick(product)
            }

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("product_name", product.name)
                    putExtra("product_description", product.description)
                    putStringArrayListExtra("product_images", ArrayList(product.images))
                }
                context.startActivity(intent)
            }
        }
    }
}

class Product(private val authViewModel: Auth, private val context: Context, private val onlyFavorite: Boolean = false) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("product_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _products = MutableLiveData<List<ProductModel>>(emptyList())
    val products: LiveData<List<ProductModel>> get() = _products

    private val _filteredProducts = MutableLiveData<List<ProductModel>>(emptyList())
    val filteredProducts: LiveData<List<ProductModel>> = _filteredProducts

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadProducts()
    }

    fun loadProducts() {
        if (_isLoading.value == true) return
        _isLoading.value = true

        if (!checkForInternet(context)) {
            val cachedProductsJson = sharedPreferences.getString("cached_products", null)
            if (cachedProductsJson != null) {
                val type = object : TypeToken<List<ProductModel>>() {}.type
                val cachedProducts = gson.fromJson<List<ProductModel>>(cachedProductsJson, type)
                _products.value = cachedProducts
                _filteredProducts.value =
                    if (onlyFavorite) cachedProducts.filter { it.isFavorite } else cachedProducts
                _isLoading.value = false
                return
            }
        }

        val userId = authViewModel.userId.value ?: return

        viewModelScope.launch {
            try {
                val favoriteProductIds = getFavoriteProductIds(userId)
                val snapshot = db.collection("products").orderBy("name").limit(30).get().await()

                if (snapshot.isEmpty) {
                    Log.d("ProductViewModel", "No products found")
                } else {
                    Log.d("ProductViewModel", "Loaded products from Firebase")
                }

                val newProducts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.copy(
                        id = doc.id,
                        isFavorite = favoriteProductIds.contains(doc.id)
                    )
                }

                sharedPreferences.edit().putString("cached_products", gson.toJson(newProducts)).apply()

                Log.d("ProductViewModel", "Loaded products: ${newProducts.size}")
                _products.value = newProducts
                _filteredProducts.value = if (onlyFavorite) newProducts.filter { it.isFavorite } else newProducts
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error loading products", e)
                _errorMessage.value = "Ошибка загрузки: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getFavoriteProductIds(userId: String): List<String> {
        return try {
            db.collection("users").document(userId).collection("favorites").get().await()
                .documents.map { it.id }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки избранного: ${e.localizedMessage}"
            emptyList()
        }
    }

    fun toggleFavorite(product: ProductModel) {
        val userId = authViewModel.userId.value ?: return

        viewModelScope.launch {
            val favoriteRef = db.collection("users").document(userId).collection("favorites").document(product.id ?: return@launch)

            try {
                val document = favoriteRef.get().await()
                if (document.exists()) {
                    favoriteRef.delete().await()
                    updateFavoriteStatus(product.id, false)
                } else {
                    favoriteRef.set(mapOf("name" to product.name)).await()
                    updateFavoriteStatus(product.id, true)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления избранного: ${e.localizedMessage}"
            }
        }
    }

    private fun updateFavoriteStatus(productId: String?, isFavorite: Boolean) {
        if (productId == null) return
        _products.value?.let { products ->
            _products.value = products.map { if (it.id == productId) it.copy(isFavorite = isFavorite) else it }
            _filteredProducts.value = products
        }
    }

    fun searchProducts(query: String) {
        _products.value?.let { products ->
            _filteredProducts.value = if (query.isEmpty()) products else {
                products.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val network = connectivityManager.activeNetwork ?: return false

            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {

                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true


                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}