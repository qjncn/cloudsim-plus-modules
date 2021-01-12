package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.heuristics.CloudletToVmMappingQlearn;

import java.util.List;

/**
 *
 */
public class DatacenterBrokerQlearn extends DatacenterBrokerSimple {
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    public double meanTime;
    public CloudletToVmMappingQlearn Qlearn;

    /**
     * Creates a DatacenterBroker object.
     *
     * @param simulation The CloudSim instance that represents the simulation the Entity is related to
     */
    public DatacenterBrokerQlearn(final CloudSim simulation,
                                  List<Cloudlet>  cloudletList, List<Vm> vmList) {
        super(simulation);
        this.cloudletList=cloudletList;
        this.vmList=vmList;
        Qlearn= new CloudletToVmMappingQlearn(cloudletList,vmList,this);
        meanTime = Qlearn.getMeanTime();
    }

    /**
     * Selects the VM with the lowest number of PEs that is able to run a given Cloudlet.
     * In case the algorithm can't find such a VM, it uses the
     * default DatacenterBroker VM mapper as a fallback.
     * *选择能够运行给定Cloudlet的PEs最少的VM。
     * *如果算法找不到这样的虚拟机，它使用默认的DatacenterBroker虚拟机映射器作为后备。
     *
     * @param cloudlet the Cloudlet to find a VM to run it
     * @return the VM selected for the Cloudlet or {@link Vm#NULL} if no suitable VM was found
     */
    @Override
    public Vm defaultVmMapper(final Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }
        //todo:添加调用的方法 getMappedVm

        final Vm mappedVm = Qlearn.getMappedVm(cloudlet);



        if (mappedVm == Vm.NULL) {
            LOGGER.warn("{}: {}: {} (PEs: {}) couldn't be mapped to any suitable VM.",
                getSimulation().clockStr(), getName(), cloudlet, cloudlet.getNumberOfPes());
        } else {
            LOGGER.trace("{}: {}: {} (PEs: {}) mapped to {} (available PEs: {}, tot PEs: {})",
                getSimulation().clockStr(), getName(), cloudlet, cloudlet.getNumberOfPes(), mappedVm,
                mappedVm.getExpectedFreePesNumber(), mappedVm.getFreePesNumber());
        }

        return mappedVm;
    }
}
