package dev.juuri.noma.capture
import android.content.Context
import dev.juuri.noma.R
enum class NomaWidgetStyle(val layoutId: Int) {
    MATERIAL_YOU(R.layout.noma_widget_material),
    FROSTED_GREEN(R.layout.noma_widget_frosted_green),
    LIQUID_GLASS(R.layout.noma_widget_liquid_glass);
    companion object {
        fun fromStorage(value: String?): NomaWidgetStyle =
            entries.firstOrNull { it.name == value } ?: MATERIAL_YOU
    }
}
object NomaWidgetPreferences {
    private const val FILE = "noma_widget_appearance"
    private const val KEY = "style"
    fun read(context: Context): NomaWidgetStyle = NomaWidgetStyle.fromStorage(
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
    )
    fun save(context: Context, style: NomaWidgetStyle) {
        check(context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, style.name).commit()) { "Unable to save widget style" }
    }
}
