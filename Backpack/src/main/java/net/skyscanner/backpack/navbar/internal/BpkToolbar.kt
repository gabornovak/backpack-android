package net.skyscanner.backpack.navbar.internal

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import net.skyscanner.backpack.R
import net.skyscanner.backpack.util.resolveThemeId

internal class BpkToolbar @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : Toolbar(ContextThemeWrapper(context, resolveThemeId(context, R.attr.toolbarStyle)), attrs, defStyleAttr) {

  init {
    background = null
  }

  private var titleTextColor: Int = 0

  override fun inflateMenu(resId: Int) {
    super.inflateMenu(resId)
    tintMenu(menu)
  }

  override fun setTitleTextColor(color: Int) {
    super.setTitleTextColor(color)
    this.titleTextColor = color
    this.overflowIcon = overflowIcon
    this.navigationIcon = navigationIcon
    this.tintMenu(menu)
  }

  private fun tintMenu(menu: Menu) {
    for (i in 0 until menu.size()) {
      val item = menu.getItem(i)
      val icon = item.icon
      if (icon != null) {
        item.icon = tintIcon(icon)
      }
    }
  }

  override fun setOverflowIcon(icon: Drawable?) {
    super.setOverflowIcon(tintIcon(icon))
  }

  override fun setNavigationIcon(icon: Drawable?) {
    super.setNavigationIcon(tintIcon(icon))
  }

  private fun tintIcon(icon: Drawable?): Drawable? = icon?.let {
    DrawableCompat.wrap(it.mutate()).apply {
      DrawableCompat.setTint(this, titleTextColor)
    }
  }
}
