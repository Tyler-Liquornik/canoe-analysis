package com.wecca.canoeanalysis.services;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;

/** Native JavaFX layout of report fractions and radicals, using the export's MathML. */
final class ReportEquationView {
    private ReportEquationView() {}
    static Node equation(String expression) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return layout(factory.newDocumentBuilder().parse(new InputSource(new StringReader(ReportMath.render(expression)))).getDocumentElement());
        } catch (Exception exception) {
            return label(expression);
        }
    }
    private static Node layout(org.w3c.dom.Node element) {
        if (element.getNodeName().equals("mfrac")) {
            Node numerator = layout(element.getFirstChild());
            Node denominator = layout(element.getLastChild());
            javafx.scene.layout.Region line = new javafx.scene.layout.Region();
            line.setMinHeight(1); line.setPrefHeight(1); line.setMaxHeight(1);
            line.setStyle("-fx-background-color: #222;");
            VBox fraction = new VBox(3, numerator, line, denominator);
            fraction.setAlignment(Pos.CENTER); fraction.setPadding(new Insets(2, 5, 2, 5));
            return fraction;
        }
        if (element.getNodeName().equals("msqrt")) {
            Node content = layout(element.getFirstChild());
            VBox radicand = new VBox(content);
            radicand.setStyle("-fx-border-color: #222 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            HBox radical = new HBox(label("√"), radicand); radical.setAlignment(Pos.CENTER); return radical;
        }
        if (element.getNodeName().equals("mtext") || element.getNodeName().equals("mi") || element.getNodeName().equals("mo"))
            return label(element.getTextContent());
        HBox row = new HBox(5); row.setAlignment(Pos.CENTER_LEFT);
        for (org.w3c.dom.Node child = element.getFirstChild(); child != null; child = child.getNextSibling())
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) row.getChildren().add(layout(child));
        return row;
    }
    private static Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Cambria Math'; -fx-font-size: 20px; -fx-text-fill: #111;");
        label.setMinWidth(Label.USE_PREF_SIZE); return label;
    }
}
