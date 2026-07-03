package com.gytxtx.openjbd.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        Element style = style(parse("values/styles.xml"), "AppTheme");

        assertItem(style, "colorSecondaryVariant", "@color/accent_dark");
        assertItem(style, "colorError", "@color/error");
        assertItem(style, "colorOnError", "@color/on_error");
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
