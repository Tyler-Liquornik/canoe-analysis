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

    public Map<String, Object> buildLogForAspect(JoinPoint joinPoint){
        Map<String, Object> inputMap = new HashMap<>();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String methodName = joinPoint.getSignature().getName();
        inputMap.put("methodName", methodName);
        String className = joinPoint.getSignature().getDeclaringTypeName();
        inputMap.put("className", className);
        Object[] inputParams = joinPoint.getArgs();
        try {
            String inputs = mapper.writeValueAsString(inputParams);
            inputMap.put("inputs", inputs);
        }
        catch (JsonProcessingException ignored) {}
        return inputMap;
    }

    public void beforeAdviceMethod(JoinPoint joinPoint) {
        try {
            Map<String, Object> inputMap = buildLogForAspect(joinPoint);
            String inputs = mapper.writeValueAsString(inputMap.get("inputs"));

            log.info(String.format("Invoking method %s in class %s %s",
                    mapper.writeValueAsString(inputMap.get("methodName")),
                    mapper.writeValueAsString(inputMap.get("className")),
                    inputs.equals("\"[[]]\"") || inputs.equals("\"[]\"") ? "" : "with inputs as " + inputs));
        }
        catch (JsonProcessingException ignored) {}
    }

    public void afterAdviceMethod(JoinPoint joinPoint, Object result){
        try {
            Map<String, Object> inputMap = buildLogForAspect(joinPoint);
            log.info(String.format("Response %s from method %s in class %s",
                    mapper.writeValueAsString(result),
                    mapper.writeValueAsString(inputMap.get("methodName")),
                    mapper.writeValueAsString(inputMap.get("className"))));
        }
        catch (JsonProcessingException ignored) {}
    }
}
