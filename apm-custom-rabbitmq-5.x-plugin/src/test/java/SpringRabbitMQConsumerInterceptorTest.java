import org.apm.plugin.spring.rabbitmq.SpringRabbitMQCallbackInterceptor;
import org.apm.plugin.spring.rabbitmq.SpringRabbitMQConsumerInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpringRabbitMQConsumerInterceptorTest {

    private SpringRabbitMQCallbackInterceptor interceptor;
//    private SpringRabbitMQConsumerInterceptor interceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private Message message;

    @Mock
    private Method method;

    @Mock
    private CorrelationData correlationData;
    @Mock
    private ReturnedMessage returnedMessage;

    @Mock
    private MessageProperties messageProperties;

    @Before
    public void setUp() {
//        interceptor = new SpringRabbitMQConsumerInterceptor();
        interceptor = new SpringRabbitMQCallbackInterceptor();
        when(message.getMessageProperties()).thenReturn(messageProperties);
    }

    @Test
    public void testBeforeMethod() throws Throwable {
        // 设置测试数据
        when(messageProperties.getReceivedExchange()).thenReturn("testExchange");
        when(messageProperties.getReceivedRoutingKey()).thenReturn("testRoutingKey");
        when(method.getName()).thenReturn("confirm");
        when(returnedMessage.getMessage()).thenReturn(message);
        when(correlationData.getReturned()).thenReturn(returnedMessage);


        // 执行方法
        interceptor.beforeMethod(enhancedInstance, method, new Object[]{new CorrelationData("123")}, null, null);

        // 验证结果
        verify(messageProperties).getReceivedExchange();
        verify(messageProperties).getReceivedRoutingKey();
        // 添加更多断言来验证 span 的创建和标签设置
    }

    // 添加更多测试方法...
}