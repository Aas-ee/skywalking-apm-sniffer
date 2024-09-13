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

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpringRabbitMQConsumerInterceptorTest {

    private SpringRabbitMQConsumerInterceptor interceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @Before
    public void setUp() {
        interceptor = new SpringRabbitMQConsumerInterceptor();
        when(message.getMessageProperties()).thenReturn(messageProperties);
    }

    @Test
    public void testBeforeMethod() throws Throwable {
        // 设置测试数据
        when(messageProperties.getReceivedExchange()).thenReturn("testExchange");
        when(messageProperties.getReceivedRoutingKey()).thenReturn("testRoutingKey");

        // 执行方法
        interceptor.beforeMethod(enhancedInstance, null, new Object[]{null, message}, null, null);

        // 验证结果
        verify(messageProperties).getReceivedExchange();
        verify(messageProperties).getReceivedRoutingKey();
        // 添加更多断言来验证 span 的创建和标签设置
    }

    // 添加更多测试方法...
}