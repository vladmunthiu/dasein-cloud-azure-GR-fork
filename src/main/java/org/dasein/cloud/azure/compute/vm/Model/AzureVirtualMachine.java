package org.dasein.cloud.azure.compute.vm.Model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 10/14/2014.
 */
@XmlRootElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AzureVirtualMachine {
    @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
