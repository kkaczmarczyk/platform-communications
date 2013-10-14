package org.motechproject.sms.web;

import org.motechproject.sms.audit.SmsLoggingRecords;
import org.motechproject.sms.audit.SmsRecordSearchCriteria;
import org.motechproject.sms.audit.SmsRecords;
import org.motechproject.sms.service.SmsAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * todo
 */
@Controller
public class LogController {

    @Autowired
    private SmsAuditService smsAuditService;

    @RequestMapping(value = "/log", method = RequestMethod.GET)
    @ResponseBody
    public SmsLoggingRecords getSmsRecords(GridSettings settings) {
        SmsRecords smsRecords = new SmsRecords();
        SmsRecordSearchCriteria criteria = settings.toSmsRecordSearchCriteria();
        if (!criteria.getSmsTypes().isEmpty() && !criteria.getSmsDeliveryStatuses().isEmpty()) {
            smsRecords = smsAuditService.findAllSmsRecords(criteria);
        }
        return new SmsLoggingRecords(settings.getPage(), settings.getRows(), smsRecords);
    }
}
