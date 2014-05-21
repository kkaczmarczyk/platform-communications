package org.motechproject.commcare.domain;

import com.google.gson.annotations.SerializedName;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

import javax.jdo.annotations.Join;
import java.util.List;

@Entity
public class CommcareApplicationJson {

    @SerializedName("name")
    @Field
    private String applicationName;

    @SerializedName("resource_uri")
    @Field
    private String resourceUri;

    @SerializedName("modules")
    @Join
    private List<CommcareModuleJson> modules;

    public CommcareApplicationJson(String applicationName, String resourceUri, List<CommcareModuleJson> modules) {
        this.applicationName = applicationName;
        this.resourceUri = resourceUri;
        this.modules = modules;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public List<CommcareModuleJson> getModules() {
        return modules;
    }

    public void setModules(List<CommcareModuleJson> modules) {
        this.modules = modules;
    }
}
