package com.asta669.wakeup

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenSmokeTest {
    @Before fun clearSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf("wakeup_prefs", "alarm_session", "school_mode_v1").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun labelled(activity: Activity, text: String) = descendants(activity.findViewById(android.R.id.content))
        .filterIsInstance<TextView>().first { it.text.toString() == text }
    private fun capture(activity: Activity, name: String) {
        val view = activity.window.decorView
        val width = 822; val height = 1782
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/reports/ui-preview").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun dashboardRequiresQrBeforeArming() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        capture(activity, "accueil")
        labelled(activity, "Activer le réveil").performClick()
        assertFalse(Prefs.isEnabled(activity))
        assertEquals(QrSetupActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
        controller.pause().stop().destroy()
    }
    @Test fun setupGeneratesPrintableCodeWithoutArming() {
        val controller = Robolectric.buildActivity(QrSetupActivity::class.java).setup()
        val activity = controller.get()
        labelled(activity, "Générer un QR code").performClick()
        assertNotNull(labelled(activity, "Imprimer / enregistrer en PDF"))
        assertFalse(Prefs.hasKitchenQr(activity))
        capture(activity, "code-cuisine")
        controller.pause().stop().destroy()
    }
    @Test fun schoolScreenShowsHonestOffStateAndAirplaneAction() {
        val controller = Robolectric.buildActivity(SchoolActivity::class.java).setup()
        val activity = controller.get()
        assertEquals("School OFF", activity.findViewById<TextView>(R.id.schoolState).text.toString())
        assertTrue(activity.findViewById<View>(R.id.schoolAirplane).isEnabled)
        capture(activity, "school")
        controller.pause().stop().destroy()
    }
}
