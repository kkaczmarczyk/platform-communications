package org.motechproject.sms.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.motechproject.config.service.ConfigurationService;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.sms.audit.SmsRecord;
import org.motechproject.sms.configs.Config;
import org.motechproject.sms.configs.ConfigProp;
import org.motechproject.sms.configs.ConfigReader;
import org.motechproject.sms.configs.Configs;
import org.motechproject.sms.service.OutgoingSms;
import org.motechproject.sms.audit.SmsAuditService;
import org.motechproject.sms.templates.Response;
import org.motechproject.sms.templates.Template;
import org.motechproject.sms.templates.TemplateReader;
import org.motechproject.sms.templates.Templates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.motechproject.commons.date.util.DateUtil.now;
import static org.motechproject.sms.audit.DeliveryStatus.*;
import static org.motechproject.sms.audit.SmsType.OUTBOUND;
import static org.motechproject.sms.event.SmsEvents.*;

/**
 * This is the main meat - here we talk to the providers using HTTP
 */
@Service
public class SmsHttpService {

    private Logger logger = LoggerFactory.getLogger(SmsHttpService.class);
    private ConfigReader configReader;
    private Configs configs;
    private Templates templates;
    @Autowired
    private EventRelay eventRelay;
    @Autowired
    private HttpClient commonsHttpClient;
    @Autowired
    private MotechSchedulerService schedulerService;
    @Autowired
    private SmsAuditService smsAuditService;
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    public SmsHttpService(@Qualifier("smsSettings") SettingsFacade settingsFacade, TemplateReader templateReader) {

        //todo: unified module-wide caching & refreshing strategy
        configReader = new ConfigReader(settingsFacade);
        configs = configReader.getConfigs();
        templates = templateReader.getTemplates();
    }

    static private String printableMethodParams(HttpMethod method) {
        if (method.getClass().equals(PostMethod.class)) {
            PostMethod p = (PostMethod)method;
            StringBuilder sb = new StringBuilder();
            for(org.apache.commons.httpclient.NameValuePair pair : p.getParameters()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("%s: %s", pair.getName(), pair.getValue()));
            }
            return "POST Parameters: " + sb.toString();
        }
        else if (method.getClass().equals(GetMethod.class)) {
            GetMethod g = (GetMethod)method;
            return String.format("GET QueryString: %s", g.getQueryString());
        }

        return "Eeek!";
    }

    public synchronized void send(OutgoingSms sms) {
        Boolean error = false;
        Config config = configs.getConfigOrDefault(sms.getConfig());
        Template template = templates.getTemplate(config.getTemplateName());
        HttpMethod httpMethod = null;
        Integer failureCount = sms.getFailureCount();
        Integer httpStatus = null;
        String httpResponse = null;
        List<String> failedRecipients = new ArrayList<String>();
        Boolean providerResponseParsingError = false;
        Map<String, String> errorMessages = new HashMap<String, String>();

        Map<String, String> props = new HashMap<String, String>();
        props.put("recipients", template.recipientsAsString(sms.getRecipients()));
        props.put("message", sms.getMessage());
        props.put("motechId", sms.getMotechId());
        props.put("callback", configurationService.getPlatformSettings().getServerUrl() + "/module/sms/status/" +
                config.getName());

        for (ConfigProp configProp : config.getProps()) {
            props.put(configProp.getName(), configProp.getValue());
        }

        // ***** WARNING *****
        // This displays usernames & passwords in the server log! But then again, so does the settings UI...
        // ***** WARNING *****
        if (logger.isDebugEnabled()) {
            for (String key : props.keySet()) {
                logger.debug("PROP {}: {}", key, props.get(key));
            }
        }

        try {
            httpMethod = template.generateRequestFor(props);
            logger.debug(printableMethodParams(httpMethod));
            if (template.getOutgoing().hasAuthentication()) {
                if (props.containsKey("username") && props.containsKey("password")) {
                    String u = props.get("username");
                    String p = props.get("password");
                    commonsHttpClient.getParams().setAuthenticationPreemptive(true);
                    commonsHttpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(u, p));
                }
                else {
                    if (props.containsKey("username")) {
                        throw new IllegalStateException(String.format("Config %s: missing password",
                                config.getName()));
                    }
                    else if (props.containsKey("password")) {
                        throw new IllegalStateException(String.format("Config %s: missing username",
                                config.getName()));
                    }
                    else {
                        throw new IllegalStateException(String.format("Config %s: missing username and password",
                                config.getName()));
                    }
                }
            }

            httpStatus = commonsHttpClient.executeMethod(httpMethod);
            httpResponse = httpMethod.getResponseBodyAsString();

            logger.debug("HTTP status:{}, response:{}", httpStatus, httpResponse.replace("\n", "\\n"));

            //todo: serialize access to configs, ie: one provider may allow 100 sms/min and another may allow 10...
            //This prevents us from sending more messages per second than the provider allows
            Integer milliseconds = template.getOutgoing().getMillisecondsBetweenMessages();
            logger.debug("Sleeping {}ms", milliseconds);
            try {
                Thread.sleep(milliseconds);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            logger.debug("Thread id {}", Thread.currentThread().getId());
        }
        catch (Exception e) {
            String errorMessage = String.format("Error while communicating with '%s': %s", config.getName(), e);
            logger.error(errorMessage);
            errorMessages.put("all", errorMessage);
            error = true;
        }
        finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }

        String msgForLog = sms.getMessage().replace("\n", "\\n");
        Response templateResponse = template.getOutgoing().getResponse();

        if (!error) {
            if ((templateResponse.hasSuccessStatus() && (templateResponse.checkSuccessStatus(httpStatus))) || //does this template know what success is & do we have success?
                    (!templateResponse.hasSuccessStatus() && httpStatus == 200)) { //or do we just have generic success?
                //
                // analyze sms provider's response
                //
                if (templateResponse.supportsMultiLineRecipientResponse()) {
                    for (String responseLine : httpResponse.split("\\r?\\n")) {
                        // todo: as of now, assume all providers return one msgid & recipient per line
                        // todo: but if we discover a provider doesn't, we'll add code here...

                        // Some multi-line response providers have a special case for single recipients
                        if (sms.getRecipients().size() == 1 && templateResponse.supportsSingleRecipientResponse()) {
                            String messageId = templateResponse.extractSingleSuccessMessageId(responseLine);
                            if (messageId != null) {
                                //
                                // success
                                //
                                logger.info(String.format("Successfully sent messageId %s '%s' to %s",
                                        messageId, msgForLog, sms.getRecipients().get(0)));
                                smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND,
                                    sms.getRecipients().get(0), sms.getMessage(), now(), DISPATCHED, null,
                                        sms.getMotechId(), messageId, null));
                                eventRelay.sendEventMessage(makeOutboundSmsEvent(OUTBOUND_SMS_DISPATCHED,
                                        config.getName(), sms.getRecipients(), sms.getMessage(), sms.getMotechId(),
                                        messageId, null, null));
                            }
                            else {
                                //
                                // failure
                                //
                                error = true;
                                String failureMessage = templateResponse.extractSingleFailureMessage(responseLine);
                                logger.error(String.format("Failed to sent message '%s' to %s: %s", msgForLog,
                                        sms.getRecipients().get(0), failureMessage));
                                errorMessages.put(sms.getRecipients().get(0), failureMessage);
                                failedRecipients.add(sms.getRecipients().get(0));
                                eventRelay.sendEventMessage(makeOutboundSmsEvent(OUTBOUND_SMS_RETRYING,
                                        config.getName(), sms.getRecipients(), sms.getMessage(), sms.getMotechId(),
                                        messageId, null, null));
                            }
                        }
                        else {
                            String[] messageAndRecipient = templateResponse.extractSuccessMessageIdAndRecipient(responseLine);

                            if (messageAndRecipient != null) {
                                //
                                // success
                                //
                                logger.info(String.format("Successfully sent messageId %s '%s' to %s",
                                    messageAndRecipient[0], msgForLog, messageAndRecipient[1]));
                                smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, messageAndRecipient[1],
                                        sms.getMessage(), now(), DISPATCHED, null, sms.getMotechId(),
                                        messageAndRecipient[0],null));
                                eventRelay.sendEventMessage(makeOutboundSmsEvent(OUTBOUND_SMS_DISPATCHED,
                                        config.getName(), Arrays.asList(new String[]{messageAndRecipient[1]}),
                                        sms.getMessage(), sms.getMotechId(), messageAndRecipient[0], null, null));
                            }
                            else {
                                //
                                // failure
                                //
                                error = true;
                                messageAndRecipient = templateResponse.extractFailureMessageAndRecipient(responseLine);
                                if (messageAndRecipient == null) {
                                    providerResponseParsingError = true;
                                    logger.error(String.format(
                                        "Failed to sent message '%s', likely config or template error: unable to parse provider's response: %s",
                                        msgForLog, responseLine));
                                    //todo: do we really want to log that or is the tomcat log (above) sufficient?? YES LOG IT ALSO
                                    errorMessages.put("all", responseLine);
                                }
                                else {
                                    logger.error(String.format("Failed to sent message '%s' to %s: %s", msgForLog,
                                        messageAndRecipient[1], messageAndRecipient[0]));
                                    failedRecipients.add(messageAndRecipient[1]);
                                    errorMessages.put(messageAndRecipient[1], messageAndRecipient[0]);
                                }
                            }
                        }
                    }
                }
                else if (templateResponse.hasSuccessResponse() &&
                        !templateResponse.checkSuccessResponse(httpResponse)) {
                    error = true;

                    String failureMessage = templateResponse.extractSingleFailureMessage(httpResponse);
                    if (failureMessage != null) {
                        logger.error(String.format("Failed to sent message '%s' to %s: %s", msgForLog,
                                sms.getRecipients().get(0), failureMessage));
                    }
                    else {
                        logger.error(String.format("Failed to sent message '%s' to %s: %s", msgForLog,
                                sms.getRecipients().get(0), httpResponse));
                    }
                }
                else {
                    //
                    // Either straight HTTP 200, or matched successful response
                    //
                    String providerMessageId;

                    if (templateResponse.hasHeaderMessageId()) {
                        Header header = httpMethod.getResponseHeader(templateResponse.getHeaderMessageId());
                        providerMessageId = header.getValue();
                    }
                    else if (templateResponse.hasSingleSuccessMessageId()) {
                        providerMessageId = templateResponse.extractSingleSuccessMessageId(httpResponse);
                    }
                    else {
                        //weird provider who responds but has no message id
                        providerMessageId = "";
                    }
                logger.info("SMS with message \"{}\" successfully routed to {}", msgForLog,
                        template.recipientsAsString(sms.getRecipients()));
                eventRelay.sendEventMessage(makeOutboundSmsEvent(OUTBOUND_SMS_DISPATCHED, sms.getConfig(),
                        sms.getRecipients(), sms.getMessage(), sms.getMotechId(), providerMessageId,
                        sms.getDeliveryTime(), failureCount));
                smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, sms.getRecipients().get(0),
                        sms.getMessage(), now(), DISPATCHED, null, sms.getMotechId(), providerMessageId, null));
                }
            }
            else {
                error = true;
                String key = sms.getRecipients().size() == 1 ? sms.getRecipients().get(0) : "all";

                String failureMessage = templateResponse.extractGeneralFailureMessage(httpResponse);
                if (failureMessage != null) {
                    logger.error("Delivery to SMS provider failed with HTTP {}: {}", httpStatus, failureMessage);
                    errorMessages.put(key, failureMessage);
                }
                else {
                    logger.error("Delivery to SMS provider failed with HTTP {}: {}", httpStatus, httpResponse);
                    errorMessages.put(key, httpResponse);
                }
            }
        }

        if (error) {
            failureCount++;

            if (failureCount < config.getMaxRetries()) {
                logger.error("SMS delivery retry {} of {}", failureCount, config.getMaxRetries());
                eventRelay.sendEventMessage(makeSendEvent(sms.getConfig(), failedRecipients, sms.getMessage(),
                        sms.getMotechId(), null, sms.getDeliveryTime(), failureCount));
                if (errorMessages.containsKey("all")) {
                    smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, failedRecipients.toString(),
                            sms.getMessage(), now(), RETRYING, null, sms.getMotechId(), null,
                            errorMessages.get("all")));
                }
                else {
                    for (String recipient : failedRecipients) {
                        smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, recipient, sms.getMessage(),
                                now(), RETRYING, null, sms.getMotechId(), null, errorMessages.get(recipient)));
                    }
                }
            }
            else {
                logger.error("SMS delivery retry {} of {}, maximum reached, abandoning", failureCount,
                        config.getMaxRetries());
                eventRelay.sendEventMessage(makeOutboundSmsEvent(OUTBOUND_SMS_ABORTED, sms.getConfig(), failedRecipients,
                        sms.getMessage(), sms.getMotechId(), null, sms.getDeliveryTime(), failureCount));
                if (errorMessages.containsKey("all")) {
                    smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, failedRecipients.toString(),
                            sms.getMessage(), now(), ABORTED, null, sms.getMotechId(), null, errorMessages.get("all")));
                }
                else {
                    for (String recipient : failedRecipients) {
                        smsAuditService.log(new SmsRecord(config.getName(), OUTBOUND, recipient, sms.getMessage(),
                                now(), ABORTED, null, sms.getMotechId(), null, errorMessages.get(recipient)));
                    }
                }
            }
        }
    }
}
