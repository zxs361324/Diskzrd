package com.example.novapanel

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*


class MainActivity : Activity() {

    // Discord-ish palette
    private val bgDarkest = Color.parseColor("#1E1F22")
    private val bgSide = Color.parseColor("#2B2D31")
    private val bgMain = Color.parseColor("#313338")
    private val bgField = Color.parseColor("#1E1F22")
    private val txtPrimary = Color.parseColor("#F2F3F5")
    private val txtMuted = Color.parseColor("#949BA4")
    private val blurple = Color.parseColor("#5865F2")
    private val green = Color.parseColor("#23A55A")
    private val red = Color.parseColor("#F23F43")
    private val swatchColors = intArrayOf(
        Color.parseColor("#5865F2"), Color.parseColor("#23A55A"), Color.parseColor("#FEE75C"),
        Color.parseColor("#EB459E"), Color.parseColor("#F23F43"), Color.parseColor("#F2F3F5")
    )

    private lateinit var servers: MutableList<Server>
    private var selected: Server? = null
    private var mode = 0 // 0 = webhook, 1 = bot

    private lateinit var sideList: LinearLayout
    private lateinit var content: FrameLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        servers = Prefs.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(bgMain)
        }

        val sideScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(bgDarkest)
            isVerticalScrollBarEnabled = false
        }
        sideList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(48), dp(10), dp(10))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        sideScroll.addView(sideList)

        content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }

        root.addView(sideScroll)
        root.addView(content)
        setContentView(root)

        refreshSidebar()
        showEmptyState()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun circleDrawable(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(color)
    }

    private fun roundedDrawable(color: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = radius; setColor(color)
    }

    // ---------- Sidebar ----------

    private fun refreshSidebar() {
        sideList.removeAllViews()
        for (s in servers) {
            val icon = TextView(this).apply {
                text = if (s.name.isNotBlank()) s.name.trim().take(1).uppercase() else "?"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 18f
                background = circleDrawable(s.color)
                layoutParams = LinearLayout.LayoutParams(dp(50), dp(50)).apply { bottomMargin = dp(14) }
                setOnClickListener { selected = s; mode = 0; renderDashboard() }
                setOnLongClickListener { showEditDialog(s); true }
            }
            sideList.addView(icon)
        }
        val add = TextView(this).apply {
            text = "+"
            gravity = Gravity.CENTER
            setTextColor(blurple)
            textSize = 22f
            background = circleDrawable(Color.parseColor("#2B2D31")).apply {
                setStroke(dp(1), blurple)
            }
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
            setOnClickListener { showAddDialog() }
        }
        sideList.addView(add)
    }

    private fun showEmptyState() {
        content.removeAllViews()
        val t = TextView(this).apply {
            text = "أضف سيرفر من زر + على الجانب\nلتبدأ بإرسال الرسائل والتحكم بالبوت"
            setTextColor(txtMuted)
            textSize = 15f
            gravity = Gravity.CENTER
        }
        content.addView(t, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    // ---------- Add / Edit dialog ----------

    private fun field(hint: String, value: String = ""): EditText = EditText(this).apply {
        this.hint = hint
        setText(value)
        setTextColor(txtPrimary)
        setHintTextColor(txtMuted)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = roundedDrawable(bgField, dp(8).toFloat())
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        layoutParams = lp
    }

    private fun showAddDialog() = showServerDialog(null)

    private fun showEditDialog(s: Server) = showServerDialog(s)

    private fun showServerDialog(existing: Server?) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(4))
        }
        val nameF = field("اسم السيرفر", existing?.name ?: "")
        val webhookF = field("رابط Webhook (اختياري)", existing?.webhookUrl ?: "")
        val tokenF = field("توكن البوت (اختياري)", existing?.botToken ?: "")
        val chanF = field("معرّف القناة الافتراضية (اختياري)", existing?.channelId ?: "")
        box.addView(nameF); box.addView(webhookF); box.addView(tokenF); box.addView(chanF)

        var pickedColor = existing?.color ?: blurple
        val swatchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        for (c in swatchColors) {
            val sw = View(this).apply {
                background = circleDrawable(c)
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(dp(6), dp(4), dp(6), dp(10)) }
                setOnClickListener { pickedColor = c }
            }
            swatchRow.addView(sw)
        }
        box.addView(swatchRow)

        val scroll = ScrollView(this).apply { addView(box) }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "سيرفر جديد" else "تعديل السيرفر")
            .setView(scroll)
            .setPositiveButton("حفظ", null)
            .setNegativeButton("إلغاء", null)
        if (existing != null) {
            builder.setNeutralButton("حذف") { _, _ ->
                servers.remove(existing)
                Prefs.save(this, servers)
                if (selected == existing) { selected = null; showEmptyState() }
                refreshSidebar()
            }
        }
        val dlg = builder.create()
        dlg.show()
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameF.text.toString().trim()
            if (n.isEmpty()) { nameF.error = "مطلوب"; return@setOnClickListener }
            if (existing == null) {
                val s = Server(Prefs.newId(), n, webhookF.text.toString().trim(), tokenF.text.toString().trim(), chanF.text.toString().trim(), pickedColor)
                servers.add(s)
                selected = s
            } else {
                existing.name = n
                existing.webhookUrl = webhookF.text.toString().trim()
                existing.botToken = tokenF.text.toString().trim()
                existing.channelId = chanF.text.toString().trim()
                existing.color = pickedColor
                selected = existing
            }
            Prefs.save(this, servers)
            refreshSidebar()
            mode = 0
            renderDashboard()
            dlg.dismiss()
        }
    }

    // ---------- Dashboard ----------

    private fun segButton(text: String, active: Boolean, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(if (active) Color.WHITE else txtMuted)
        setPadding(dp(18), dp(10), dp(18), dp(10))
        background = roundedDrawable(if (active) blurple else bgDarkest, dp(20).toFloat())
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8)
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(txtMuted)
        textSize = 12f
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(10); lp.bottomMargin = dp(4)
        layoutParams = lp
    }

    private fun actionButton(text: String, bg: Int, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = roundedDrawable(bg, dp(8).toFloat())
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(6)
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun renderDashboard() {
        val s = selected ?: return
        content.removeAllViews()

        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(s.color)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = s.name
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✏️"
            textSize = 16f
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { showEditDialog(s) }
        })
        outer.addView(header)

        // Segmented control
        val seg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(4))
        }
        seg.addView(segButton("📨 Webhook", mode == 0) { mode = 0; renderDashboard() })
        seg.addView(segButton("🤖 البوت", mode == 1) { mode = 1; renderDashboard() })
        outer.addView(seg)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        if (mode == 0) buildWebhookPanel(s, body) else buildBotPanel(s, body)
        scroll.addView(body)
        outer.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        content.addView(outer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun buildWebhookPanel(s: Server, body: LinearLayout) {
        body.addView(sectionLabel("محتوى الرسالة"))
        val contentF = field("اكتب رسالتك هنا...")
        body.addView(contentF)

        body.addView(sectionLabel("تخصيص (اختياري)"))
        val userF = field("اسم مستعار للبوت")
        val avatarF = field("رابط صورة مستعارة")
        body.addView(userF); body.addView(avatarF)

        body.addView(sectionLabel("Embed (اختياري)"))
        val embedTitleF = field("عنوان الـ Embed")
        val embedDescF = field("وصف الـ Embed")
        body.addView(embedTitleF); body.addView(embedDescF)

        var embedColor = s.color
        val swatchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.START }
        for (c in swatchColors) {
            swatchRow.addView(View(this).apply {
                background = circleDrawable(c)
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply { setMargins(0, 0, dp(10), dp(12)) }
                setOnClickListener { embedColor = c }
            })
        }
        body.addView(swatchRow)

        val status = TextView(this).apply { setTextColor(txtMuted); textSize = 13f }
        body.addView(actionButton("إرسال عبر Webhook", blurple) {
            status.text = "جارٍ الإرسال..."
            DiscordApi.sendWebhook(
                s.webhookUrl, contentF.text.toString(), userF.text.toString(), avatarF.text.toString(),
                embedTitleF.text.toString(), embedDescF.text.toString(), embedColor
            ) { ok, msg -> status.setTextColor(if (ok) green else red); status.text = msg }
        })
        body.addView(status)
    }

    private fun buildBotPanel(s: Server, body: LinearLayout) {
        body.addView(sectionLabel("توكن البوت"))
        val masked = if (s.botToken.isBlank()) "لم يُضف بعد" else "•".repeat(minOf(20, s.botToken.length)) + " (محفوظ)"
        body.addView(TextView(this).apply { text = masked; setTextColor(txtPrimary) })

        val testStatus = TextView(this).apply { setTextColor(txtMuted); textSize = 13f }
        body.addView(actionButton("اختبار التوكن", bgDarkest) {
            testStatus.text = "جارٍ التحقق..."
            DiscordApi.testBotToken(s.botToken) { ok, msg -> testStatus.setTextColor(if (ok) green else red); testStatus.text = msg }
        })
        body.addView(testStatus)

        body.addView(sectionLabel("إرسال رسالة كبوت"))
        val chanF = field("معرّف القناة (Channel ID)", s.channelId)
        val msgF = field("نص الرسالة")
        body.addView(chanF); body.addView(msgF)

        val sendStatus = TextView(this).apply { setTextColor(txtMuted); textSize = 13f }
        body.addView(actionButton("إرسال عبر البوت", blurple) {
            s.channelId = chanF.text.toString().trim()
            Prefs.save(this, servers)
            sendStatus.text = "جارٍ الإرسال..."
            DiscordApi.sendBotMessage(s.botToken, chanF.text.toString().trim(), msgF.text.toString()) { ok, msg ->
                sendStatus.setTextColor(if (ok) green else red); sendStatus.text = msg
            }
        })
        body.addView(sendStatus)
    }
}
