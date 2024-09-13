package org.apm.plugin.spring.rabbitmq;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Method;

public class SpringRabbitMQProducerInterceptor implements InstanceMethodsAroundInterceptor {
    public static final String OPERATE_NAME_PREFIX = "SpringRabbitMQ/";
    public static final String PRODUCER_OPERATE_NAME_SUFFIX = "/Producer";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String exchange = (String) allArguments[0];
        String routingKey = (String) allArguments[1];
        Message message = (Message) allArguments[2];
        MessageProperties properties = message.getMessageProperties();

        // todo 远程地址可以改为由jvm传入
        AbstractSpan span = ContextManager.createExitSpan(
                OPERATE_NAME_PREFIX + "Topic/" +
                        exchange + "RoutingKey/" +
                        routingKey + PRODUCER_OPERATE_NAME_SUFFIX,
                "rabbitmq");

        span.setComponent(ComponentsDefine.RABBITMQ_PRODUCER);
        SpanLayer.asMQ(span);
        Tags.MQ_BROKER.set(span, "rabbitmq");
        Tags.MQ_TOPIC.set(span, exchange);
        Tags.MQ_QUEUE.set(span, routingKey);

        if (message != null && message.getMessageProperties() != null) {
            MessageProperties props = message.getMessageProperties();
            span.tag("message_id", props.getMessageId());
            span.tag("correlation_id", props.getCorrelationId());
        }
        ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            properties.setHeader(next.getHeadKey(), next.getHeadValue());
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }
}