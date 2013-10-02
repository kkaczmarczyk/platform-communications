package org.motechproject.sms.service;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.sms.constants.SendSmsConstants;
import org.motechproject.sms.model.OutgoingSms;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SendSmsEventHandler {

    private SmsService smsService;

    @MotechListener (subjects = { SendSmsConstants.SEND_SMS })
    public void handle(MotechEvent event) {
        List<String> recipients = (List<String>) event.getParameters().get(SendSmsConstants.RECIPIENTS);
        String message = (String) event.getParameters().get(SendSmsConstants.MESSAGE);
        //TODO: null is ok, make sure we can do that
        DateTime deliveryTime = (DateTime) event.getParameters().get(SendSmsConstants.DELIVERY_TIME);

        smsService.send(new OutgoingSms(recipients, message, deliveryTime));
    }
}

