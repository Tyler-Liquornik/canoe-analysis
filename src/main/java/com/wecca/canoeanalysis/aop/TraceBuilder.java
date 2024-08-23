package com.wecca.canoeanalysis.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;

@Slf4j
public class TraceBuilder {

    ObjectMapper mapper = new ObjectMapper();

    /**
     * @param joinPoint the execution point at which to introspect details of the running thread
     * @return a map of <execution detail name : execution detail value>
     */
    public Map<String, String> buildLogForAspect(JoinPoint joinPoint){
        Map<String, String> inputMap = new HashMap<>();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String methodName = joinPoint.getSignature().getName();
        inputMap.put("methodName", methodName);
        String className = joinPoint.getSignature().getDeclaringTypeName();
        inputMap.put("className", className);
        try {
            String inputs = mapper.writeValueAsString(joinPoint.getArgs());
            inputMap.put("inputs", inputs);
        }
        catch (JsonProcessingException ignored) {}
        return inputMap;
    }

    /**
     * Log details for class and input parameters of an advised method or static field/block invoked
     * @param joinPoint the execution point at which to introspect details of the running thread
     */
    public void beforeAdviceMethod(JoinPoint joinPoint) {
        Map<String, String> inputMap = buildLogForAspect(joinPoint);
        String inputs = inputMap.get("inputs");
        log.info(String.format("Invoking method %s in class %s %s",
                inputMap.get("methodName"),
                inputMap.get("className"),
                inputs == null || inputs.equals("\"[[]]\"") || inputs.equals("\"[]\"") || inputs.equals("[]") || inputs.equals("[{}]")
                        ? ""
                        : "with inputs as " + inputs));
    }

    /**
     * Log details for class and returned values of an advised method or static field/block being exited
     * @param joinPoint the execution point at which to introspect details of the running thread
     * @param result the object returned from the advised method
     */
    public void afterAdviceMethod(JoinPoint joinPoint, Object result){
        try {
            Map<String, String> inputMap = buildLogForAspect(joinPoint);
            log.info(String.format("Response %s from method %s in class %s",
                    mapper.writeValueAsString(result),
                    inputMap.get("methodName"),
                    inputMap.get("className")));
        }
        catch (JsonProcessingException ignored) {}
    }
}
