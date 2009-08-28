/*
 * Fabric3
 * Copyright (c) 2009 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.binding.jms.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.oasisopen.sca.ServiceRuntimeException;
import org.osoa.sca.ServiceUnavailableException;

import org.fabric3.binding.jms.common.CorrelationScheme;
import org.fabric3.binding.jms.provision.PayloadType;
import org.fabric3.binding.jms.runtime.helper.JmsHelper;
import org.fabric3.binding.jms.runtime.helper.MessageHelper;
import org.fabric3.binding.jms.spi.runtime.TransactionType;
import org.fabric3.spi.binding.format.EncodeCallback;
import org.fabric3.spi.binding.format.EncoderException;
import org.fabric3.spi.binding.format.MessageEncoder;
import org.fabric3.spi.binding.format.ParameterEncoder;
import org.fabric3.spi.invocation.CallFrame;
import org.fabric3.spi.invocation.Message;
import org.fabric3.spi.invocation.MessageImpl;
import org.fabric3.spi.util.Base64;
import org.fabric3.spi.wire.Interceptor;

/**
 * Dispatches an invocation to a destination.
 *
 * @version $Revision$ $Date$
 */
public class JmsInterceptor implements Interceptor {
    private static final Message ONE_WAY_RESPONSE = new MessageImpl();
    private Interceptor next;
    private String methodName;
    private PayloadType payloadType;
    private Destination destination;
    private ConnectionFactory connectionFactory;
    private CorrelationScheme correlationScheme;
    private ResponseListener responseListener;
    private MessageEncoder messageEncoder;
    private ParameterEncoder parameterEncoder;
    private ClassLoader cl;
    private boolean oneWay;
    private TransactionType transactionType;
    private TransactionManager tm;
    private long timeout;

    /**
     * Constructor.
     *
     * @param configuration the configuration template
     */
    public JmsInterceptor(InterceptorConfiguration configuration) {
        WireConfiguration wireConfig = configuration.getWireConfiguration();
        this.destination = wireConfig.getRequestDestination();
        this.connectionFactory = wireConfig.getRequestConnectionFactory();
        this.correlationScheme = wireConfig.getCorrelationScheme();
        this.cl = wireConfig.getClassloader();
        this.responseListener = wireConfig.getResponseListener();
        this.tm = wireConfig.getTransactionManager();
        this.transactionType = wireConfig.getTransactionType();
        this.timeout = wireConfig.getTimeout();
        this.oneWay = configuration.isOneWay();
        this.methodName = configuration.getOperationName();
        this.payloadType = configuration.getPayloadType();
        this.messageEncoder = configuration.getMessageEncoder();
        this.parameterEncoder = configuration.getParameterEncoder();

    }

    public Message invoke(Message message) {
        Connection connection = null;
        Session session = null;
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            // FIXME username/password
            connection = connectionFactory.createConnection();
            connection.start();
            int status = tm.getStatus();
            Transaction suspended = null;
            boolean begun = false;
            if (Status.STATUS_NO_TRANSACTION == status && TransactionType.GLOBAL == transactionType) {
                tm.begin();
                begun = true;
            } else if ((Status.STATUS_ACTIVE == status && TransactionType.NONE == transactionType)) {
                suspended = tm.suspend();
            }

            if (TransactionType.GLOBAL == transactionType) {
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } else {
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }

            MessageProducer producer = session.createProducer(destination);

            javax.jms.Message jmsMessage = createMessage(message, session);

            // enqueue the message
            producer.send(jmsMessage);

            String correlationId = null;
            switch (correlationScheme) {
            case None:
            case RequestCorrelIDToCorrelID:
                throw new UnsupportedOperationException("Correlation scheme not supported");
            case RequestMsgIDToCorrelID:
                correlationId = jmsMessage.getJMSMessageID();
            }
            if (!oneWay) {
                // request-response, block on response
                Message resp = receive(correlationId, session);
                if (begun) {
                    tm.commit();
                }
                if (suspended != null) {
                    tm.resume(suspended);
                }
                return resp;
            } else {
                if (begun) {
                    tm.commit();
                }
                if (suspended != null) {
                    tm.resume(suspended);
                }
                // one-way invocation, return an empty message
                return ONE_WAY_RESPONSE;
            }

        } catch (JMSException ex) {
            throw new ServiceRuntimeException("Unable to receive response", ex);
        } catch (IOException ex) {
            throw new ServiceRuntimeException("Error serializing callframe", ex);
        } catch (SystemException e) {
            throw new ServiceRuntimeException(e);
        } catch (NotSupportedException e) {
            throw new ServiceRuntimeException(e);
        } catch (HeuristicMixedException e) {
            throw new ServiceRuntimeException(e);
        } catch (HeuristicRollbackException e) {
            throw new ServiceRuntimeException(e);
        } catch (RollbackException e) {
            throw new ServiceRuntimeException(e);
        } finally {
            JmsHelper.closeQuietly(session);
            JmsHelper.closeQuietly(connection);
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public Interceptor getNext() {
        return next;
    }

    public void setNext(Interceptor next) {
        this.next = next;
    }

    /**
     * Blocks waiting for a response message from the service provider.
     *
     * @param correlationId the id for correlating the response message
     * @param session       the session to perform the receive in
     * @return the response message
     * @throws JMSException if an error occurs waiting for or processing the response
     */
    private Message receive(String correlationId, Session session) throws JMSException {
        javax.jms.Message resultMessage = responseListener.receive(correlationId, session, timeout);
        if (resultMessage == null) {
            throw new ServiceUnavailableException("Timeout waiting for response to message: " + correlationId);
        }
        Object payload = MessageHelper.getPayload(resultMessage, payloadType);
        Message response = new MessageImpl();
        if (messageEncoder != null) {
            decode(response, payload);
        } else {
            if (resultMessage.getBooleanProperty(JmsConstants.FAULT_HEADER)) {
                response.setBodyWithFault(payload);
            } else {
                response.setBody(payload);
            }
        }
        return response;
    }

    /**
     * Decodes a serialized response message payload.
     *
     * @param response the response message
     * @param payload  the payload
     */
    private void decode(Message response, Object payload) {
        try {
            if (payload == null) {
                throw new ServiceRuntimeException("Response body was null");
            } else if (String.class.equals(payload.getClass())) {
                Message ret = messageEncoder.decodeResponse((String) payload);
                if (ret.isFault()) {
                    Throwable deserialized = parameterEncoder.decodeFault(methodName, (String) ret.getBody());
                    response.setBodyWithFault(deserialized);
                } else {
                    Object deserialized = parameterEncoder.decodeResponse(methodName, (String) ret.getBody());
                    response.setBody(deserialized);
                }
            } else if (byte[].class.equals(payload.getClass())) {
                Message ret = messageEncoder.decodeResponse((byte[]) payload);
                if (ret.isFault()) {
                    Throwable deserialized = parameterEncoder.decodeFault(methodName, (byte[]) ret.getBody());
                    response.setBodyWithFault(deserialized);
                } else {
                    Object deserialized = parameterEncoder.decodeResponse(methodName, (byte[]) ret.getBody());
                    response.setBody(deserialized);
                }
            } else {
                throw new ServiceRuntimeException("Unnown response payload type: " + payload.getClass().getName());
            }
        } catch (EncoderException e) {
            throw new ServiceRuntimeException(e);
        }
    }

    /**
     * Creates a JMS message to be dispatched to the service endpoint from the invocation message.
     *
     * @param message the invocation message
     * @param session the session that will be used for dispatching the message
     * @return the JMS message
     * @throws JMSException if an error occurs creating the JMS message
     * @throws IOException  if an error occurs writing information to the JMS message
     */
    private javax.jms.Message createMessage(Message message, Session session) throws JMSException, IOException {
        Object[] payload = (Object[]) message.getBody();
        javax.jms.Message jmsMessage;
        switch (payloadType) {
        case OBJECT:
            jmsMessage = session.createObjectMessage(payload);
            setRoutingHeaders(message, jmsMessage);
            return jmsMessage;
        case STREAM:
            throw new UnsupportedOperationException("Not yet implemented");
        case TEXT:
            if (payload.length != 1) {
                throw new UnsupportedOperationException("Only single parameter operations are supported");
            }
            if (messageEncoder != null) {
                try {
                    String serialied = parameterEncoder.encodeText(message);
                    message.setBody(serialied);
                    // Add empty string as workaround for ActiveMQ bug:
                    //     https://issues.apache.org/activemq/browse/AMQ-2197
                    TextMessage textMessage = session.createTextMessage("");
                    EncodeCallback callback = new JMSEncodeCallback(textMessage);
                    String serializedMessage = messageEncoder.encodeText(methodName, message, callback);
                    textMessage.setText(serializedMessage);
                    return textMessage;
                } catch (EncoderException e) {
                    throw new ServiceRuntimeException(e);
                }
            } else {
                jmsMessage = session.createTextMessage((String) payload[0]);
                setRoutingHeaders(message, jmsMessage);
                return jmsMessage;
            }
        default:
            if (payload.length != 1) {
                throw new AssertionError("Bytes messages must have a single parameter");
            }
            jmsMessage = MessageHelper.createBytesMessage(session, payload[0], payloadType);
            setRoutingHeaders(message, jmsMessage);
            return jmsMessage;
        }
    }

    /**
     * Adds F3-specific routing headers to a message.
     *
     * @param message    the invocation message
     * @param jmsMessage the JMS message to be dispatched
     * @throws JMSException if an error occurs setting the headers
     * @throws IOException  if an error occurs serializing the routing information
     */
    private void setRoutingHeaders(Message message, javax.jms.Message jmsMessage) throws JMSException, IOException {
        // add the operation name being invoked
        jmsMessage.setObjectProperty("scaOperationName", methodName);

        // serialize the callframes
        List<CallFrame> stack = message.getWorkContext().getCallFrameStack();
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(bas);
        stream.writeObject(stack);
        stream.close();
        String encoded = Base64.encode(bas.toByteArray());
        jmsMessage.setStringProperty("f3Context", encoded);
    }

}
