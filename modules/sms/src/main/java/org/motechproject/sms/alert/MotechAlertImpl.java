package org.motechproject.sms.alert;

import org.motechproject.admin.service.StatusMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Helper class - Uses StatusMessageService to send system Alerts
 */
@Service
public class MotechAlertImpl implements  MotechAlert {
    private StatusMessageService statusMessageService;

    @Autowired
    public MotechAlertImpl(StatusMessageService statusMessageService) {
        this.statusMessageService = statusMessageService;
    }

    public void alert(String message) {
        statusMessageService.warn(message, "motech-sms");
    }
}
