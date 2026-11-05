package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.PunchingShearController;
import com.wecca.canoeanalysis.controllers.modules.FailureEnvelopeController;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class CalculationPopupTest {
    @BeforeAll static void toolkit() throws Exception {
        FutureTask<Void> startup = new FutureTask<>(() -> { Platform.setImplicitExit(false); return null; });
        try { Platform.startup(startup); } catch (IllegalStateException alreadyStarted) { Platform.runLater(startup); }
        startup.get(20, TimeUnit.SECONDS);
    }
    @Test void punchingPopupUsesCurrentForceAndRefreshesAfterEditing() throws Exception {
        onFx(() -> {
            MainController main = mainController();
            PunchingShearController.setMainController(main);
            PunchingShearController controller = new PunchingShearController();
            for (String name : List.of("hullThicknessTextField", "compressiveStrengthTextField", "maxShearTextField",
                    "twoWayPCritTextField", "twoWayACritTextField", "twoWayVfTextField", "twoWayVc1TextField",
                    "twoWayVc2TextField", "twoWayVc3TextField", "twoWayVcMinTextField")) set(controller, name, field(""));
            for (String name : List.of("twoWaySafeLabel", "twoWayUnsafeLabel")) set(controller, name, new Label());
            ((JFXTextField)get(controller,"hullThicknessTextField")).setText("12");
            ((JFXTextField)get(controller,"compressiveStrengthTextField")).setText("30");
            JFXTextField force = (JFXTextField)get(controller,"maxShearTextField"); force.setText("900");
            controller.calculateTwoWay();
            CalculationReport first = (CalculationReport)get(controller,"calculationReport");
            assertEquals(900.0 / (4 * (30 + 12) * 12), first.steps().get(2).value(), 1e-12);
            assertTrue(Window.getWindows().stream().anyMatch(w -> w instanceof Stage stage && stage.getTitle().contains("Report algebra") && w.isShowing()));
            force.setText("1800"); controller.calculateTwoWay();
            CalculationReport second = (CalculationReport)get(controller,"calculationReport");
            assertEquals(first.steps().get(2).value() * 2, second.steps().get(2).value(), 1e-12);
            assertTrue(second.steps().get(2).substitution().contains("1800"));
        });
    }
    @Test void failureStressPopupUsesVisibleShearInertia() throws Exception {
        onFx(() -> {
            FailureEnvelopeController.setMainController(mainController());
            FailureEnvelopeController controller = new FailureEnvelopeController();
            String[] names = {"compressionField", "tensionField", "qMax", "momentOfInertia", "shearInertiaField", "canoeThickness", "maxMoment", "maxShear"};
            String[] values = {"0.1", "0.2", "0.001", "0.0002", "0.0003", "0.01", "4", "2"};
            for (int i=0;i<names.length;i++) set(controller,names[i],field(values[i]));
            for (String name : List.of("maxCompression", "maxTension", "maxShearStress")) set(controller,name,field(""));
            controller.calculateValues();
            CalculationReport report=(CalculationReport)get(controller,"calculationReport");
            assertEquals(2 * .001 / (1000 * .0003 * .01),report.steps().get(2).value(),1e-12);
            assertTrue(Window.getWindows().stream().anyMatch(Window::isShowing));
            ((JFXTextField)get(controller,"shearInertiaField")).setText("");
            controller.calculateValues();
            CalculationReport sharedInertia=(CalculationReport)get(controller,"calculationReport");
            assertEquals(2 * .001 / (1000 * .0002 * .01),sharedInertia.steps().get(2).value(),1e-12);
        });
    }
    private static MainController mainController() throws Exception { MainController main=new MainController(); set(main,"primaryStage",new Stage()); return main; }
    private static JFXTextField field(String value) { JFXTextField field=new JFXTextField(); field.setText(value); return field; }
    private static void set(Object object,String name,Object value) throws Exception { Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value); }
    private static Object get(Object object,String name) throws Exception { Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object); }
    private interface FxAction { void run() throws Exception; }
    private static void onFx(FxAction action) throws Exception {
        FutureTask<Void> task=new FutureTask<>(() -> { try { action.run(); } finally { for (Window window : List.copyOf(Window.getWindows())) window.hide(); } return null; });
        Platform.runLater(task);task.get(30,TimeUnit.SECONDS);
    }
}
