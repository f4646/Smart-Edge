package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class IconPackPickerDialog : BottomSheetDialogFragment() {

    private lateinit var panelPrefs: PanelPreferences
    private lateinit var iconPackManager: IconPackManager
    private var onSelected: (() -> Unit)? = null

    companion object {
        const val TAG = "IconPackPickerDialog"
        
        fun show(context: Context, onSelected: () -> Unit) {
            val dialog = IconPackPickerDialog()
            dialog.onSelected = onSelected
            (context as? androidx.fragment.app.FragmentActivity)?.let {
                dialog.show(it.supportFragmentManager, TAG)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        panelPrefs = PanelPreferences(ctx)
        iconPackManager = IconPackManager(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (24 * density).toInt(),
                (12 * density).toInt(),
                (24 * density).toInt(),
                (28 * density).toInt()
            )
        }

        // Drag handle
        val handle = View(ctx).apply {
            val lp = LinearLayout.LayoutParams((40 * density).toInt(), (4 * density).toInt())
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = (20 * density).toInt()
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#33FFFFFF"))
            }
        }
        root.addView(handle)

        // Header
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }

        val iconBg = FrameLayout(ctx).apply {
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (16 * density).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1A4A9EFF"))
            }
        }
        val iconView = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_section_appearance)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A9EFF"))
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        iconBg.addView(iconView)
        headerRow.addView(iconBg)

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitle = TextView(ctx).apply {
            text = "Select Icon Pack"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        val tvStatus = TextView(ctx).apply {
            text = "Current: ${panelPrefs.iconPackLabel}"
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
        }
        titleCol.addView(tvTitle)
        titleCol.addView(tvStatus)
        headerRow.addView(titleCol)
        root.addView(headerRow)

        // Search Bar
        val searchContainer = com.google.android.material.textfield.TextInputLayout(ctx).apply {
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(14 * density, 14 * density, 14 * density, 14 * density)
            hint = "Search icon packs..."
            setPadding(0, 0, 0, (16 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val etSearch = com.google.android.material.textfield.TextInputEditText(ctx).apply {
            setSingleLine()
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        searchContainer.addView(etSearch)
        root.addView(searchContainer)

        // List
        val rv = RecyclerView(ctx).apply {
            id = View.generateViewId()
            layoutManager = LinearLayoutManager(ctx)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (450 * density).toInt()
            )
        }
        
        val allPacks = mutableListOf<IconPackInfo>()
        allPacks.add(IconPackInfo("none", "System Default", ctx.getDrawable(android.R.drawable.sym_def_app_icon)!!))
        allPacks.addAll(iconPackManager.getInstalledIconPacks())

        val adapter = PackAdapter(allPacks) { item ->
            panelPrefs.selectedIconPack = item.packageName
            panelPrefs.iconPackLabel = item.label
            
            val intent = Intent(ctx, FloatingPanelService::class.java).apply {
                action = FloatingPanelService.ACTION_REFRESH
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
            onSelected?.invoke()
            dismiss()
        }
        rv.adapter = adapter
        root.addView(rv)

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val density = resources.displayMetrics.density
                cornerRadii = floatArrayOf(
                    28 * density, 28 * density,
                    28 * density, 28 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#1F2732"))
            }
        }
        return dialog
    }

    inner class PackAdapter(
        private val allItems: List<IconPackInfo>,
        private val onSelect: (IconPackInfo) -> Unit
    ) : RecyclerView.Adapter<PackAdapter.VH>() {

        private var displayItems = allItems.toList()

        fun filter(query: String) {
            displayItems = if (query.isEmpty()) allItems
            else allItems.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(android.R.id.icon)
            val name: TextView = v.findViewById(android.R.id.text1)
            val pkg: TextView = v.findViewById(android.R.id.text2)
            val radio: android.widget.RadioButton = v.findViewById(android.R.id.checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val density = ctx.resources.displayMetrics.density
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (4 * density).toInt()
                }
                
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16 * density
                    setColor(Color.TRANSPARENT)
                }

                val outValue = android.util.TypedValue()
                ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = ctx.getDrawable(outValue.resourceId)
                isClickable = true
                isFocusable = true
            }

            val iconView = ImageView(ctx).apply {
                id = android.R.id.icon
                val size = (42 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (16 * density).toInt() }
            }
            row.addView(iconView)

            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(ctx).apply {
                id = android.R.id.text1
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
            val tvPkg = TextView(ctx).apply {
                id = android.R.id.text2
                textSize = 11f
                setTextColor(Color.parseColor("#99FFFFFF"))
            }
            textCol.addView(tvName)
            textCol.addView(tvPkg)
            row.addView(textCol)

            val rb = android.widget.RadioButton(ctx).apply {
                id = android.R.id.checkbox
                isClickable = false
                isFocusable = false
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A9EFF"))
            }
            row.addView(rb)

            return VH(row)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = displayItems[position]
            val isSelected = item.packageName == panelPrefs.selectedIconPack
            val density = holder.itemView.context.resources.displayMetrics.density

            holder.icon.setImageDrawable(item.icon)
            holder.name.text = item.label
            holder.pkg.text = if (item.packageName == "none") "Default app icons" else item.packageName
            holder.radio.isChecked = isSelected

            // Highlight background for selected item
            (holder.itemView.background as? GradientDrawable)?.apply {
                if (isSelected) {
                    setColor(Color.parseColor("#1A4A9EFF"))
                    setStroke((1 * density).toInt(), Color.parseColor("#334A9EFF"))
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(0, Color.TRANSPARENT)
                }
            }

            holder.itemView.setOnClickListener {
                onSelect(item)
            }
        }

        override fun getItemCount() = displayItems.size
    }
}
