package org.dasein.cloud.azure.compute.vm.Model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 10/14/2014.
 */
@XmlRootElement(name="HostedServices", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AzureVirtualMachines {
    @XmlElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<AzureVirtualMachine> azureVirtualMachines;

    public List<AzureVirtualMachine> getAzureVirtualMachines() {
        return azureVirtualMachines;
    }

    public void setAzureVirtualMachines(List<AzureVirtualMachine> azureVirtualMachines) {
        this.azureVirtualMachines = azureVirtualMachines;
    }
}