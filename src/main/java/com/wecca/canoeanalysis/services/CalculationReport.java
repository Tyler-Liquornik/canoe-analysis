package com.wecca.canoeanalysis.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A snapshot of equations and the unrounded values from an actual calculation. */
public final class CalculationReport {
    public record Step(String symbol, String equation, String substitution, double value, String unit) {}
    private final String title;
    private final List<Step> steps = new ArrayList<>();
    public CalculationReport(String title) { this.title = title; }
    public void add(String symbol, String equation, String substitution, double value, String unit) {
        steps.add(new Step(symbol, equation, substitution, value, unit));
    }
    public List<Step> steps() { return List.copyOf(steps); }
    public String title() { return title; }
    public static String number(double value) { return String.format(Locale.ROOT, "%.8g", value); }
    public String text() {
        StringBuilder out = new StringBuilder(title + "\n\nLast calculation snapshot; later form edits are not included. Recalculate after editing. Values retain 8 significant digits.\n\n");
        for (Step step : steps) out.append(step.symbol()).append(" = ").append(step.equation())
                .append("\n    = ").append(step.substitution()).append("\n    = ")
                .append(number(step.value())).append(" ").append(step.unit()).append("\n\n");
        return out.toString();
    }
    public String html() {
        StringBuilder out = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>" + escape(title)
                + "</title><style>body{max-width:900px;margin:40px auto;color:#111;font:18px Georgia}section{break-inside:avoid;margin:28px 0}p{margin:8px 0;white-space:pre-wrap}small{font:14px sans-serif}@media print{body{margin:0}}</style><h1>" + escape(title)
                + "</h1><small>PADDL last calculation snapshot; recalculate after editing inputs. Values shown to 8 significant digits. Units are stated with each result.</small>");
        for (Step step : steps) out.append("<section>").append(ReportMath.render(step.symbol() + " = " + step.equation()))
                .append(ReportMath.render(step.symbol() + " = " + step.substitution()))
                .append(ReportMath.render(step.symbol() + " = " + number(step.value()) + " " + step.unit())).append("</section>");
        return out.toString();
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
