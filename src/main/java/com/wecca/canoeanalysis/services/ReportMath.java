package com.wecca.canoeanalysis.services;

/** Renders the small arithmetic notation used by calculation reports as native MathML. */
final class ReportMath {
    private ReportMath() {}
    static String render(String expression) {
        return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\">" + node(expression.trim()) + "</math>";
    }
    private static String node(String value) {
        value = value.trim();
        if (value.startsWith("(") && closing(value, 0) == value.length() - 1)
            return "<mrow><mo>(</mo>" + node(value.substring(1, value.length() - 1)) + "<mo>)</mo></mrow>";
        for (String operators : new String[]{"=", "+−", "×/"}) {
            int depth = 0;
            boolean inAbsolute = false;
            for (int i = value.length() - 1; i >= 0; i--) {
                char ch = value.charAt(i);
                if (ch == '|') { inAbsolute = !inAbsolute; continue; }
                if (ch == ')') depth++;
                else if (ch == '(') depth--;
                else if (depth == 0 && !inAbsolute && i > 0 && operators.indexOf(ch) >= 0) {
                    String left = node(value.substring(0, i)), right = node(value.substring(i + 1));
                    if (ch == '/') return "<mfrac>" + left + right + "</mfrac>";
                    return "<mrow>" + left + "<mo>" + ch + "</mo>" + right + "</mrow>";
                }
            }
        }
        if (value.startsWith("|") && value.endsWith("|"))
            return "<mrow><mo>|</mo>" + node(value.substring(1, value.length() - 1)) + "<mo>|</mo></mrow>";
        if (value.startsWith("√(") && closing(value, 1) == value.length() - 1)
            return "<msqrt>" + node(value.substring(2, value.length() - 1)) + "</msqrt>";
        if ((value.startsWith("min(") || value.startsWith("max(")) && closing(value, 3) == value.length() - 1) {
            String args = value.substring(4, value.length() - 1);
            StringBuilder out = new StringBuilder("<mrow><mi>" + value.substring(0, 3) + "</mi><mo>(</mo>");
            int depth = 0, start = 0;
            for (int i = 0; i < args.length(); i++) {
                char ch = args.charAt(i);
                if (ch == '(') depth++; else if (ch == ')') depth--;
                else if (ch == ',' && depth == 0) { out.append(node(args.substring(start, i))).append("<mo>,</mo>"); start = i + 1; }
            }
            return out.append(node(args.substring(start))).append("<mo>)</mo></mrow>").toString();
        }
        return "<mtext>" + value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</mtext>";
    }
    private static int closing(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '(') depth++;
            else if (text.charAt(i) == ')' && --depth == 0) return i;
        }
        return -1;
    }
}
