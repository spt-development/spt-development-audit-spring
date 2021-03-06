package com.spt.development.audit.spring;

import com.spt.development.cid.CorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JmsAuditEventWriterTest {
    private interface TestData {
        String DESTINATION_NAME = "test-destination-queue";
        String CORRELATION_ID = "bfae9d36-fdac-48b4-85a0-e5381d872c16";
        String AUDIT_EVENT_TYPE = "DOMAIN_TYPE";
        String AUDIT_EVENT_SUB_TYPE = "CREATE";
    }

    @BeforeEach
    void setUp() {
        CorrelationId.set(TestData.CORRELATION_ID);
    }

    @AfterEach
    void tearDown() {
        CorrelationId.reset();
    }

    @Test
    void write_validAuditEvent_shouldDelegateToJmaTemplate() throws Exception {
        final JmsAuditEventWriterArgs args = new JmsAuditEventWriterArgs();

        createWriter(args).write(
                AuditEvent.builder()
                        .type(TestData.AUDIT_EVENT_TYPE)
                        .subType(TestData.AUDIT_EVENT_SUB_TYPE)
                        .build()
        );

        final ArgumentCaptor<MessageCreator> messageCreatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);

        verify(args.jmsTemplate, times(1)).send(eq(TestData.DESTINATION_NAME), messageCreatorCaptor.capture());

        final Session session = Mockito.mock(Session.class);
        final TextMessage message = Mockito.mock(TextMessage.class);

        when(session.createTextMessage(anyString())).thenAnswer(iom -> {
            when(message.getText()).thenReturn(iom.getArgument(0));

            return message;
        });

        final Message createdMessage = messageCreatorCaptor.getValue().createMessage(session);

        assertThat(createdMessage, is(message));

        final AuditEvent auditEvent = AuditEvent.fromJson(message.getText());

        assertThat(auditEvent.getType(), is(TestData.AUDIT_EVENT_TYPE));
        assertThat(auditEvent.getSubType(), is(TestData.AUDIT_EVENT_SUB_TYPE));

        verify(message, times(1)).setJMSCorrelationID(TestData.CORRELATION_ID);
    }

    private JmsAuditEventWriter createWriter(JmsAuditEventWriterArgs args) {
        return new JmsAuditEventWriter(TestData.DESTINATION_NAME, args.jmsTemplate);
    }

    private static class JmsAuditEventWriterArgs {
        JmsTemplate jmsTemplate = Mockito.mock(JmsTemplate.class);
    }
}