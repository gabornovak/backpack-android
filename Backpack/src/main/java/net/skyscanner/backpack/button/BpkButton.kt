package net.skyscanner.backpack.button

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import net.skyscanner.backpack.R
import net.skyscanner.backpack.button.internal.*
import net.skyscanner.backpack.util.use
import net.skyscanner.backpack.util.ResourcesUtil
import net.skyscanner.backpack.util.darken
import net.skyscanner.backpack.util.unsafeLazy

open class BpkButton : BpkButtonBase {
  constructor(context: Context) : this(context, null)
  constructor(context: Context, type: Type) : this(context, null, getStyle(type), type)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, getStyle(context, attrs))
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, Type.Primary)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, type: Type) :
    super(context, attrs, defStyleAttr) {
    this.initialType = type
    initialize(attrs, defStyleAttr)
  }

  companion object {
    const val START = ICON_POSITION_START
    const val END = ICON_POSITION_END
    const val ICON_ONLY = ICON_POSITION_ICON_ONLY
  }

  @IntDef(START, END, ICON_ONLY)
  annotation class IconPosition

  @BpkButton.IconPosition
  override var iconPosition
    get() = super.iconPosition
    set(value) {
      super.iconPosition = value
    }

  private var initialType: Type

  @ColorInt
  private var buttonBackgroundColor: Int = ContextCompat.getColor(context, R.color.bpkMonteverde)

  @ColorInt
  private var buttonStrokeColor: Int = ContextCompat.getColor(context, android.R.color.transparent)

  @ColorInt
  private var buttonStrokeColorPressed: Int = buttonStrokeColor

  @Dimension
  private val paddingHorizontal = tokens.bpkSpacingBase - tokens.bpkSpacingSm

  @Dimension
  private val paddingVertical = tokens.bpkSpacingMd + (tokens.bpkBorderSizeLg / 2)

  @Dimension
  private var cornerRadius = context.resources.getDimension(R.dimen.bpkSpacingSm)

  @Dimension
  private var iconOnlyCornerRadius = context.resources.getDimension(R.dimen.bpkSpacingLg)

  @Dimension
  private val strokeWidthNormal = tokens.bpkBorderSizeLg

  @Dimension
  private val strokeWidthSelected = tokens.bpkBorderSizeLg + ResourcesUtil.dpToPx(1, context)

  val type: Type
    get() {
      return initialType
    }

  private var _icon: Drawable? = super.icon
  final override var icon: Drawable?
    get() = super.icon
    set(value) {
      if (_icon != value) {
        _icon = value
        super.icon = value
      }
    }

  private val _progress by unsafeLazy {
    CircularProgressDrawable(context).apply {
      setStyle(CircularProgressDrawable.DEFAULT)
      centerRadius = resources.getDimension(R.dimen.bpkSpacingSm) * 1.3f
      strokeWidth = resources.getDimension(R.dimen.bpkSpacingSm) * 0.5f
      val disabledColour = textColors.getColorForState(intArrayOf(-android.R.attr.state_enabled), textColors.defaultColor)
      setColorSchemeColors(disabledColour)
      start()
    }
  }

  private var _loading = false
  var loading: Boolean = false
    get() = _loading
    set(value) {
      _loading = value
      if (_loading != field) {
        field = value
        update()
      }
    }

  private fun initialize(attrs: AttributeSet?, defStyleAttr: Int) {
    var textColor = ContextCompat.getColor(context, R.color.bpkWhite)
    context.theme.obtainStyledAttributes(attrs, R.styleable.BpkButton, defStyleAttr, 0)
      .use {
        if (it.hasValue(R.styleable.BpkButton_buttonType)) {
          initialType = Type.fromId(it.getInt(R.styleable.BpkButton_buttonType, 0))
        }

        buttonBackgroundColor = it.getColor(R.styleable.BpkButton_buttonBackgroundColor, ContextCompat.getColor(context, type.bgColor))

        buttonStrokeColor = it.getColor(R.styleable.BpkButton_buttonStrokeColor, ContextCompat.getColor(context, type.strokeColor))
        buttonStrokeColorPressed = it.getColor(R.styleable.BpkButton_buttonStrokeColorPressed, ContextCompat.getColor(context, type.strokeColorSelected))

        _loading = it.getBoolean(R.styleable.BpkButton_buttonLoading, _loading)

        textColor = it.getColor(R.styleable.BpkButton_buttonTextColor, ContextCompat.getColor(context, type.textColor))
      }

    setTextColor(getColorSelector(
      textColor,
      darken(textColor, .1f),
      ContextCompat.getColor(context, type.disabledTextColor)
    ))
    update()
  }

  override fun update() {
    if (iconPosition == ICON_ONLY) {
      text = ""
    }

    var paddingHorizontal = paddingHorizontal
    val paddingVertical = paddingVertical

    if (iconPosition == ICON_ONLY) {
      paddingHorizontal = tokens.bpkSpacingMd
    }

    setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

    if (!text.isNullOrEmpty()) {
      compoundDrawablePadding = tokens.bpkSpacingSm
    }

    background = getButtonBackground()

    if (shouldSetStateListAnimator()) {
      loadStateListAnimator(R.drawable.bpk_button_state_animator)
    }

    clipToOutline = true

    if (loading) {
      super.icon = _progress
      super.setEnabled(false)
    } else {
      super.icon = _icon
      super.setEnabled(enabled != false)
    }
  }

  private var enabled: Boolean? = null

  override fun setEnabled(enabled: Boolean) {
    // we want to store the enabling state set
    // by the used in order to recover to it when loading is set to false.
    // the null values used to detect the initialization
    if (enabled != isEnabled) {
      this.enabled = enabled
      super.setEnabled(enabled)
    }
  }

  @VisibleForTesting
  internal fun disabledBackground(): Drawable {
    return getRippleDrawable(
      normalColor = ContextCompat.getColor(context, type.disabledBgColor),
      cornerRadius = if (iconPosition == ICON_ONLY) iconOnlyCornerRadius else cornerRadius,
      strokeWidth = null,
      strokeColor = null
    )
  }

  private fun getButtonBackground(): Drawable? {
    return if (this.isEnabled) {
      getRippleDrawable(
        normalColor = buttonBackgroundColor,
        cornerRadius = if (iconPosition == ICON_ONLY) iconOnlyCornerRadius else cornerRadius,
        strokeWidth = strokeWidthForType(type),
        strokeColor = getColorSelector(buttonStrokeColor, buttonStrokeColorPressed, buttonStrokeColor)
      )
    } else disabledBackground()
  }

  private fun strokeWidthForType(type: Type): StrokeWidth? =
    when (type) {
      BpkButton.Type.Secondary, BpkButton.Type.Destructive, BpkButton.Type.Outline ->
        Pair(strokeWidthNormal, strokeWidthSelected)
      else -> null
    }

  private fun shouldSetStateListAnimator() =
    isEnabled && isElevationRequiredForType() && isStateListAnimatorSupported()

  private fun isElevationRequiredForType() = type == Type.Primary || type == Type.Featured

  private fun loadStateListAnimator(@DrawableRes animator: Int) {
    this.stateListAnimator = AnimatorInflater.loadStateListAnimator(context, animator)
  }

  enum class Type(
    internal val id: Int,
    @ColorRes internal val bgColor: Int,
    @ColorRes internal val textColor: Int,
    @ColorRes internal val strokeColor: Int,
    @ColorRes internal val strokeColorSelected: Int,
    @ColorRes internal val disabledBgColor: Int,
    @ColorRes internal val disabledTextColor: Int
  ) {
    Primary(
      id = 0,
      bgColor = R.color.bpkMonteverde,
      textColor = R.color.bpkWhite,
      strokeColor = android.R.color.transparent,
      strokeColorSelected = android.R.color.transparent,
      disabledBgColor = R.color.__buttonDisabledBackground,
      disabledTextColor = R.color.__buttonDisabledText
    ),
    Secondary(
      id = 1,
      bgColor = R.color.__buttonSecondaryBackground,
      textColor = R.color.bpkPrimary,
      strokeColor = R.color.__buttonSecondaryBorder,
      strokeColorSelected = R.color.bpkPrimary,
      disabledBgColor = R.color.__buttonDisabledBackground,
      disabledTextColor = R.color.__buttonDisabledText
    ),
    Featured(
      id = 2,
      bgColor = R.color.bpkSkyBlue,
      textColor = R.color.bpkWhite,
      strokeColor = android.R.color.transparent,
      strokeColorSelected = android.R.color.transparent,
      disabledBgColor = R.color.__buttonDisabledBackground,
      disabledTextColor = R.color.__buttonDisabledText
    ),
    Destructive(
      id = 3,
      bgColor = R.color.__buttonSecondaryBackground,
      textColor = R.color.bpkPanjin,
      strokeColor = R.color.__buttonSecondaryBorder,
      strokeColorSelected = R.color.bpkPanjin,
      disabledBgColor = R.color.__buttonDisabledBackground,
      disabledTextColor = R.color.__buttonDisabledText
    ),
    Outline(
      id = 4,
      bgColor = android.R.color.transparent,
      textColor = R.color.bpkWhite,
      strokeColor = R.color.bpkWhite,
      strokeColorSelected = R.color.bpkWhite,
      disabledBgColor = R.color.bpkSkyGrayTint06,
      disabledTextColor = R.color.bpkSkyGrayTint04
    );

    internal companion object {
      internal fun fromId(id: Int): Type {
        for (f in values()) {
          if (f.id == id) return f
        }
        throw IllegalArgumentException()
      }
    }
  }
}

private fun getStyle(context: Context, attrs: AttributeSet?): Int {
  val attr = context.theme.obtainStyledAttributes(attrs, R.styleable.BpkButton, 0, 0)
  val style = BpkButton.Type.fromId(attr.getInt(R.styleable.BpkButton_buttonType, 0))
  return getStyle(style)
}

private fun getStyle(type: BpkButton.Type): Int {
  return when (type) {
    BpkButton.Type.Primary -> R.attr.bpkButtonPrimaryStyle
    BpkButton.Type.Secondary -> R.attr.bpkButtonSecondaryStyle
    BpkButton.Type.Outline -> R.attr.bpkButtonOutlineStyle
    BpkButton.Type.Featured -> R.attr.bpkButtonFeaturedStyle
    BpkButton.Type.Destructive -> R.attr.bpkButtonDestructiveStyle
  }
}

private fun isStateListAnimatorSupported() =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && !isSpecificDeviceBlackListed()

private fun isSpecificDeviceBlackListed() =
  Build.MANUFACTURER.equals("samsung", true) && Build.MODEL.equals("gt-i9505", true)
