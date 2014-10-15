package org.dasein.cloud.azure.compute.vm;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureSSLSocketFactory;
import org.dasein.cloud.azure.AzureX509;
import org.dasein.cloud.azure.compute.vm.Model.AzureVirtualMachine;
import org.dasein.cloud.azure.compute.vm.Model.AzureVirtualMachines;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.identity.ServiceAction;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vlad_Munthiu on 10/8/2014.
 */
public class AzureVirtualMachineSupport extends AbstractVirtualMachineSupport {
    static public final String HOSTED_SERVICES = "https://management.core.windows.net/%s/services/hostedservices";

    private Azure provider;

    public AzureVirtualMachineSupport(Azure provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    protected HttpUriRequest getListVirtualMachinesRequest() {
        //all headers, parameters specific to driver goes here.
        RequestBuilder requestBuilder = RequestBuilder.get();
        requestBuilder.setUri(String.format(HOSTED_SERVICES, this.provider.getContext().getAccountNumber()));
        requestBuilder.addHeader("x-ms-version", "2012-03-01");
        requestBuilder.addHeader("Content-Type", "application/xml;charset=UTF-8");

        return requestBuilder.build();
    }

    @Override
    protected HttpClientBuilder getHttpClient() throws InternalException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // all auth, keep alive strategies, retry handlers that are driver specific goes here.
        HttpClientBuilder builder = HttpClientBuilder.create();

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", new AzureSSLSocketFactory(new AzureX509(provider)))
                .build();

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
        builder.setConnectionManager(ccm);

        return builder;
    }

    @Override
    protected ResponseHandler<Iterable<VirtualMachine>> getListVirtualMachineResponseHandler() {
        return new DaseinResponseHandler<Iterable<VirtualMachine>, AzureVirtualMachines>(new AzureVirtualMachineMapper(), AzureVirtualMachines.class);

        /*return new DaseinResponseHandler<Iterable<VirtualMachine>, AzureVirtualMachines>(new DriverToCoreMapper<Iterable<VirtualMachine>, AzureVirtualMachines>() {
            @Override
            public Iterable<VirtualMachine> mapFrom(AzureVirtualMachines entity) {
                ArrayList<VirtualMachine> virtualMachinesResult = new ArrayList<VirtualMachine>();
                for (AzureVirtualMachine azureVirtualMachine : entity.getAzureVirtualMachines()){
                    VirtualMachine virtualMachine = new VirtualMachine();
                    virtualMachine.setName(azureVirtualMachine.getName());
                    virtualMachinesResult.add(virtualMachine);
                }
                return virtualMachinesResult;
            }
        }, AzureVirtualMachines.class);*/
    }

    class AzureVirtualMachineMapper implements DriverToCoreMapper<Iterable<VirtualMachine>, AzureVirtualMachines>{

        @Override
        public Iterable<VirtualMachine> mapFrom(AzureVirtualMachines entity) {
            ArrayList<VirtualMachine> virtualMachinesResult = new ArrayList<VirtualMachine>();
            for (AzureVirtualMachine azureVirtualMachine : entity.getAzureVirtualMachines()){
                VirtualMachine virtualMachine = new VirtualMachine();
                virtualMachine.setName(azureVirtualMachine.getServiceName());
                virtualMachinesResult.add(virtualMachine);
            }
            return virtualMachinesResult;
        }
    }


    private HttpClientBuilder getAzureHttpClientBuilder(){
        HttpClientBuilder builder = HttpClientBuilder.create();

        Registry<ConnectionSocketFactory> registry = null;
        try {
            registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", new AzureSSLSocketFactory(new AzureX509(provider)))
                    .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        }

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
        builder.setConnectionManager(ccm);

        return builder;
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        RequestBuilder requestBuilder = RequestBuilder.get();
        requestBuilder.setUri(String.format(HOSTED_SERVICES, this.provider.getContext().getAccountNumber()));
        requestBuilder.addHeader("x-ms-version", "2012-03-01");
        requestBuilder.addHeader("Content-Type", "application/xml;charset=UTF-8");


        DaseinResponseHandler<Iterable<VirtualMachine>, AzureVirtualMachines> requestHandler =
                new DaseinResponseHandler<Iterable<VirtualMachine>, AzureVirtualMachines>(new AzureVirtualMachineMapper(), AzureVirtualMachines.class);

        return new DaseinRequestExecutor<Iterable<VirtualMachine>, AzureVirtualMachines>(this.provider,
                getAzureHttpClientBuilder(), requestBuilder.build(), requestHandler).execute();
    }
    /**
     * Scales a virtual machine in accordance with the specified scaling options. Few clouds will support all possible
     * options. Therefore a client should check with the cloud's [VMScalingCapabilities] to see what can be scaled.
     * To support the widest variety of clouds, a client should be prepared for the fact that the returned virtual
     * machine will actually be different from the original. However, it isn't proper vertical scaling if the new VM
     * has a different state or if the old VM is still running. Ideally, it's just the same VM with it's new state.
     *
     * @param vmId    the virtual machine to scale
     * @param options the options governing how the virtual machine is scaled
     * @return a virtual machine representing the scaled virtual machine
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     */
    @Override
    public VirtualMachine alterVirtualMachine(@Nonnull String vmId, @Nonnull VMScalingOptions options) throws InternalException, CloudException {
        return null;
    }

    /**
     * Allows certain properties of a virtual machine  to be changed in accordance with the specified  options.
     *
     * @param vmId      the virtual machine to scale
     * @param firewalls the options governing how the virtual machine is scaled
     * @return a virtual machine representing the scaled virtual machine
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     */
    @Override
    public VirtualMachine modifyInstance(@Nonnull String vmId, @Nonnull String[] firewalls) throws InternalException, CloudException {
        return null;
    }

    /**
     * Cancels the data feed for Spot VMs
     *
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     */
    @Override
    public void cancelSpotDataFeedSubscription() throws CloudException, InternalException {

    }

    /**
     * Cancels and removes a request for Spot VMs
     *
     * @param providerSpotVirtualMachineRequestID the ID of the SpotVirtualMachineRequest to be cancelled
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     */
    @Override
    public void cancelSpotVirtualMachineRequest(String providerSpotVirtualMachineRequestID) throws CloudException, InternalException {

    }

    /**
     * Clones an existing virtual machine into a new copy.
     *
     * @param vmId        the ID of the server to be cloned
     * @param intoDcId    the ID of the data center in which the new server will operate
     * @param name        the name of the new server
     * @param description a description for the new server
     * @param powerOn     power on the new server
     * @param firewallIds a list of firewall IDs to protect the new server
     * @return a newly deployed server cloned from the original
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     */
    @Nonnull
    @Override
    public VirtualMachine clone(@Nonnull String vmId, @Nonnull String intoDcId, @Nonnull String name, @Nonnull String description, boolean powerOn, @Nullable String... firewallIds) throws InternalException, CloudException {
        return null;
    }

    /**
     * Creates a SpotVirtualMachineRequest fitting the options specified in the SpotVirtualMachineRequestCreateOptions
     *
     * @param options the configuration options for the spot VM request
     * @return a newly created SpotVirtualMachineRequest
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     */
    @Nonnull
    @Override
    public SpotVirtualMachineRequest createSpotVirtualMachineRequest(SpotVirtualMachineRequestCreateOptions options) throws CloudException, InternalException {
        return null;
    }

    /**
     * Turns extended analytics off for the target server. If the underlying cloud does not support
     * hypervisor monitoring, this method will be a NO-OP.
     *
     * @param vmId the provider ID for the server to stop monitoring
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public void disableAnalytics(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * Turns extended hypervisor analytics for the target server. If the underlying cloud does not support
     * extended analytics, this method will be a NO-OP.
     *
     * @param vmId the provider ID for the server to start monitoring
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public void enableAnalytics(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * Creates the datafeed for Spot VMs, enabling you to view Spot VMs usage logs.
     *
     * @param bucketName the object storage bucket to which the logs will be written
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Override
    public void enableSpotDataFeedSubscription(String bucketName) throws CloudException, InternalException {

    }

    /**
     * Provides access to meta-data about virtual machine capabilities in the current region of this cloud.
     *
     * @return a description of the features supported by this region of this cloud
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides the password as stored by the cloud provider (sometimes encrypted)
     *
     * @param vmId the unique ID of the target server
     * @return the current password of the virtual machine as stored by the provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nullable
    @Override
    public String getPassword(@Nonnull String vmId) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides the userData as stored by the cloud provider (encrypted)
     *
     * @param vmId the unique ID of the target server
     * @return the current userData of the virtual machine as stored by the provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nullable
    @Override
    public String getUserData(@Nonnull String vmId) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides all output from the console of the target server since the specified Unix time.
     *
     * @param vmId the unique ID of the target server
     * @return the current output from the server console
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public String getConsoleOutput(@Nonnull String vmId) throws InternalException, CloudException {
        return null;
    }

    /**
     * Fetches the VM product associated with a specific product ID.
     *
     * @param productId the virtual machine product ID (flavor, size, etc.)
     * @return the product represented by the specified product ID
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation fetching the product
     * @throws org.dasein.cloud.CloudException    an error occurred fetching the product from the cloud
     */
    @Nullable
    @Override
    public VirtualMachineProduct getProduct(@Nonnull String productId) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides hypervisor statistics for the specified server that fit within the defined time range.
     * For clouds that do not provide hypervisor statistics, this method should return an empty
     * {@link org.dasein.cloud.compute.VmStatistics} object and NOT <code>null</code>.
     *
     * @param vmId the unique ID for the target server
     * @param from the beginning of the timeframe for which you want statistics
     * @param to   the end of the timeframe for which you want statistics
     * @return the statistics for the target server
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public VmStatistics getVMStatistics(@Nonnull String vmId, @Nonnegative long from, @Nonnegative long to) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides hypervisor statistics for the specified server that fit within the defined time range.
     * For clouds that do not provide hypervisor statistics, this method should return an empty
     * list.
     *
     * @param vmId the unique ID for the target server
     * @param from the beginning of the timeframe for which you want statistics
     * @param to   the end of the timeframe for which you want statistics
     * @return a collection of discreet server statistics over the specified period
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public Iterable<VmStatistics> getVMStatisticsForPeriod(@Nonnull String vmId, @Nonnegative long from, @Nonnegative long to) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides the status as determined by the cloud provider
     *
     * @param vmIds the unique ID(s) of the target server(s)
     * @return the status(es) of the virtual machines
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nullable
    @Override
    public Iterable<VirtualMachineStatus> getVMStatus(@Nullable String... vmIds) throws InternalException, CloudException {
        return null;
    }

    /**
     * Lists all virtual machines status(es) matching the given {@link org.dasein.cloud.compute.VmStatusFilterOptions) belonging to the account owner
     * currently in the cloud. The filtering functionality is delegated to the cloud provider.
     *
     * @param filterOptions filter options
     * @param filterOptions
     * @return the status(es) of the virtual machines
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     * @throws CloudException    an error occurred within the cloud provider
     */
    @Nullable
    @Override
    public Iterable<VirtualMachineStatus> getVMStatus(@Nullable VmStatusFilterOptions filterOptions) throws InternalException, CloudException {
        return null;
    }

    /**
     * Indicates whether this account is subscribed to using virtual machines.
     *
     * @return true if the subscription is valid for using virtual machines
     * @throws org.dasein.cloud.CloudException    an error occurred querying the cloud for subscription info
     * @throws org.dasein.cloud.InternalException an error occurred within the implementation determining subscription state
     */
    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return false;
    }

    /**
     * Preferred mechanism for launching a virtual machine in the cloud. This method accepts a rich set of launch
     * configuration options that define what the virtual machine should look like once launched. These options may
     * include things that behave very differently in some clouds. It is expected that the method will return
     * immediately once Dasein Cloud as a trackable server ID, even if it has to spawn off a background thread
     * to complete follow on tasks (such as provisioning and attaching volumes).
     *
     * @param withLaunchOptions the launch options to use in creating a new virtual machine
     * @return the newly created virtual machine
     * @throws org.dasein.cloud.CloudException    the cloud provider errored out when launching the virtual machine
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Nonnull
    @Override
    public VirtualMachine launch(@Nonnull VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        return null;
    }

    /**
     * Launches a virtual machine in the cloud. If the cloud supports persistent servers, this method will
     * first define a server and then boot it. The end result of this operation should be a server
     * that is in the middle of booting up.
     *
     * @param fromMachineImageId the provider ID of the image from which the server should be built
     * @param product            the product being provisioned against
     * @param dataCenterId       the provider ID for the data center into which the server will be launched
     * @param name               the name of the new server
     * @param description        a user-friendly description of the new virtual machine
     * @param withKeypairId      the name of the keypair to use for root authentication or null if no keypair
     * @param inVlanId           the ID of the VLAN into which the server should be launched, or null if not specifying (or not supported by the cloud)
     * @param withAnalytics      whether or not hypervisor analytics should be enabled for the virtual machine
     * @param asSandbox          for clouds that require sandboxes for image building, this launches the VM in a sandbox context
     * @param firewallIds        the firewalls to protect the new server
     * @return the newly launched server
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @deprecated use {@link #launch(org.dasein.cloud.compute.VMLaunchOptions)}
     */
    @Nonnull
    @Override
    public VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String... firewallIds) throws InternalException, CloudException {
        return null;
    }

    /**
     * Launches a virtual machine in the cloud. If the cloud supports persistent servers, this method will
     * first define a server and then boot it. The end result of this operation should be a server
     * that is in the middle of booting up.
     *
     * @param fromMachineImageId the provider ID of the image from which the server should be built
     * @param product            the product being provisioned against
     * @param dataCenterId       the provider ID for the data center into which the server will be launched
     * @param name               the name of the new server
     * @param description        a user-friendly description of the new virtual machine
     * @param withKeypairId      the name of the keypair to use for root authentication or null if no keypair
     * @param inVlanId           the ID of the VLAN into which the server should be launched, or null if not specifying (or not supported by the cloud)
     * @param withAnalytics      whether or not hypervisor analytics should be enabled for the virtual machine
     * @param asSandbox          for clouds that require sandboxes for image building, this launches the VM in a sandbox context
     * @param firewallIds        the firewalls to protect the new server
     * @param tags               a list of meta data to pass to the cloud provider
     * @return the newly launched server
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @deprecated use {@link #launch(org.dasein.cloud.compute.VMLaunchOptions)}
     */
    @Nonnull
    @Override
    public VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String[] firewallIds, @Nullable Tag... tags) throws InternalException, CloudException {
        return null;
    }

    /**
     * Launches multiple virtual machines based on the same set of launch options. In clouds that support launching many VMs
     * in a single request, it will perform this operation as a single request. In other VMs, however, it may perform this
     * as parallel calls to {@link #launch(org.dasein.cloud.compute.VMLaunchOptions)}. In the event of parallel launches, this method is considered
     * a success as long as just one virtual machine launches. Thus an error is thrown only if no virtual machines were provisioned.
     *
     * @param withLaunchOptions the launch options that define how the virtual machines will be configured
     * @param count             the number of virtual machines to launch
     * @return a list of virtual machines successfully launched (the number launched may not match the requested number)
     * @throws org.dasein.cloud.CloudException    the cloud provider failed to provision ANY virtual machines
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation (virtual machines may have been provisioned)
     */
    @Nonnull
    @Override
    public Iterable<String> launchMany(@Nonnull VMLaunchOptions withLaunchOptions, @Nonnegative int count) throws CloudException, InternalException {
        return null;
    }

    /**
     * Provides a list of firewalls protecting the specified server. If firewalls are not supported
     * in this cloud, the list will be empty.
     *
     * @param vmId the server ID whose firewall list is being sought
     * @return the list of firewalls protecting the target server
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public Iterable<String> listFirewalls(@Nonnull String vmId) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides a list of instance types, service offerings, or server sizes (however the underlying cloud
     * might describe it) for a particular architecture
     *
     * @param architecture the desired architecture size offerings
     * @return the list of server sizes available for the specified architecture
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public Iterable<VirtualMachineProduct> listProducts(Architecture architecture) throws InternalException, CloudException {
        return null;
    }

    /**
     * Lists all virtual machine products matching the given VirtualMachineProductFilterOptions belonging to the account owner currently in
     * the cloud. The filtering functionality is delegated to the cloud provider.
     *
     * @param options the filter options
     * @return the list of server sizes available matching the filter
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options) throws InternalException, CloudException {
        return null;
    }

    /**
     * Lists all virtual machine products matching the given VirtualMachineProductFilterOptions and specified architecture belonging to the account owner currently in
     * the cloud. The filtering functionality is delegated to the cloud provider.
     *
     * @param options      the filter options
     * @param architecture the desired architecture size offerings
     * @return the list of server sizes available matching the filter
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options, Architecture architecture) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides a list of price history records for Spot VMs
     *
     * @param options filter options
     * @return all price history entries that match the specified filter
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     */
    @Override
    public Iterable<SpotPriceHistory> listSpotPriceHistories(@Nullable SpotPriceHistoryFilterOptions options) throws CloudException, InternalException {
        return null;
    }

    /**
     * Providers a list of spot VM requests
     *
     * @param options
     * @return all spot VM requests that match the specified filter
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     */
    @Override
    public Iterable<SpotVirtualMachineRequest> listSpotVirtualMachineRequests(@Nullable SpotVirtualMachineRequestFilterOptions options) throws CloudException, InternalException {
        return null;
    }

    /**
     * Lists the status for all virtual machines in the current region.
     *
     * @return the status for all virtual machines in the current region
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider
     */
    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
        return null;
    }

    /**
     * Executes a hypervisor pause that essentially removes the virtual machine from the hypervisor scheduler.
     * The virtual machine is considered active and volatile at this point, but it won't actually do anything
     * from  CPU-perspective until it is {@link #unpause(String)}'ed.
     *
     * @param vmId the provider ID for the server to pause
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws OperationNotSupportedException     pausing is not supported for the specified virtual machine
     * @see #unpause(String)
     */
    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * Executes a virtual machine reboot for the target virtual machine.
     *
     * @param vmId the provider ID for the server to reboot
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public void reboot(@Nonnull String vmId) throws CloudException, InternalException {

    }

    /**
     * Resumes a previously suspended virtual machine and returns it to an operational state ({@link org.dasein.cloud.compute.VmState#RUNNING}).
     *
     * @param vmId the virtual machine ID to be resumed
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider in attempting to resume the virtual machine
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     * @throws OperationNotSupportedException     the target virtual machine cannot be suspended/resumed
     * @see #suspend(String)
     */
    @Override
    public void resume(@Nonnull String vmId) throws CloudException, InternalException {

    }

    /**
     * Starts up a virtual machine that was previously stopped (or a VM that is created in a {@link org.dasein.cloud.compute.VmState#STOPPED} state).
     *
     * @param vmId the virtual machine to boot up
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws OperationNotSupportedException     starting/stopping is not supported for this virtual machine
     * @see #stop(String)
     */
    @Override
    public void start(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * Shuts down a virtual machine with the capacity to boot it back up at a later time. The contents of volumes
     * associated with this virtual machine are preserved, but the memory is not. This method should first
     * attempt a nice shutdown, then force the shutdown.
     *
     * @param vmId the virtual machine to be shut down
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws OperationNotSupportedException     starting/stopping is not supported for this virtual machine
     * @see #start(String)
     * @see #stop(String, boolean)
     */
    @Override
    public void stop(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * Shuts down a virtual machine with the capacity to boot it back up at a later time. The contents of volumes
     * associated with this virtual machine are preserved, but the memory is not.
     *
     * @param vmId  the virtual machine to be shut down
     * @param force whether or not to force a shutdown (kill the power)
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws OperationNotSupportedException     starting/stopping is not supported for this virtual machine
     * @see #start(String)
     */
    @Override
    public void stop(@Nonnull String vmId, boolean force) throws InternalException, CloudException {

    }

    /**
     * Suspends a running virtual machine so that the memory is flushed to some kind of persistent storage for
     * the purpose of later resuming the virtual machine in the exact same state.
     *
     * @param vmId the unique ID of the virtual machine to be suspended
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider suspending the virtual machine
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     * @throws OperationNotSupportedException     suspending is not supported for this virtual machine
     * @see #resume(String)
     */
    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {

    }

    /**
     * TERMINATES AND DESTROYS the specified virtual machine. If it is running, it will be stopped. Once it is
     * stopped, all of its data will be destroyed and it will no longer be usable. This is a very
     * dangerous operation, especially in clouds with persistent servers.
     *
     * @param vmId the provider ID of the server to be destroyed
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {

    }

    /**
     * TERMINATES AND DESTROYS the specified virtual machine. If it is running, it will be stopped. Once it is
     * stopped, all of its data will be destroyed and it will no longer be usable. This is a very
     * dangerous operation, especially in clouds with persistent servers.
     *
     * @param vmId        the provider ID of the server to be destroyed
     * @param explanation an optional explanation supplied to the cloud provider for audit purposes describing why the VM was terminated
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Override
    public void terminate(@Nonnull String vmId, @Nullable String explanation) throws InternalException, CloudException {

    }

    /**
     * Executes a hypervisor unpause operation on a currently paused virtual machine, adding it back into the
     * hypervisor scheduler.
     *
     * @param vmId the unique ID of the virtual machine to be unpaused
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider while unpausing
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws OperationNotSupportedException     pausing/unpausing is not supported for the specified virtual machine
     * @see #pause(String)
     */
    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {

    }

    /**
     * Updates meta-data for a virtual machine with the new values. It will not overwrite any value that currently
     * exists unless it appears in the tags you submit.
     *
     * @param vmId the virtual machine to update
     * @param tags the meta-data tags to set
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Override
    public void updateTags(@Nonnull String vmId, @Nonnull Tag... tags) throws CloudException, InternalException {

    }

    /**
     * Updates meta-data for multiple virtual machines with the new values. It will not overwrite any value that currently
     * exists unless it appears in the tags you submit.
     *
     * @param vmIds the virtual machines to update
     * @param tags  the meta-data tags to set
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Override
    public void updateTags(@Nonnull String[] vmIds, @Nonnull Tag... tags) throws CloudException, InternalException {

    }

    /**
     * Removes meta-data from a virtual machine. If tag values are set, their removal is dependent on underlying cloud
     * provider behavior. They may be removed only if the tag value matches or they may be removed regardless of the
     * value.
     *
     * @param vmId the virtual machine to update
     * @param tags the meta-data tags to remove
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Override
    public void removeTags(@Nonnull String vmId, @Nonnull Tag... tags) throws CloudException, InternalException {

    }

    /**
     * Removes meta-data from multiple virtual machines. If tag values are set, their removal is dependent on underlying cloud
     * provider behavior. They may be removed only if the tag value matches or they may be removed regardless of the
     * value.
     *
     * @param vmIds the virtual machine to update
     * @param tags  the meta-data tags to remove
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Override
    public void removeTags(@Nonnull String[] vmIds, @Nonnull Tag... tags) throws CloudException, InternalException {

    }

    /**
     * Describes the ways in which this cloud supports the vertical scaling of a virtual machine. A null response
     * means this cloud just doesn't support vertical scaling.
     *
     * @return a description of how this cloud supports vertical scaling
     * @throws org.dasein.cloud.InternalException an internal error occurred processing the request
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud processing the request
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#getVerticalScalingCapabilities()}
     */
    @Nullable
    @Override
    public VMScalingCapabilities describeVerticalScalingCapabilities() throws CloudException, InternalException {
        return null;
    }

    /**
     * Provides a number between 0 and 100 describing what percentage of the standard VM bill rate should be charged for
     * virtual machines in the specified state. 0 means that the VM incurs no charges while in the specified state, 100
     * means it incurs full charges, and a number in between indicates the percent discount that applies.
     *
     * @param state the VM state being checked
     * @return the discount factor for VMs in the specified state
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#getCostFactor(org.dasein.cloud.compute.VmState)}
     */
    @Override
    public int getCostFactor(@Nonnull VmState state) throws InternalException, CloudException {
        return 0;
    }

    /**
     * Provides the maximum number of virtual machines that may be launched in this region for the current account.
     *
     * @return the maximum number of launchable VMs or {@link Capabilities#LIMIT_UNLIMITED} for unlimited or {@link Capabilities#LIMIT_UNKNOWN} for unknown
     * @throws org.dasein.cloud.CloudException    an error occurred fetching the limits from the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining the limits
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#getMaximumVirtualMachineCount()}
     */
    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return 0;
    }

    /**
     * Assists UIs by providing the cloud-specific term for a virtual server in the cloud.
     *
     * @param locale the locale for which the term should be translated
     * @return the provider-specific term for a virtual server
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#getProviderTermForVirtualMachine(java.util.Locale)}
     */
    @Nonnull
    @Override
    public String getProviderTermForServer(@Nonnull Locale locale) {
        return null;
    }

    /**
     * Identifies whether images of the specified image class are required for launching a VM. This method should
     * always return {@link org.dasein.cloud.Requirement#REQUIRED} when the image class chosen is {@link org.dasein.cloud.compute.ImageClass#MACHINE}.
     *
     * @param cls the desired image class
     * @return the requirements level of support for this image class
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyImageRequirement(org.dasein.cloud.compute.ImageClass)}
     */
    @Nonnull
    @Override
    public Requirement identifyImageRequirement(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates the degree to which specifying a user name and password at launch is required for a Unix operating system.
     *
     * @return the requirements level for specifying a user name and password at launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyPasswordRequirement(org.dasein.cloud.compute.Platform)}
     */
    @Nonnull
    @Override
    public Requirement identifyPasswordRequirement() throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates the degree to which specifying a user name and password at launch is required.
     *
     * @param platform the platform for which password requirements are being sought
     * @return the requirements level for specifying a user name and password at launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyPasswordRequirement(org.dasein.cloud.compute.Platform)}
     */
    @Nonnull
    @Override
    public Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates whether or not a root volume product must be specified when launching a virtual machine.
     *
     * @return the requirements level for a root volume product
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyRootVolumeRequirement()}
     */
    @Nonnull
    @Override
    public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates the degree to which specifying a shell key at launch is required for a Unix operating system.
     *
     * @return the requirements level for shell key support at launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated Use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyShellKeyRequirement(org.dasein.cloud.compute.Platform)}
     */
    @Nonnull
    @Override
    public Requirement identifyShellKeyRequirement() throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates the degree to which specifying a shell key at launch is required.
     *
     * @param platform the target platform for which you are testing
     * @return the requirements level for shell key support at launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyShellKeyRequirement(org.dasein.cloud.compute.Platform)}
     */
    @Nonnull
    @Override
    public Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates the degree to which static IP addresses are required when launching a VM.
     *
     * @return the requirements level for static IP on launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyStaticIPRequirement()}
     */
    @Nonnull
    @Override
    public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates whether or not specifying a VLAN in your VM launch options is required or optional.
     *
     * @return the requirements level for a VLAN during launch
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud identifying this requirement
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation identifying this requirement
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#identifyVlanRequirement()}
     */
    @Nonnull
    @Override
    public Requirement identifyVlanRequirement() throws CloudException, InternalException {
        return null;
    }

    /**
     * Indicates that the ability to terminate the VM via API can be disabled.
     *
     * @return true if the cloud supports the ability to prevent API termination
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud while determining this capability
     * @throws org.dasein.cloud.InternalException an error occurred in the Dasein Cloud implementation determining this capability
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isAPITerminationPreventable()}
     */
    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether or not this cloud provider supports basic analytics. Basic analytics are analytics
     * that are being gathered for every virtual machine without any intervention necessary to enable them. Extended
     * analytics implies basic analytics, so this method should always be true if {@link #isExtendedAnalyticsSupported()}
     * is true (even if there are, in fact, only extended analytics).
     *
     * @return true if the cloud provider supports the gathering of extended analytics
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud provider determining extended analytics support
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining extended analytics support
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isBasicAnalyticsSupported()}
     */
    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether or not this cloud provider supports extended analytics. Extended analytics are analytics
     * that must be specifically enabled above and beyond any basic analytics the cloud provider is gathering.
     *
     * @return true if the cloud provider supports the gathering of extended analytics
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud provider determining extended analytics support
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining extended analytics support
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isExtendedAnalyticsSupported()}
     */
    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether or not the cloud allows bootstrapping with user data.
     *
     * @return true of user-data bootstrapping is supported
     * @throws org.dasein.cloud.CloudException    an error occurred querying the cloud for this kind of support
     * @throws org.dasein.cloud.InternalException an error inside the Dasein Cloud implementation occurred determining support
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isUserDataSupported()}
     */
    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }

    /**
     * Identifies what architectures are supported in this cloud.
     *
     * @return a list of supported architectures
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation calculating supported architectures
     * @throws org.dasein.cloud.CloudException    an error occurred fetching the list of supported architectures from the cloud
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#listSupportedArchitectures()}
     */
    @Override
    public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        return null;
    }

    /**
     * Identifies whether or not this cloud supports hypervisor-based analytics around usage and performance.
     *
     * @return true if this cloud supports hypervisor-based analytics
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining analytics support
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining analytics support
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isBasicAnalyticsSupported()} or {@link org.dasein.cloud.compute.VirtualMachineCapabilities#isExtendedAnalyticsSupported()}
     */
    @Override
    public boolean supportsAnalytics() throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether the ability to pause/unpause a virtual machine is supported for the specified VM.
     *
     * @param vm the virtual machine to verify
     * @return true if pause/unpause is supported for this virtual machine
     * @see #pause(String)
     * @see #unpause(String)
     * @see org.dasein.cloud.compute.VmState#PAUSING
     * @see org.dasein.cloud.compute.VmState#PAUSED
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canPause(org.dasein.cloud.compute.VmState)} or {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canUnpause(org.dasein.cloud.compute.VmState)}
     */
    @Override
    public boolean supportsPauseUnpause(@Nonnull VirtualMachine vm) {
        return false;
    }

    /**
     * Indicates whether the ability to start/stop a virtual machine is supported for the specified VM.
     *
     * @param vm the virtual machine to verify
     * @return true if start/stop operations are supported for this virtual machine
     * @see #start(String)
     * @see #stop(String)
     * @see org.dasein.cloud.compute.VmState#RUNNING
     * @see org.dasein.cloud.compute.VmState#STOPPING
     * @see org.dasein.cloud.compute.VmState#STOPPED
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canStart(org.dasein.cloud.compute.VmState)} or {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canStop(org.dasein.cloud.compute.VmState)}
     */
    @Override
    public boolean supportsStartStop(@Nonnull VirtualMachine vm) {
        return false;
    }

    /**
     * Indicates whether the ability to suspend/resume a virtual machine is supported for the specified VM.
     *
     * @param vm the virtual machine to verify
     * @return true if suspend/resume operations are supported for this virtual machine
     * @see #suspend(String)
     * @see #resume(String)
     * @see org.dasein.cloud.compute.VmState#SUSPENDING
     * @see org.dasein.cloud.compute.VmState#SUSPENDED
     * @deprecated use {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canResume(org.dasein.cloud.compute.VmState)} or {@link org.dasein.cloud.compute.VirtualMachineCapabilities#canSuspend(org.dasein.cloud.compute.VmState)}
     */
    @Override
    public boolean supportsSuspendResume(@Nonnull VirtualMachine vm) {
        return false;
    }

    /**
     * Maps the specified Dasein Cloud service action to an identifier specific to an underlying cloud. If there is
     * no mapping that makes any sense, the method will return an empty array.
     *
     * @param action the Dasein Cloud service action
     * @return a list of cloud-specific IDs (e.g. iam:ListGroups) representing an action with this cloud provider
     */
    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }
}
