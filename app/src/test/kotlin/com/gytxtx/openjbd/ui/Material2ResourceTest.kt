package com.gytxtx.openjbd.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class Material2ResourceTest {
    @Test
    fun appThemeDefinesCompleteMaterial2ColorRoles() {
        assertCompleteMaterial2ColorRoles("values/styles.xml")
        assertCompleteMaterial2ColorRoles("values-v23/styles.xml")
        assertCompleteMaterial2ColorRoles("values-night-v23/styles.xml")
    }

    @Test
    fun themesDefineExpectedMaterial2PaletteAndDarkElevationSurfaces() {
        assertColor("values/colors.xml", "accent_dark", "#007A55")
        assertColor("values/colors.xml", "error", "#B00020")
        assertColor("values/colors.xml", "on_error", "#FFFFFF")
        assertColor("values-night/colors.xml", "app_bg", "#121212")
        assertColor("values-night/colors.xml", "surface_elevation_2", "#242424")
        assertColor("values-night/colors.xml", "surface_elevation_4", "#272727")
        assertColor("values-night/colors.xml", "surface_elevation_8", "#2D2D2D")
    }

    @Test
    fun textIconsAndOutlinedComponentsMeetMinimumContrast() {
        assertContrastAtLeast("values/colors.xml", "text_primary", "app_bg", 4.5)
        assertContrastAtLeast("values/colors.xml", "text_secondary", "app_bg", 4.5)
        assertContrastAtLeast("values/colors.xml", "icon_default", "app_bg", 3.0)
        assertContrastAtLeast("values/colors.xml", "card_outline", "surface", 3.0)
        assertContrastAtLeast("values/colors.xml", "on_primary", "primary", 4.5)
        assertContrastAtLeast("values/colors.xml", "on_primary", "primary_dark", 4.5)
        assertContrastAtLeast("values/colors.xml", "on_secondary", "accent", 4.5)
        assertContrastAtLeast("values/colors.xml", "on_error", "error", 4.5)

        assertContrastAtLeast("values-night/colors.xml", "text_primary", "app_bg", 4.5)
        assertContrastAtLeast("values-night/colors.xml", "text_secondary", "app_bg", 4.5)
        assertContrastAtLeast("values-night/colors.xml", "icon_default", "app_bg", 3.0)
        assertContrastAtLeast("values-night/colors.xml", "card_outline", "surface", 3.0)
        assertContrastAtLeast("values-night/colors.xml", "on_primary", "primary", 4.5)
        assertContrastAtLeast("values-night/colors.xml", "on_primary", "primary_dark", 4.5)
        assertContrastAtLeast("values-night/colors.xml", "on_secondary", "accent", 4.5)
        assertContrastAtLeast("values-night/colors.xml", "on_error", "error", 4.5)
    }

    @Test
    fun toolbarUsesPrimaryMaterial2Colors() {
        val style = style(parse("values/styles.xml"), "Widget.OpenJbd.Toolbar")
        assertItem(style, "android:background", "?attr/colorPrimary")
        assertItem(style, "titleTextColor", "?attr/colorOnPrimary")
        assertItem(style, "navigationIconTint", "?attr/colorOnPrimary")
    }

    @Test
    fun appBarsUseStandardHeightInsetsAndNavigationDescriptions() {
        assertToolbar("layout/activity_main.xml", "top_app_bar", "@dimen/top_app_bar_height", "@string/action_select_device")
        assertToolbar("layout/activity_device_list.xml", "device_top_app_bar", "@dimen/top_app_bar_height", "@string/action_back")
        assertToolbar("layout/activity_about.xml", "about_top_app_bar", "@dimen/top_app_bar_height", "@string/action_back")
        assertToolbar("layout/activity_licenses.xml", "licenses_top_app_bar", "@dimen/top_app_bar_height", "@string/action_back")

        assertViewOmitsAndroidAttribute("layout/activity_main.xml", "top_app_bar", "paddingStart")
        assertViewOmitsAndroidAttribute("layout/activity_main.xml", "top_app_bar", "paddingEnd")
        assertViewOmitsAppAttribute("layout/activity_main.xml", "top_app_bar", "subtitle")
        assertViewOmitsAppAttribute("layout/activity_device_list.xml", "device_top_app_bar", "subtitle")
    }

    @Test
    fun devicePermissionErrorDefinesLocalizedRecoveryAction() {
        val button = elementById(parse("layout/activity_device_list.xml"), "btn_device_permission_action")
        assertEquals("gone", button.getAttributeNS(ANDROID_NAMESPACE, "visibility"))
        assertEquals("@style/Widget.MaterialComponents.Button.TextButton", button.getAttribute("style"))

        assertString("values/strings.xml", "action_select_device", "Select device")
        assertString("values/strings.xml", "action_grant_permission", "Grant permission")
        assertString("values/strings.xml", "action_open_app_settings", "Open app settings")
        assertString("values/strings.xml", "device_placeholder_permission_denied_title", "Bluetooth permission required")
        assertString("values/strings.xml", "device_placeholder_permission_denied_subtitle", "Allow Bluetooth permission to scan for nearby BMS devices.")

        assertString("values-zh-rCN/strings.xml", "action_select_device", "选择设备")
        assertString("values-zh-rCN/strings.xml", "action_grant_permission", "授予权限")
        assertString("values-zh-rCN/strings.xml", "action_open_app_settings", "打开应用设置")
        assertString("values-zh-rCN/strings.xml", "device_placeholder_permission_denied_title", "需要蓝牙权限")
        assertString("values-zh-rCN/strings.xml", "device_placeholder_permission_denied_subtitle", "授予蓝牙权限后才能扫描附近的 BMS 设备。")
    }

    @Test
    fun decorativeRowIconsAreExcludedFromAccessibilityTree() {
        assertDecorativeImage("layout/row_setting_item.xml", "img_setting_icon")
        assertDecorativeImage("layout/row_setting_item.xml", "img_setting_chevron")
        assertDecorativeImage("layout/row_device_list_item.xml", "img_device_type")
    }

    @Test
    fun everyLayoutImageHasExplicitAccessibilitySemantics() {
        val layoutDirectory = File("src/main/res/layout")
        val files = layoutDirectory.listFiles { _, name -> name.endsWith(".xml") }
        assertNotNull("Layout directory must be readable", files)

        for (file in files!!) {
            val document = parse("layout/" + file.name)
            assertImagesHaveAccessibilitySemantics(file.name, document, "ImageView")
            assertImagesHaveAccessibilitySemantics(file.name, document, "ImageButton")
        }
    }

    @Test
    fun dynamicConnectionStatusUsesPoliteLiveRegions() {
        assertLiveRegion("layout/activity_device_list.xml", "txt_device_status")
        assertLiveRegion("layout/fragment_overview.xml", "txt_reconnect_banner_body")
        assertLiveRegion("layout/fragment_overview.xml", "txt_status")
        assertLiveRegion("layout/activity_dashboard.xml", "txt_dashboard_status")
    }

    @Test
    fun layoutTextUsesStringResources() {
        val layoutDirectory = File("src/main/res/layout")
        val files = layoutDirectory.listFiles { _, name -> name.endsWith(".xml") }
        assertNotNull("Layout directory must be readable", files)

        for (file in files!!) {
            val document = parse("layout/" + file.name)
            val elements = document.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                val element = elements.item(index) as Element
                val text = element.getAttributeNS(ANDROID_NAMESPACE, "text")
                if (text.isNotEmpty() && !text.startsWith("@string/")) {
                    fail(file.name + " contains literal android:text: " + text)
                }
            }
        }
    }

    @Test
    fun standardLayoutsUseMaterial2TextAppearancesInsteadOfHardcodedTextSizes() {
        val layoutDirectory = File("src/main/res/layout")
        val files = layoutDirectory.listFiles { _, name ->
            name.endsWith(".xml") && name != "activity_dashboard.xml"
        }
        assertNotNull("Layout directory must be readable", files)

        for (file in files!!) {
            val document = parse("layout/" + file.name)
            val elements = document.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                val element = elements.item(index) as Element
                if (element.hasAttributeNS(ANDROID_NAMESPACE, "textSize")) {
                    fail(file.name + " contains hardcoded textSize on " + element.tagName)
                }
            }
        }
    }

    @Test
    fun secondPassDefinesProjectDimensAndTextAppearances() {
        assertDimen("values/dimens.xml", "space_8", "8dp")
        assertDimen("values/dimens.xml", "space_12", "12dp")
        assertDimen("values/dimens.xml", "space_14", "14dp")
        assertDimen("values/dimens.xml", "space_16", "16dp")
        assertDimen("values/dimens.xml", "space_18", "18dp")
        assertDimen("values/dimens.xml", "space_24", "24dp")
        assertDimen("values/dimens.xml", "touch_target_min", "48dp")
        assertDimen("values/dimens.xml", "top_app_bar_height", "56dp")
        assertDimen("values/dimens.xml", "list_top_app_bar_height", "72dp")
        assertDimen("values/dimens.xml", "placeholder_illustration_size", "88dp")

        val styles = style(parse("values/styles.xml"), "TextAppearance.OpenJbd.AboutTitle")
        assertEquals("TextAppearance.MaterialComponents.Headline5", styles.getAttribute("parent"))
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.AboutBody", "TextAppearance.MaterialComponents.Body1")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.ListItemTitle", "TextAppearance.MaterialComponents.Subtitle1")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.ListItemSubtitle", "TextAppearance.MaterialComponents.Body2")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.DashboardStatus", "TextAppearance.MaterialComponents.Subtitle2")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.DashboardLabel", "TextAppearance.MaterialComponents.Subtitle2")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.DashboardPrimaryValue", "TextAppearance.MaterialComponents.Headline3")
        assertStyleParent("values/styles.xml", "TextAppearance.OpenJbd.DashboardMetricValue", "TextAppearance.MaterialComponents.Headline4")
        assertStyleParent("values/styles.xml", "Widget.OpenJbd.SecondaryPanel", "")
    }

    @Test
    fun secondPassLayoutsUseProjectTokensInsteadOfDirectMaterialStyles() {
        assertFileContains("layout/activity_about.xml", "@style/TextAppearance.OpenJbd.AboutTitle")
        assertFileContains("layout/activity_about.xml", "@style/TextAppearance.OpenJbd.AboutBody")
        assertFileDoesNotContain("layout/activity_about.xml", "TextAppearance.MaterialComponents.Headline5")
        assertFileDoesNotContain("layout/activity_about.xml", "TextAppearance.MaterialComponents.Body1")
        assertFileContains("layout/row_about_list_item.xml", "@style/TextAppearance.OpenJbd.ListItemTitle")
        assertFileContains("layout/row_about_list_item.xml", "@style/TextAppearance.OpenJbd.ListItemSubtitle")
        assertFileDoesNotContain("layout/row_about_list_item.xml", "TextAppearance.MaterialComponents.Subtitle1")
        assertFileDoesNotContain("layout/row_about_list_item.xml", "TextAppearance.MaterialComponents.Body2")
        assertFileContains("layout/activity_device_list.xml", "@style/TextAppearance.OpenJbd.DeviceStatus")
        assertFileContains("layout/activity_dashboard.xml", "@style/TextAppearance.OpenJbd.DashboardStatus")
        assertFileContains("layout/activity_dashboard.xml", "@style/TextAppearance.OpenJbd.DashboardPrimaryValue")
        assertFileContains("layout/activity_dashboard.xml", "@style/TextAppearance.OpenJbd.DashboardMetricValue")
        assertFileContains("layout/activity_dashboard.xml", "@style/TextAppearance.OpenJbd.DashboardLabel")
    }

    @Test
    fun secondPassAboutRowIconsRemainDecorative() {
        assertDecorativeImage("layout/row_about_list_item.xml", "img_about_item_icon")
        assertDecorativeImage("layout/row_about_list_item.xml", "img_about_item_chevron")
        assertFileDoesNotContain("layout/row_about_list_item.xml", "@string/action_open")
    }

    @Test
    fun secondPassOverviewUsesSecondaryPanelStyleInsteadOfRawBackgroundBlocks() {
        assertFileContains("layout/fragment_overview.xml", "style=\"@style/Widget.OpenJbd.SecondaryPanel\"")
        assertFileDoesNotContain("layout/fragment_overview.xml", "android:background=\"@color/app_bg\"")
    }

    @Test
    fun settingsScreenUsesLightweightGroupsWithoutOutlinedCards() {
        val document = parse("layout/fragment_settings.xml")

        assertTextViewExists(document, "@string/settings_group_interface")
        assertTextViewExists(document, "@string/settings_group_device")
        assertTextViewExists(document, "@string/settings_group_other")

        assertLinearLayoutList(document, "list_settings_interface")
        assertLinearLayoutList(document, "list_settings_device")
        assertLinearLayoutList(document, "list_settings_other")

        assertFileDoesNotContain("layout/fragment_settings.xml", "style=\"@style/OpenJbdOutlinedCard\"")
        assertFileDoesNotContain("layout/fragment_settings.xml", "<com.google.android.material.card.MaterialCardView")
    }

    @Test
    fun settingsRowKeepsTouchTargetAndPaddingAfterGroupSimplification() {
        assertSettingsRowSpacing(
            "layout/row_setting_item.xml",
            "@dimen/touch_target_min",
            "@dimen/space_16",
            "@dimen/space_10",
            "@dimen/space_16",
            "@dimen/space_10"
        )
    }

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"

        private fun parse(relativePath: String): Document {
            val file = File("src/main/res", relativePath)
            assertEquals("Resource file must exist", true, file.isFile)
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            return factory.newDocumentBuilder().parse(file)
        }

        private fun assertToolbar(
            relativePath: String,
            id: String,
            expectedHeight: String,
            expectedNavigationDescription: String
        ) {
            val toolbar = elementById(parse(relativePath), id)
            assertEquals(expectedHeight, toolbar.getAttributeNS(ANDROID_NAMESPACE, "layout_height"))
            assertEquals(
                expectedNavigationDescription,
                toolbar.getAttributeNS(APP_NAMESPACE, "navigationContentDescription")
            )
        }

        private fun assertViewOmitsAndroidAttribute(relativePath: String, id: String, attribute: String) {
            val view = elementById(parse(relativePath), id)
            assertEquals("", view.getAttributeNS(ANDROID_NAMESPACE, attribute))
        }

        private fun assertViewOmitsAppAttribute(relativePath: String, id: String, attribute: String) {
            val view = elementById(parse(relativePath), id)
            assertEquals("", view.getAttributeNS(APP_NAMESPACE, attribute))
        }

        private fun elementById(document: Document, id: String): Element {
            val elements = document.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                val element = elements.item(index) as Element
                if (("@+id/$id") == element.getAttributeNS(ANDROID_NAMESPACE, "id")) {
                    return element
                }
            }
            throw AssertionError("Missing view: $id")
        }

        private fun assertTextViewExists(document: Document, expectedText: String) {
            for (textView in elements(document, "TextView")) {
                if (expectedText == textView.getAttributeNS(ANDROID_NAMESPACE, "text")) {
                    return
                }
            }
            throw AssertionError("Missing TextView text: $expectedText")
        }

        private fun assertLinearLayoutList(document: Document, id: String) {
            val list = elementById(document, id)
            assertEquals("LinearLayout", list.tagName)
            assertEquals("vertical", list.getAttributeNS(ANDROID_NAMESPACE, "orientation"))
            assertEquals("middle", list.getAttributeNS(ANDROID_NAMESPACE, "showDividers"))
            assertEquals("@drawable/divider_card_stroke", list.getAttributeNS(ANDROID_NAMESPACE, "divider"))
        }

        private fun assertSettingsRowSpacing(
            relativePath: String,
            expectedMinHeight: String,
            expectedPaddingStart: String,
            expectedPaddingTop: String,
            expectedPaddingEnd: String,
            expectedPaddingBottom: String
        ) {
            val row = parse(relativePath).documentElement
            assertEquals(expectedMinHeight, row.getAttributeNS(ANDROID_NAMESPACE, "minHeight"))
            assertEquals(expectedPaddingStart, row.getAttributeNS(ANDROID_NAMESPACE, "paddingStart"))
            assertEquals(expectedPaddingTop, row.getAttributeNS(ANDROID_NAMESPACE, "paddingTop"))
            assertEquals(expectedPaddingEnd, row.getAttributeNS(ANDROID_NAMESPACE, "paddingEnd"))
            assertEquals(expectedPaddingBottom, row.getAttributeNS(ANDROID_NAMESPACE, "paddingBottom"))
        }

        private fun style(document: Document, name: String): Element {
            for (element in elements(document, "style")) {
                if (name == element.getAttribute("name")) {
                    return element
                }
            }
            throw AssertionError("Missing style: $name")
        }

        private fun assertItem(style: Element, name: String, expectedValue: String) {
            for (item in childElements(style, "item")) {
                if (name == item.getAttribute("name")) {
                    assertEquals("Unexpected value for $name", expectedValue, item.textContent.trim())
                    return
                }
            }
            throw AssertionError("Missing item $name in style ${style.getAttribute("name")}")
        }

        private fun assertCompleteMaterial2ColorRoles(relativePath: String) {
            val style = style(parse(relativePath), "AppTheme")
            assertItem(style, "colorSecondaryVariant", "@color/accent_dark")
            assertItem(style, "colorError", "@color/error")
            assertItem(style, "colorOnError", "@color/on_error")
        }

        private fun assertColor(relativePath: String, name: String, expectedValue: String) {
            assertEquals("Unexpected value for color $name", expectedValue, colorValue(relativePath, name))
        }

        private fun assertString(relativePath: String, name: String, expectedValue: String) {
            val document = parse(relativePath)
            for (string in elements(document, "string")) {
                if (name == string.getAttribute("name")) {
                    assertEquals("Unexpected value for string $name", expectedValue, string.textContent.trim())
                    return
                }
            }
            throw AssertionError("Missing string $name in $relativePath")
        }

        private fun assertDimen(relativePath: String, name: String, expectedValue: String) {
            val document = parse(relativePath)
            for (dimen in elements(document, "dimen")) {
                if (name == dimen.getAttribute("name")) {
                    assertEquals("Unexpected value for dimen $name", expectedValue, dimen.textContent.trim())
                    return
                }
            }
            throw AssertionError("Missing dimen $name in $relativePath")
        }

        private fun colorValue(relativePath: String, name: String): String {
            val document = parse(relativePath)
            for (color in elements(document, "color")) {
                if (name == color.getAttribute("name")) {
                    return color.textContent.trim()
                }
            }
            throw AssertionError("Missing color $name in $relativePath")
        }

        private fun assertContrastAtLeast(
            relativePath: String,
            foregroundName: String,
            backgroundName: String,
            minimum: Double
        ) {
            val foreground = colorValue(relativePath, foregroundName)
            val background = colorValue(relativePath, backgroundName)
            val contrast = contrastRatio(foreground, background)
            if (contrast < minimum) {
                fail(
                    "$relativePath contrast $foregroundName/$backgroundName was $contrast, expected at least $minimum"
                )
            }
        }

        private fun contrastRatio(first: String, second: String): Double {
            val firstLuminance = relativeLuminance(first)
            val secondLuminance = relativeLuminance(second)
            val lighter = maxOf(firstLuminance, secondLuminance)
            val darker = minOf(firstLuminance, secondLuminance)
            return (lighter + 0.05) / (darker + 0.05)
        }

        private fun relativeLuminance(color: String): Double {
            val red = Integer.parseInt(color.substring(1, 3), 16)
            val green = Integer.parseInt(color.substring(3, 5), 16)
            val blue = Integer.parseInt(color.substring(5, 7), 16)
            return 0.2126 * linearChannel(red) + 0.7152 * linearChannel(green) + 0.0722 * linearChannel(blue)
        }

        private fun linearChannel(value: Int): Double {
            val channel = value / 255.0
            return if (channel <= 0.04045) channel / 12.92 else Math.pow(
                (channel + 0.055) / 1.055,
                2.4
            )
        }

        private fun assertDecorativeImage(relativePath: String, id: String) {
            val document = parse(relativePath)
            var image: Element? = null
            for (element in elements(document, "ImageView")) {
                if (("@+id/$id") == element.getAttributeNS(ANDROID_NAMESPACE, "id")) {
                    image = element
                    break
                }
            }
            assertNotNull("Missing ImageView: $id", image)
            assertEquals("@null", image!!.getAttributeNS(ANDROID_NAMESPACE, "contentDescription"))
            assertEquals("no", image!!.getAttributeNS(ANDROID_NAMESPACE, "importantForAccessibility"))
        }

        private fun assertImagesHaveAccessibilitySemantics(
            fileName: String,
            document: Document,
            tagName: String
        ) {
            for (image in elements(document, tagName)) {
                val contentDescription = image.getAttributeNS(ANDROID_NAMESPACE, "contentDescription")
                val importantForAccessibility = image.getAttributeNS(ANDROID_NAMESPACE, "importantForAccessibility")
                if (contentDescription.isEmpty() && "no" != importantForAccessibility) {
                    fail("$fileName contains $tagName without explicit accessibility semantics")
                }
            }
        }

        private fun assertLiveRegion(relativePath: String, id: String) {
            val document = parse(relativePath)
            var target: Element? = null
            val elements = document.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                val element = elements.item(index) as Element
                if (("@+id/$id") == element.getAttributeNS(ANDROID_NAMESPACE, "id")) {
                    target = element
                    break
                }
            }
            assertNotNull("Missing view: $id", target)
            assertEquals("polite", target!!.getAttributeNS(ANDROID_NAMESPACE, "accessibilityLiveRegion"))
        }

        private fun assertStyleParent(relativePath: String, styleName: String, expectedParent: String) {
            val style = style(parse(relativePath), styleName)
            assertEquals(expectedParent, style.getAttribute("parent"))
        }

        private fun assertFileContains(relativePath: String, expectedText: String) {
            val content = readResourceFile(relativePath)
            if (!content.contains(expectedText)) {
                fail("$relativePath does not contain expected text: $expectedText")
            }
        }

        private fun assertFileDoesNotContain(relativePath: String, unexpectedText: String) {
            val content = readResourceFile(relativePath)
            if (content.contains(unexpectedText)) {
                fail("$relativePath contains unexpected text: $unexpectedText")
            }
        }

        private fun readResourceFile(relativePath: String): String {
            val file = File("src/main/res", relativePath)
            assertEquals("Resource file must exist", true, file.isFile)
            return String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        }

        private fun elements(document: Document, tagName: String): List<Element> {
            val result = mutableListOf<Element>()
            val nodes = document.getElementsByTagName(tagName)
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                if (node is Element) {
                    result.add(node)
                }
            }
            return result
        }

        private fun childElements(parent: Element, tagName: String): List<Element> {
            val result = mutableListOf<Element>()
            val nodes = parent.childNodes
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                if (node is Element && tagName == node.nodeName) {
                    result.add(node)
                }
            }
            return result
        }
    }
}
