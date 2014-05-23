package org.motechproject.ivr.domain;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.motechproject.mds.annotations.Entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.commons.date.util.DateUtil.now;
import static org.motechproject.commons.date.util.DateUtil.setTimeZoneUTC;


/**
 * Call Detail Record represents call events and data captured in a call along with call metrics.
 */
@Entity
public class CallDetailRecord implements CallDetail {

    private DateTime startDate;
    private DateTime endDate;
    private Date answerDate;
    private CallDisposition disposition;
    private String errorMessage;
    private String phoneNumber;
    private String callId;
    private Integer duration;
    private CallDirection callDirection;
    private Map<String, Object> customProperties = new HashMap<>();
    private List<String> eventLog = new ArrayList<>();


    private CallDetailRecord() {
    }

    public CallDetailRecord(String callId, String phoneNumber) {
        this.callId = callId;
        this.phoneNumber = phoneNumber;
        this.startDate = now();
    }

    /**
     * Constructor to create CallDetailRecord
     *
     * @param startDate
     * @param endDate
     * @param answerDate
     * @param disposition
     * @param duration
     */
    public CallDetailRecord(Date startDate, Date endDate, Date answerDate, CallDisposition disposition, Integer duration) {
        this.startDate = startDate != null ? newDateTime(startDate) : null;
        this.endDate = endDate != null ? newDateTime(endDate) : null;
        this.answerDate = answerDate;
        this.disposition = disposition;
        this.duration = duration;
    }

    /**
     * CallDetailRecord constructor for failed calls
     *
     * @param disposition: Status of call
     * @param errorMessage
     */
    public CallDetailRecord(CallDisposition disposition, String errorMessage) {
        this.errorMessage = errorMessage;
        this.disposition = disposition;
    }

    /**
     * Creates a call details record for given phone number and call details
     *
     * @param phoneNumber:   phone number of user.
     * @param callDirection: Incoming/outgoing
     * @param disposition:   Call status (busy, failed etc)
     * @return
     */
    public static CallDetailRecord create(String phoneNumber, CallDirection callDirection, CallDisposition disposition) {
        CallDetailRecord callDetailRecord = new CallDetailRecord();
        callDetailRecord.startDate = now();
        callDetailRecord.disposition = disposition;
        callDetailRecord.answerDate = callDetailRecord.startDate.toDate();
        callDetailRecord.phoneNumber = phoneNumber;
        callDetailRecord.callDirection = callDirection;
        return callDetailRecord;
    }

    public String getCallId() {
        return callId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public DateTime getStartDate() {
        return setTimeZoneUTC(startDate);
    }

    @Override
    public DateTime getEndDate() {
        return setTimeZoneUTC(endDate);
    }

    public Date getAnswerDate() {
        return answerDate != null ? setTimeZoneUTC(newDateTime(answerDate)).toDate() : answerDate;
    }

    public CallDisposition getDisposition() {
        return disposition;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


    public CallDirection getCallDirection() {
        return callDirection;
    }

    public CallDetailRecord setCallDirection(CallDirection callDirection) {
        this.callDirection = callDirection;
        return this;
    }

    public CallDetailRecord setStartDate(DateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    public CallDetailRecord setEndDate(DateTime endDate) {
        this.endDate = endDate;
        duration = new Period(startDate, endDate).toStandardSeconds().getSeconds();
        return this;
    }

    public CallDetailRecord setAnswerDate(Date answerDate) {
        this.answerDate = answerDate;
        return this;
    }

    public CallDetailRecord setDisposition(CallDisposition disposition) {
        this.disposition = disposition;
        return this;
    }

    public CallDetailRecord setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public CallDetailRecord setCustomProperties(Map<String, Object> customProperties) {
        if (customProperties != null) {
            this.customProperties = customProperties;
        }
        return this;
    }

    public CallDetailRecord addCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
        return this;
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public CallDetailRecord setEventLog(List<String> eventLog) {
        if (eventLog != null) {
            this.eventLog = eventLog;
        }
        return this;
    }

    public CallDetailRecord addEventLog(Object value) {
        //todo: double check happiness about the date string format
        this.eventLog.add(DateTime.now().toLocalDateTime().toString() + " - " + value.toString());
        return this;
    }

    public CallDetailRecord setCallId(String callId) {
        this.callId = callId;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

}
