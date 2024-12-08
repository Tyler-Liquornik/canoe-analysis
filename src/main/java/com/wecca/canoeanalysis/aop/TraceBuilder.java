package com.wecca.canoeanalysis.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

@Slf4j
public class TraceBuilder {

    private final ObjectMapper mapper;
    private final Deque<Long> startTimeStack = new ArrayDeque<>();

    public TraceBuilder() {
        this.mapper = new ObjectMapper();
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
        inputMap.put("methodName", methodName);
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        inputMap.put("simpleClassName", simpleClassName);

        String[] parameterNames = methodSignature.getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        StringBuilder parameters = new StringBuilder();
        if (parameterNames != null && parameterValues != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (i > 0) parameters.append(", ");
                parameters.append(parameterNames[i]).append(": ");
                try {
                    parameters.append(mapper.writeValueAsString(parameterValues[i]));
                } catch (JsonProcessingException e) {
                    parameters.append("null");
                }
            }
        }

        inputMap.put("parameters", parameters.toString());
        return inputMap;
    }

    /**
     * Log details for class and input parameters of an advised method or static field/block invoked
     * @param joinPoint the execution point at which to introspect details of the running thread
     */
    public void beforeAdvice(JoinPoint joinPoint) {
        startTimeStack.push(System.nanoTime());
        Map<String, String> inputMap = buildLogForAspect(joinPoint);
        String parameters = inputMap.get("parameters");
        log.info(String.format("%sInvoking %s::%s %s",
                getLogPrefix(joinPoint),
                inputMap.get("simpleClassName"),
                inputMap.get("methodName"),
                parameters.isEmpty() ? "" : "with inputs [" + parameters + "]"));
    }

    /**
     * Log details for class and returned values of an advised method or static field/block being exited
     * @param joinPoint the execution point at which to introspect details of the running thread
     * @param result the object returned from the advised method
     */
    public void afterAdvice(JoinPoint joinPoint, Object result) {
        long durationNs = System.nanoTime() - startTimeStack.pop();

        String logPrefix = getLogPrefix(joinPoint);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String returnType = signature.getReturnType().getSimpleName();

        // Determine the optimal unit for duration formatting
        String timeInfo;
        if (durationNs >= 1_000_000)
            timeInfo = String.format("(%d ms)", durationNs / 1_000_000);
        else if (durationNs >= 1_000)
            timeInfo = String.format("(%d μs)", durationNs / 1_000);
        else
            timeInfo = String.format("(%d ns)", durationNs);

        String message = returnType.equals("void")
                ? String.format("%s%s Exiting %s::%s with no response", logPrefix, timeInfo,  className, methodName)
                : String.format("%s%s Exiting %s::%s with response %s", logPrefix, timeInfo, className, methodName, serializeResult(result));

        log.info(message);
    }

    /**
     * Serialize an object in a JSON string
     * @param o the object to serialize
     * @return the JSON string
     */
    private String serializeResult(Object o) {
        if (o == null)
            return "null";
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
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
