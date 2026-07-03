package com.gytxtx.openjbd.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Material2ResourceTest {
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    @Test
    public void appThemeDefinesCompleteMaterial2ColorRoles() throws Exception {
        assertCompleteMaterial2ColorRoles("values/styles.xml");
        assertCompleteMaterial2ColorRoles("values-v23/styles.xml");
        assertCompleteMaterial2ColorRoles("values-night-v23/styles.xml");
    }

    @Test
    public void themesDefineExpectedMaterial2PaletteAndDarkElevationSurfaces() throws Exception {
        assertColor("values/colors.xml", "accent_dark", "#007A55");
        assertColor("values/colors.xml", "error", "#B00020");
        assertColor("values/colors.xml", "on_error", "#FFFFFF");
        assertColor("values-night/colors.xml", "app_bg", "#121212");
        assertColor("values-night/colors.xml", "surface_elevation_2", "#242424");
        assertColor("values-night/colors.xml", "surface_elevation_4", "#272727");
        assertColor("values-night/colors.xml", "surface_elevation_8", "#2D2D2D");
    }

    @Test
    public void textIconsAndOutlinedComponentsMeetMinimumContrast() throws Exception {
        assertContrastAtLeast("values/colors.xml", "text_primary", "app_bg", 4.5);
        assertContrastAtLeast("values/colors.xml", "text_secondary", "app_bg", 4.5);
        assertContrastAtLeast("values/colors.xml", "icon_default", "app_bg", 3.0);
        assertContrastAtLeast("values/colors.xml", "card_outline", "surface", 3.0);
        assertContrastAtLeast("values/colors.xml", "on_primary", "primary", 4.5);
        assertContrastAtLeast("values/colors.xml", "on_primary", "primary_dark", 4.5);
        assertContrastAtLeast("values/colors.xml", "on_secondary", "accent", 4.5);
        assertContrastAtLeast("values/colors.xml", "on_error", "error", 4.5);

        assertContrastAtLeast("values-night/colors.xml", "text_primary", "app_bg", 4.5);
        assertContrastAtLeast("values-night/colors.xml", "text_secondary", "app_bg", 4.5);
        assertContrastAtLeast("values-night/colors.xml", "icon_default", "app_bg", 3.0);
        assertContrastAtLeast("values-night/colors.xml", "card_outline", "surface", 3.0);
        assertContrastAtLeast("values-night/colors.xml", "on_primary", "primary", 4.5);
        assertContrastAtLeast("values-night/colors.xml", "on_primary", "primary_dark", 4.5);
        assertContrastAtLeast("values-night/colors.xml", "on_secondary", "accent", 4.5);
        assertContrastAtLeast("values-night/colors.xml", "on_error", "error", 4.5);
    }

    @Test
    public void toolbarUsesPrimaryMaterial2Colors() throws Exception {
        Element style = style(parse("values/styles.xml"), "Widget.OpenJbd.Toolbar");

        assertItem(style, "android:background", "?attr/colorPrimary");
        assertItem(style, "titleTextColor", "?attr/colorOnPrimary");
        assertItem(style, "navigationIconTint", "?attr/colorOnPrimary");
    }

    @Test
    public void decorativeRowIconsAreExcludedFromAccessibilityTree() throws Exception {
        assertDecorativeImage("layout/row_setting_item.xml", "img_setting_icon");
        assertDecorativeImage("layout/row_setting_item.xml", "img_setting_chevron");
        assertDecorativeImage("layout/row_device_list_item.xml", "img_device_type");
    }

    @Test
    public void everyLayoutImageHasExplicitAccessibilitySemantics() throws Exception {
        File layoutDirectory = new File("src/main/res/layout");
        File[] files = layoutDirectory.listFiles((directory, name) -> name.endsWith(".xml"));
        assertNotNull("Layout directory must be readable", files);

        for (File file : files) {
            Document document = parse("layout/" + file.getName());
            assertImagesHaveAccessibilitySemantics(file.getName(), document, "ImageView");
            assertImagesHaveAccessibilitySemantics(file.getName(), document, "ImageButton");
        }
    }

    @Test
    public void dynamicConnectionStatusUsesPoliteLiveRegions() throws Exception {
        assertLiveRegion("layout/activity_device_list.xml", "txt_device_status");
        assertLiveRegion("layout/fragment_overview.xml", "txt_reconnect_banner_body");
        assertLiveRegion("layout/fragment_overview.xml", "txt_status");
        assertLiveRegion("layout/activity_dashboard.xml", "txt_dashboard_status");
    }

    @Test
    public void layoutTextUsesStringResources() throws Exception {
        File layoutDirectory = new File("src/main/res/layout");
        File[] files = layoutDirectory.listFiles((directory, name) -> name.endsWith(".xml"));
        assertNotNull("Layout directory must be readable", files);

        for (File file : files) {
            Document document = parse("layout/" + file.getName());
            NodeList elements = document.getElementsByTagName("*");
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                String text = element.getAttributeNS(ANDROID_NAMESPACE, "text");
                if (!text.isEmpty() && !text.startsWith("@string/")) {
                    fail(file.getName() + " contains literal android:text: " + text);
                }
            }
        }
    }

    @Test
    public void standardLayoutsUseMaterial2TextAppearancesInsteadOfHardcodedTextSizes() throws Exception {
        File layoutDirectory = new File("src/main/res/layout");
        File[] files = layoutDirectory.listFiles((directory, name) ->
                name.endsWith(".xml") && !"activity_dashboard.xml".equals(name));
        assertNotNull("Layout directory must be readable", files);

        for (File file : files) {
            Document document = parse("layout/" + file.getName());
            NodeList elements = document.getElementsByTagName("*");
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                if (element.hasAttributeNS(ANDROID_NAMESPACE, "textSize")) {
                    fail(file.getName() + " contains hardcoded textSize on " + element.getTagName());
                }
            }
        }
    }

    private static Document parse(String relativePath) throws Exception {
        File file = new File("src/main/res", relativePath);
        assertEquals("Resource file must exist", true, file.isFile());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static Element style(Document document, String name) {
        for (Element element : elements(document, "style")) {
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        throw new AssertionError("Missing style: " + name);
    }

    private static void assertItem(Element style, String name, String expectedValue) {
        for (Element item : childElements(style, "item")) {
            if (name.equals(item.getAttribute("name"))) {
                assertEquals("Unexpected value for " + name, expectedValue, item.getTextContent().trim());
                return;
            }
        }
        throw new AssertionError("Missing item " + name + " in style " + style.getAttribute("name"));
    }

    private static void assertCompleteMaterial2ColorRoles(String relativePath) throws Exception {
        Element style = style(parse(relativePath), "AppTheme");
        assertItem(style, "colorSecondaryVariant", "@color/accent_dark");
        assertItem(style, "colorError", "@color/error");
        assertItem(style, "colorOnError", "@color/on_error");
    }

    private static void assertColor(String relativePath, String name, String expectedValue) throws Exception {
        assertEquals("Unexpected value for color " + name, expectedValue, colorValue(relativePath, name));
    }

    private static String colorValue(String relativePath, String name) throws Exception {
        Document document = parse(relativePath);
        for (Element color : elements(document, "color")) {
            if (name.equals(color.getAttribute("name"))) {
                return color.getTextContent().trim();
            }
        }
        throw new AssertionError("Missing color " + name + " in " + relativePath);
    }

    private static void assertContrastAtLeast(
            String relativePath,
            String foregroundName,
            String backgroundName,
            double minimum) throws Exception {
        String foreground = colorValue(relativePath, foregroundName);
        String background = colorValue(relativePath, backgroundName);
        double contrast = contrastRatio(foreground, background);
        if (contrast < minimum) {
            fail(relativePath + " contrast " + foregroundName + "/" + backgroundName
                    + " was " + contrast + ", expected at least " + minimum);
        }
    }

    private static double contrastRatio(String first, String second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126 * linearChannel(red)
                + 0.7152 * linearChannel(green)
                + 0.0722 * linearChannel(blue);
    }

    private static double linearChannel(int value) {
        double channel = value / 255.0;
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static void assertDecorativeImage(String relativePath, String id) throws Exception {
        Document document = parse(relativePath);
        Element image = null;
        for (Element element : elements(document, "ImageView")) {
            if (("@+id/" + id).equals(element.getAttributeNS(ANDROID_NAMESPACE, "id"))) {
                image = element;
                break;
            }
        }
        assertNotNull("Missing ImageView: " + id, image);
        assertEquals("@null", image.getAttributeNS(ANDROID_NAMESPACE, "contentDescription"));
        assertEquals("no", image.getAttributeNS(ANDROID_NAMESPACE, "importantForAccessibility"));
    }

    private static void assertImagesHaveAccessibilitySemantics(
            String fileName,
            Document document,
            String tagName) {
        for (Element image : elements(document, tagName)) {
            String contentDescription = image.getAttributeNS(ANDROID_NAMESPACE, "contentDescription");
            String importantForAccessibility = image.getAttributeNS(ANDROID_NAMESPACE, "importantForAccessibility");
            if (contentDescription.isEmpty() && !"no".equals(importantForAccessibility)) {
                fail(fileName + " contains " + tagName + " without explicit accessibility semantics");
            }
        }
    }

    private static void assertLiveRegion(String relativePath, String id) throws Exception {
        Document document = parse(relativePath);
        Element target = null;
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (("@+id/" + id).equals(element.getAttributeNS(ANDROID_NAMESPACE, "id"))) {
                target = element;
                break;
            }
        }
        assertNotNull("Missing view: " + id, target);
        assertEquals("polite", target.getAttributeNS(ANDROID_NAMESPACE, "accessibilityLiveRegion"));
    }

    private static List<Element> elements(Document document, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static List<Element> childElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element && tagName.equals(node.getNodeName())) {
                result.add((Element) node);
            }
        }
        return result;
    }
}
