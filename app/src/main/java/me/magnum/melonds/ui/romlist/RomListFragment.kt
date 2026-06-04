package me.magnum.melonds.ui.romlist

import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import me.magnum.melonds.R
import me.magnum.melonds.ui.theme.MelonTheme
import me.magnum.melonds.databinding.ItemRomConfigurableBinding
import me.magnum.melonds.databinding.ItemRomSimpleBinding
import me.magnum.melonds.databinding.RomListFragmentBinding
import me.magnum.melonds.domain.model.RomIconFiltering
import me.magnum.melonds.domain.model.RomScanningStatus
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.extensions.setViewEnabledRecursive
import me.magnum.melonds.common.Permission
import me.magnum.melonds.common.contracts.DirectoryPickerContract
import me.magnum.melonds.parcelables.RomParcelable
import me.magnum.melonds.ui.romdetails.RomDetailsActivity
import me.magnum.melonds.ui.romlist.RomListFragment.RomListAdapter.RomViewHolder

@AndroidEntryPoint
class RomListFragment : Fragment() {
    companion object {
        private const val KEY_ALLOW_ROM_CONFIGURATION = "allow_rom_configuration"
        private const val KEY_ROM_ENABLE_CRITERIA = "rom_enable_criteria"

        fun newInstance(allowRomConfiguration: Boolean, enableCriteria: RomEnableCriteria): RomListFragment {
            return RomListFragment().also {
                it.arguments = bundleOf(
                    KEY_ALLOW_ROM_CONFIGURATION to allowRomConfiguration,
                    KEY_ROM_ENABLE_CRITERIA to enableCriteria.toString(),
                )
            }
        }
    }

    enum class RomEnableCriteria {
        ENABLE_ALL,
        ENABLE_NON_DSIWARE,
    }

    private lateinit var binding: RomListFragmentBinding
    private val romListViewModel: RomListViewModel by activityViewModels()
    private lateinit var romListAdapter: RomListAdapter

    private var romSelectedListener: ((Rom) -> Unit)? = null

    private val directoryPickerLauncher = registerForActivityResult(DirectoryPickerContract(Permission.READ_WRITE)) { uri ->
        if (uri != null) {
            romListViewModel.addRomSearchDirectory(uri)
        }
    }

    private var isGridMode = false
    private val viewModePrefs by lazy {
        requireContext().getSharedPreferences("liteds_prefs", Context.MODE_PRIVATE)
    }
    private var dividerDecoration: DividerItemDecoration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RomListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.listRoms) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(bottom = insets.bottom)

            WindowInsetsCompat.CONSUMED
        }
        binding.swipeRefreshRoms.setOnRefreshListener { romListViewModel.refreshRoms() }

        val allowRomConfiguration = arguments?.getBoolean(KEY_ALLOW_ROM_CONFIGURATION) ?: true
        val romEnableCriteria = arguments?.getString(KEY_ROM_ENABLE_CRITERIA)?.let { RomEnableCriteria.valueOf(it) } ?: RomEnableCriteria.ENABLE_ALL

        romListAdapter = RomListAdapter(
            allowRomConfiguration = allowRomConfiguration,
            context = requireContext(),
            coroutineScope = lifecycleScope,
            listener = object : RomClickListener {
                override fun onRomClicked(rom: Rom) {
                    romListViewModel.setRomLastPlayedNow(rom)
                    romSelectedListener?.invoke(rom)
                }

                override fun onRomConfigClicked(rom: Rom) {
                    val intent = Intent(requireContext(), RomDetailsActivity::class.java).apply {
                        putExtra(RomDetailsActivity.KEY_ROM, RomParcelable(rom))
                    }
                    startActivity(intent)
                }
            },
            romEnabledFilter = buildRomEnabledFilter(romEnableCriteria),
        )

        isGridMode = viewModePrefs.getString("view_mode", "list") == "grid"
        binding.listRoms.adapter = romListAdapter
        applyLayoutMode()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                romListViewModel.romScanningStatus.collectLatest { status ->
                    binding.swipeRefreshRoms.isRefreshing = status == RomScanningStatus.SCANNING
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                romListViewModel.roms.filterNotNull().collectLatest { roms ->
                    romListAdapter.setRoms(roms)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    romListViewModel.roms,
                    romListViewModel.romScanningStatus,
                ) { roms, status ->
                    roms != null && roms.isEmpty() && status != RomScanningStatus.SCANNING
                }.collectLatest { showEmpty ->
                    binding.composeEmptyState.isVisible = showEmpty
                }
            }
        }

        binding.composeEmptyState.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MelonTheme {
                    val searchQuery by romListViewModel.searchQuery.collectAsState()
                    RomListEmptyStateView(
                        searchQuery = searchQuery,
                        onRefresh = { romListViewModel.refreshRoms() },
                        onSelectDirectory = { directoryPickerLauncher.launch(null) },
                    )
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                romListViewModel.onRomIconFilteringChanged.collectLatest {
                    romListAdapter.updateIcons()
                }
            }
        }

        binding.composeRecentlyPlayed.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MelonTheme {
                    val recentlyPlayed by romListViewModel.recentlyPlayed.collectAsState()
                    RecentlyPlayedSection(
                        recentlyPlayed = recentlyPlayed,
                        onRomClick = { rom ->
                            romListViewModel.setRomLastPlayedNow(rom)
                            romSelectedListener?.invoke(rom)
                        },
                    )
                }
            }
        }
    }

    private fun applyLayoutMode() {
        romListAdapter.isGridMode = isGridMode
        if (isGridMode) {
            dividerDecoration?.let { binding.listRoms.removeItemDecoration(it) }
            binding.listRoms.layoutManager = GridLayoutManager(context, 2)
        } else {
            val lm = LinearLayoutManager(context)
            binding.listRoms.layoutManager = lm
            dividerDecoration = DividerItemDecoration(context, lm.orientation).also {
                binding.listRoms.addItemDecoration(it)
            }
        }
        romListAdapter.notifyDataSetChanged()
    }

    fun toggleViewMode() {
        isGridMode = !isGridMode
        viewModePrefs.edit()
            .putString("view_mode", if (isGridMode) "grid" else "list")
            .apply()
        applyLayoutMode()
    }

    private fun buildRomEnabledFilter(romEnableCriteria: RomEnableCriteria): RomEnabledFilter {
        return when (romEnableCriteria) {
            RomEnableCriteria.ENABLE_ALL -> RomEnabledFilter { true }
            RomEnableCriteria.ENABLE_NON_DSIWARE -> RomEnabledFilter { !it.isDsiWareTitle}
        }
    }

    fun setRomSelectedListener(listener: (Rom) -> Unit) {
        romSelectedListener = listener
    }

    private inner class RomListAdapter(
        private val allowRomConfiguration: Boolean,
        private val context: Context,
        private val coroutineScope: CoroutineScope,
        private val listener: RomClickListener,
        private val romEnabledFilter: RomEnabledFilter,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_LIST = 0
        private val VIEW_TYPE_LIST_CONFIG = 1
        private val VIEW_TYPE_GRID = 2

        var isGridMode: Boolean = false
        private val roms: ArrayList<Rom> = ArrayList()

        fun setRoms(roms: List<Rom>) {
            val result = DiffUtil.calculateDiff(RomsDiffUtilCallback(this.roms, roms))
            result.dispatchUpdatesTo(this)

            this.roms.clear()
            this.roms.addAll(roms)
        }

        fun updateIcons() {
            val diff = DiffUtil.calculateDiff(RomIconDiffUtilCallback(this.roms.size))
            diff.dispatchUpdatesTo(this)
        }

        override fun getItemCount(): Int {
            return roms.size
        }

        override fun getItemViewType(position: Int): Int = when {
            isGridMode -> VIEW_TYPE_GRID
            allowRomConfiguration -> VIEW_TYPE_LIST_CONFIG
            else -> VIEW_TYPE_LIST
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_GRID -> {
                    val view = LayoutInflater.from(context).inflate(R.layout.item_rom_grid, viewGroup, false)
                    GridRomViewHolder(view, coroutineScope, listener::onRomClicked, listener::onRomConfigClicked)
                }
                VIEW_TYPE_LIST_CONFIG -> {
                    val binding = ItemRomConfigurableBinding.inflate(LayoutInflater.from(context), viewGroup, false)
                    ConfigurableRomViewHolder(binding.root, lifecycleScope, listener::onRomClicked, listener::onRomConfigClicked)
                }
                else -> {
                    val binding = ItemRomSimpleBinding.inflate(LayoutInflater.from(context), viewGroup, false)
                    RomViewHolder(binding.root, coroutineScope, listener::onRomClicked)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
            val rom = roms[i]
            val isRomEnabled = romEnabledFilter.isRomEnabled(rom)
            when (holder) {
                is GridRomViewHolder -> holder.setRom(rom, isRomEnabled)
                is RomViewHolder -> holder.setRom(rom, isRomEnabled)
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            when (holder) {
                is GridRomViewHolder -> holder.cleanup()
                is RomViewHolder -> holder.cleanup()
            }
        }

        open inner class RomViewHolder(itemView: View, private val coroutineScope: CoroutineScope, onRomClick: (Rom) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val imageViewRomIcon = itemView.findViewById<ImageView>(R.id.imageRomIcon)
            private val textViewRomName = itemView.findViewById<TextView>(R.id.textRomName)
            private val textViewRomPath = itemView.findViewById<TextView>(R.id.textRomPath)
            private val imagePlatformLogo = itemView.findViewById<ImageView>(R.id.logoPlatform)

            private lateinit var rom: Rom
            private var romIconLoadJob: Job? = null

            init {
                itemView.setOnClickListener { onRomClick(rom) }
            }

            fun cleanup() {
                romIconLoadJob?.cancel()
            }

            open fun setRom(rom: Rom, isEnabled: Boolean) {
                this.rom = rom
                textViewRomName.text = rom.config.customName ?: rom.name
                val playTimeStr = rom.totalPlayTime.toPlayTimeString()
                val lastPlayedStr = rom.lastPlayed?.toRelativeTimeString(itemView.context) ?: ""
                textViewRomPath.text = listOfNotNull(
                    playTimeStr.ifEmpty { null },
                    lastPlayedStr.ifEmpty { null },
                ).joinToString("  ").ifEmpty { rom.fileName }
                imageViewRomIcon.setImageDrawable(null)
                imagePlatformLogo.isVisible = rom.isDsiWareTitle

                val platformDrawable = if (rom.isDsiWareTitle) {
                     ResourcesCompat.getDrawable(itemView.resources, R.drawable.logo_dsiware, null)
                } else {
                    null
                }

                if (platformDrawable != null && !isEnabled) {
                    platformDrawable.apply {
                        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        alpha = 127
                    }
                }

                imagePlatformLogo.setImageDrawable(platformDrawable)

                romIconLoadJob = coroutineScope.launch {
                    val romIcon = romListViewModel.getRomIcon(rom)
                    val iconDrawable = romIcon.bitmap?.toDrawable(itemView.resources)?.apply {
                        paint.isFilterBitmap = romIcon.filtering == RomIconFiltering.LINEAR
                        if (isEnabled) {
                            colorFilter = null
                            alpha = 255
                        } else {
                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                            alpha = 127
                        }
                    }
                    imageViewRomIcon.setImageDrawable(iconDrawable)
                }

                itemView.setViewEnabledRecursive(isEnabled)
            }

            protected fun getRom() = rom
        }

        inner class ConfigurableRomViewHolder(
            itemView: View,
            coroutineScope: CoroutineScope,
            onRomClick: (Rom) -> Unit,
            onRomConfigClick: (Rom) -> Unit
        ) : RomViewHolder(itemView, coroutineScope, onRomClick) {

            private val imageViewButtonRomConfig = itemView.findViewById<ImageView>(R.id.buttonRomConfig)

            init {
                imageViewButtonRomConfig.setOnClickListener {
                    onRomConfigClick(getRom())
                }
            }
        }

        inner class GridRomViewHolder(
            itemView: View,
            private val coroutineScope: CoroutineScope,
            onRomClick: (Rom) -> Unit,
            onRomLongClick: (Rom) -> Unit,
        ) : RecyclerView.ViewHolder(itemView) {

            private val imageViewRomIcon = itemView.findViewById<ImageView>(R.id.imageRomIcon)
            private val textViewRomName = itemView.findViewById<TextView>(R.id.textRomName)
            private val textViewPlayTime = itemView.findViewById<TextView>(R.id.textPlayTime)
            private val textViewLastPlayed = itemView.findViewById<TextView>(R.id.textLastPlayed)
            private lateinit var currentRom: Rom
            private var romIconLoadJob: Job? = null

            init {
                itemView.setOnClickListener { onRomClick(currentRom) }
                itemView.setOnLongClickListener { onRomLongClick(currentRom); true }
            }

            fun cleanup() { romIconLoadJob?.cancel() }

            fun setRom(rom: Rom, isEnabled: Boolean) {
                currentRom = rom
                textViewRomName.text = rom.config.customName ?: rom.name

                val playTimeStr = rom.totalPlayTime.toPlayTimeString()
                textViewPlayTime.text = playTimeStr
                textViewPlayTime.visibility = if (playTimeStr.isNotEmpty()) View.VISIBLE else View.GONE

                val lastPlayedStr = rom.lastPlayed?.toRelativeTimeString(itemView.context) ?: ""
                textViewLastPlayed.text = lastPlayedStr
                textViewLastPlayed.visibility = if (lastPlayedStr.isNotEmpty()) View.VISIBLE else View.GONE

                imageViewRomIcon.setImageDrawable(null)
                romIconLoadJob?.cancel()
                romIconLoadJob = coroutineScope.launch {
                    val romIcon = romListViewModel.getRomIcon(rom)
                    val iconDrawable = romIcon.bitmap?.toDrawable(itemView.resources)?.apply {
                        paint.isFilterBitmap = romIcon.filtering == RomIconFiltering.LINEAR
                        if (!isEnabled) {
                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                            alpha = 127
                        }
                    }
                    imageViewRomIcon.setImageDrawable(iconDrawable)
                }

                itemView.setViewEnabledRecursive(isEnabled)
            }
        }

        inner class RomsDiffUtilCallback(private val oldRoms: List<Rom>, private val newRoms: List<Rom>) : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldRoms.size

            override fun getNewListSize(): Int = newRoms.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldRoms[oldItemPosition].uri == newRoms[newItemPosition].uri
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldRoms[oldItemPosition] == newRoms[newItemPosition]
            }
        }

        /**
         * [DiffUtil] callback used to update the ROM icons in the adapter. This callback doesn't compare any item and has a fixed output. To force the icons to update, the
         * items are always assume to be the same but with different content.
         */
        inner class RomIconDiffUtilCallback(private val romCount: Int) : DiffUtil.Callback() {
            override fun getOldListSize(): Int = romCount

            override fun getNewListSize(): Int = romCount

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
        }
    }

    private interface RomClickListener {
        fun onRomClicked(rom: Rom)
        fun onRomConfigClicked(rom: Rom)
    }

    fun interface RomEnabledFilter {
        fun isRomEnabled(rom: Rom): Boolean
    }
}
