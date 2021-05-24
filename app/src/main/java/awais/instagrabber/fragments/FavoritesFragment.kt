package awais.instagrabber.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import awais.instagrabber.R
import awais.instagrabber.adapters.FavoritesAdapter
import awais.instagrabber.databinding.FragmentFavoritesBinding
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.db.repositories.RepositoryCallback
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.FavoritesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FavoritesFragment : Fragment() {
    private var shouldRefresh = true

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var root: RecyclerView
    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var adapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context ?: return
        favoriteRepository = FavoriteRepository.getInstance(FavoriteDataSource.getInstance(context))
        favoritesViewModel = ViewModelProvider(this).get(FavoritesViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (this::root.isInitialized) {
            shouldRefresh = false
            return root
        }
        binding = FragmentFavoritesBinding.inflate(layoutInflater)
        root = binding.root
        binding.favoriteList.layoutManager = LinearLayoutManager(context)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldRefresh) return
        init()
        shouldRefresh = false
    }

    override fun onResume() {
        super.onResume()
        if (!this::adapter.isInitialized) return
        // refresh list every time in onViewStateRestored since it is cheaper than implementing pull down to refresh
        favoritesViewModel.list.observe(viewLifecycleOwner, { list: List<Favorite?>? -> adapter.submitList(list) })
        favoriteRepository.getAllFavorites(object : RepositoryCallback<List<Favorite>> {
            override fun onSuccess(favorites: List<Favorite>) {
                favoritesViewModel.list.postValue(favorites)
            }

            override fun onDataNotAvailable() {}
        })
    }

    private fun init() {
        adapter = FavoritesAdapter({ model: Favorite ->
            when (model.type) {
                FavoriteType.USER -> {
                    val username = model.query
                    // Log.d(TAG, "username: " + username);
                    val navController = NavHostFragment.findNavController(this)
                    val bundle = Bundle()
                    bundle.putString("username", "@$username")
                    navController.navigate(R.id.action_global_profileFragment, bundle)
                }
                FavoriteType.LOCATION -> {
                    val locationId = model.query
                    // Log.d(TAG, "locationId: " + locationId);
                    val navController = NavHostFragment.findNavController(this)
                    val bundle = Bundle()
                    try {
                        bundle.putLong("locationId", locationId.toLong())
                        navController.navigate(R.id.action_global_locationFragment, bundle)
                    } catch (e: Exception) {
                        Log.e(TAG, "init: ", e)
                    }
                }
                FavoriteType.HASHTAG -> {
                    val hashtag = model.query
                    // Log.d(TAG, "hashtag: " + hashtag);
                    val navController = NavHostFragment.findNavController(this)
                    val bundle = Bundle()
                    bundle.putString("hashtag", "#$hashtag")
                    navController.navigate(R.id.action_global_hashTagFragment, bundle)
                }
                else -> {
                }
            }
        }, { model: Favorite ->
            // delete
            val context = context ?: return@FavoritesAdapter false
            MaterialAlertDialogBuilder(context)
                .setMessage(getString(R.string.quick_access_confirm_delete, model.query))
                .setPositiveButton(R.string.yes) { d: DialogInterface, _: Int ->
                    favoriteRepository.deleteFavorite(model.query, model.type, object : RepositoryCallback<Void> {
                        override fun onSuccess(result: Void) {
                            d.dismiss()
                            favoriteRepository.getAllFavorites(object : RepositoryCallback<List<Favorite>> {
                                override fun onSuccess(result: List<Favorite>) {
                                    favoritesViewModel.list.postValue(result)
                                }

                                override fun onDataNotAvailable() {}
                            })
                        }

                        override fun onDataNotAvailable() {}
                    })
                }
                .setNegativeButton(R.string.no, null)
                .show()
            true
        })
        binding.favoriteList.adapter = adapter
    }
}