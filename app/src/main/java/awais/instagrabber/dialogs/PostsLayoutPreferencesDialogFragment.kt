package awais.instagrabber.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import awais.instagrabber.R
import awais.instagrabber.databinding.DialogPostLayoutPreferencesBinding
import awais.instagrabber.models.PostsLayoutPreferences
import awais.instagrabber.models.PostsLayoutPreferences.PostsLayoutType
import awais.instagrabber.models.PostsLayoutPreferences.ProfilePicSize
import awais.instagrabber.utils.Utils
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PostsLayoutPreferencesDialogFragment(
    private val layoutPreferenceKey: String,
    private val onApplyListener: OnApplyListener
) : DialogFragment() {

    private lateinit var binding: DialogPostLayoutPreferencesBinding

    private val preferencesBuilder: PostsLayoutPreferences.Builder

    init {
        val preferences = PostsLayoutPreferences.fromJson(Utils.settingsHelper.getString(layoutPreferenceKey))
        preferencesBuilder = PostsLayoutPreferences.builder().mergeFrom(preferences)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPostLayoutPreferencesBinding.inflate(layoutInflater)
        init()
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.getRoot())
            .setPositiveButton(R.string.apply) { _: DialogInterface?, _: Int ->
                val preferences = preferencesBuilder.build()
                val json = preferences.json
                Utils.settingsHelper.putString(layoutPreferenceKey, json)
                onApplyListener.onApply(preferences)
            }
            .create()
    }

    private fun init() {
        initLayoutToggle()
        if (preferencesBuilder.type != PostsLayoutType.LINEAR) {
            initStaggeredOrGridOptions()
        }
    }

    private fun initStaggeredOrGridOptions() {
        initColCountToggle()
        initNamesToggle()
        initAvatarsToggle()
        initCornersToggle()
        initGapToggle()
    }

    private fun initLayoutToggle() {
        val selectedLayoutId = selectedLayoutId
        binding.layoutToggle.check(selectedLayoutId)
        if (selectedLayoutId == R.id.layout_linear) {
            binding.staggeredOrGridOptions.visibility = View.GONE
        }
        binding.layoutToggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.layout_linear -> {
                    preferencesBuilder.type = PostsLayoutType.LINEAR
                    preferencesBuilder.colCount = 1
                    binding.staggeredOrGridOptions.visibility = View.GONE
                }
                R.id.layout_staggered -> {
                    preferencesBuilder.type = PostsLayoutType.STAGGERED_GRID
                    if (preferencesBuilder.colCount == 1) {
                        preferencesBuilder.colCount = 2
                    }
                    binding.staggeredOrGridOptions.visibility = View.VISIBLE
                    initStaggeredOrGridOptions()
                }
                else -> {
                    preferencesBuilder.type = PostsLayoutType.GRID
                    if (preferencesBuilder.colCount == 1) {
                        preferencesBuilder.colCount = 2
                    }
                    binding.staggeredOrGridOptions.visibility = View.VISIBLE
                    initStaggeredOrGridOptions()
                }
            }
        }
    }

    private fun initColCountToggle() {
        binding.colCountToggle.check(selectedColCountId)
        binding.colCountToggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (!isChecked) return@addOnButtonCheckedListener
            preferencesBuilder.colCount = (if (checkedId == R.id.col_count_two) 2 else 3)
        }
    }

    private fun initAvatarsToggle() {
        binding.showAvatarToggle.isChecked = preferencesBuilder.isAvatarVisible
        binding.avatarSizeToggle.check(selectedAvatarSizeId)
        binding.showAvatarToggle.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferencesBuilder.isAvatarVisible = isChecked
            binding.labelAvatarSize.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.avatarSizeToggle.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.labelAvatarSize.visibility = if (preferencesBuilder.isAvatarVisible) View.VISIBLE else View.GONE
        binding.avatarSizeToggle.visibility = if (preferencesBuilder.isAvatarVisible) View.VISIBLE else View.GONE
        binding.avatarSizeToggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (!isChecked) return@addOnButtonCheckedListener
            preferencesBuilder.profilePicSize = when (checkedId) {
                R.id.avatar_size_tiny -> ProfilePicSize.TINY
                R.id.avatar_size_small -> ProfilePicSize.SMALL
                else -> ProfilePicSize.REGULAR
            }
        }
    }

    private fun initNamesToggle() {
        binding.showNamesToggle.isChecked = preferencesBuilder.isNameVisible
        binding.showNamesToggle.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferencesBuilder.isNameVisible = isChecked
        }
    }

    private fun initCornersToggle() {
        binding.cornersToggle.check(selectedCornersId)
        binding.cornersToggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.corners_round) {
                preferencesBuilder.hasRoundedCorners = true
                return@addOnButtonCheckedListener
            }
            preferencesBuilder.hasRoundedCorners = false
        }
    }

    private fun initGapToggle() {
        binding.showGapToggle.isChecked = preferencesBuilder.hasGap
        binding.showGapToggle.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferencesBuilder.hasGap = isChecked
        }
    }

    private val selectedLayoutId: Int
        get() = when (preferencesBuilder.type) {
            PostsLayoutType.STAGGERED_GRID -> R.id.layout_staggered
            PostsLayoutType.LINEAR -> R.id.layout_linear
            PostsLayoutType.GRID -> R.id.layout_grid
            else -> R.id.layout_grid
        }

    private val selectedColCountId: Int
        get() = when (preferencesBuilder.colCount) {
            2 -> R.id.col_count_two
            3 -> R.id.col_count_three
            else -> R.id.col_count_three
        }

    private val selectedCornersId: Int
        get() = if (preferencesBuilder.hasRoundedCorners) {
            R.id.corners_round
        } else R.id.corners_square

    private val selectedAvatarSizeId: Int
        get() = when (preferencesBuilder.profilePicSize) {
            ProfilePicSize.TINY -> R.id.avatar_size_tiny
            ProfilePicSize.SMALL -> R.id.avatar_size_small
            ProfilePicSize.REGULAR -> R.id.avatar_size_regular
            else -> R.id.avatar_size_regular
        }

    fun interface OnApplyListener {
        fun onApply(preferences: PostsLayoutPreferences)
    }
}