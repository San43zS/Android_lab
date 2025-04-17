package com.example.mobile_lab_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobile_lab_android.databinding.FragmentFavoritesProductsBinding

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesProductsBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: Auth by viewModels()
    private val productViewModel: Product by viewModels { ProductFactory(authViewModel, requireContext(), true) }
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesProductsBinding.inflate(inflater, container, false)

        setupRecyclerView()
        observeViewModel()
        setupSearchView()

        return binding.root
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            productViewModel.toggleFavorite(product)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun observeViewModel() {
        productViewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }

        productViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        productViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            binding.errorTextView.text = errorMessage
            binding.errorTextView.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productViewModel.searchProducts(newText.orEmpty())
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}