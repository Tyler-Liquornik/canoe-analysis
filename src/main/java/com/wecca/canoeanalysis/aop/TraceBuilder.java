package com.wecca.canoeanalysis.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

@Slf4j
public class TraceBuilder {

    private final ObjectMapper mapper = new ObjectMapper();

    public TraceBuilder() {
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * @param joinPoint the execution point at which to introspect details of the running thread
     * @return a map of <execution detail name : execution detail value>
     */
    public Map<String, String> buildLogForAspect(JoinPoint joinPoint) {
        Map<String, String> inputMap = new HashMap<>();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getName();
        String returnType = methodSignature.getReturnType().getSimpleName();

        inputMap.put("methodName", String.format("%s %s", methodName, returnType));
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        inputMap.put("simpleClassName", simpleClassName);
        try {
            String inputs = mapper.writeValueAsString(joinPoint.getArgs());
            inputMap.put("inputs", inputs);
        } catch (JsonProcessingException ignored) {}
        return inputMap;
    }

    /**
     * Log details for class and input parameters of an advised method or static field/block invoked
     * @param joinPoint the execution point at which to introspect details of the running thread
     */
    public void beforeAdvice(JoinPoint joinPoint) {
        Map<String, String> inputMap = buildLogForAspect(joinPoint);

        String logPrefix = getLogPrefix(joinPoint);

        String inputs = inputMap.get("inputs");
        log.info(String.format("%sInvoking %s::%s %s",
                logPrefix,
                inputMap.get("simpleClassName"),
                inputMap.get("methodName").split(" ")[0],
                inputs == null || inputs.equals("\"[[]]\"") || inputs.equals("\"[]\"") || inputs.equals("[]") || inputs.equals("[{}]")
                        ? ""
                        : "with inputs as " + inputs));
    }

    /**
     * Log details for class and returned values of an advised method or static field/block being exited
     * @param joinPoint the execution point at which to introspect details of the running thread
     * @param result the object returned from the advised method
     */
    public void afterAdvice(JoinPoint joinPoint, Object result) {
        String logPrefix = getLogPrefix(joinPoint);

        try {
            Map<String, String> inputMap = buildLogForAspect(joinPoint);
            String returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType().getSimpleName();

            if (returnType.equals("void")) {
                log.info(String.format("%sExiting %s::%s with no response",
                        logPrefix,
                        inputMap.get("simpleClassName"),
                        inputMap.get("methodName").split(" ")[0]));
            } else {
                String resultString = result != null ? mapper.writeValueAsString(result) : "null";
                log.info(String.format("%sExiting %s::%s with response %s",
                        logPrefix,
                        inputMap.get("simpleClassName"),
                        inputMap.get("methodName").split(" ")[0],
                        resultString));
            }
        } catch (JsonProcessingException ignored) {}
    }

    /**
     * @return a prefix for the log with the method reference to the method that invoked another method
     */
    private String getLogPrefix(JoinPoint joinPoint) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
            StackTraceElement caller = stackTrace[i];
            String className = caller.getClassName();

            if (!className.contains(TracingAspect.class.getSimpleName()) &&
                    !className.equals(this.getClass().getName()) &&
                    !className.equals(joinPoint.getSignature().getDeclaringTypeName())) {

                String simpleCallerClassName = className.substring(className.lastIndexOf('.') + 1);
                String callerMethodName = caller.getMethodName();
                return String.format("%s::%s -- ", simpleCallerClassName, callerMethodName);
            }
        }
        return "UnknownClass::unknownMethod -- ";
    }
}
