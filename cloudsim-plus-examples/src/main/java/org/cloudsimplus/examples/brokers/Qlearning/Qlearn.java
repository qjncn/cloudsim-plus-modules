package org.cloudsimplus.examples.brokers.Qlearning;

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerQlearn;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 主程序，负责模拟的各种参数设计和模拟的运行，包括调用Q-learn mapping算法。
 * 116行可以单独设置e的取值。表示cloudlet和vm的数量，建议取10~30以内，太大不容易收敛。配合迭代次数设置。
 */

public class Qlearn {
    private static final int SCHEDULING_INTERVAL = 300;
    private static final int HOSTS = 100;
    private static final int HOST_PES = 8;

    private int CLOUDLETS = 10;//读取data中某列的数据，在某300s内的总数
    private static final int CLOUDLET_LENGTH = 1500;
    private  int VMS =10;

    private final CloudSim simulation;
    //比较不同算法,修改三处broker类
    private static List<DatacenterBrokerQlearn> brokers= new ArrayList<>();

    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private List<Cloudlet> totalCloudletList = new ArrayList<>();
    private DatacenterSimple datacenter0;

    private double batchMeanTime;
    //main
    public static void main(String[] args) {
        new Qlearn();
    }

    private Qlearn() {
        Log.setLevel(Level.OFF);
        simulation = new CloudSim();
        //simulation.addOnClockTickListener(this::onClockTickListener);
        datacenter0 =  createDatacenter();
        //simulation.addOnClockTickListener(this::createAndSubmitCloudletsAndVmsAndBorkers);
        cloudletList = createCloudlets(0);
        vmList = createVms();
        DatacenterBrokerQlearn broker= new DatacenterBrokerQlearn(simulation,cloudletList,vmList);
        brokers.add(broker);
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        simulation.start();
        printResults();
    }



    /**
     * Shows updates every time the simulation clock advances.
     * @param evt information about the event happened (that for this Listener is just the simulation time)
     */
    private void onClockTickListener(EventInfo evt) {
        vmList.forEach(vm ->
            System.out.printf(
                "\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). RAM usage: %.2f%% (%d MB)%n",
                evt.getTime(), vm.getId(), vm.getCpuPercentUtilization()*100.0, vm.getNumberOfPes(),
                vm.getCloudletScheduler().getCloudletExecList().size(),
                vm.getRam().getPercentUtilization()*100, vm.getRam().getAllocatedResource())
        );
    }


    /**
     * 创建cloudlet，从data中读取出总数分批次设置延迟，构造
     */
//    private List<Cloudlet> createAndSubmitCloudletsAndVmsAndBorkers() {
//
//        ReadCsv readCsv = new ReadCsv(0);
//        //读取到全部负载数组
//        int[] totalNumCLOUDLETS = readCsv.getCloudletList();
//        //debug 调试用，取前几个值
//        totalNumCLOUDLETS = Arrays.copyOfRange(totalNumCLOUDLETS, 0, 1);
//        //设置批次延迟
//        int n=0;//批次
//        //for循环负责批次循环
//        //for (int e:totalNumCLOUDLETS) {
    private List<Cloudlet> createCloudlets(int n){
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);
        int submissionDelay = 300*n;    //延迟
        java.util.Random r = new java.util.Random(10);
        for (int i = 0; i < CLOUDLETS; i++) {
            Cloudlet cloudlet =
                //new CloudletSimple(i,1000*i+15000,1)    //CLOUDLET_LENGTH长度不同15000-30000
                new CloudletSimple(i, r.nextInt(20000) + 10000, 1)
                    //.setFileSize(1024)
                    //.setOutputSize(1024)
                    .setUtilizationModel(new UtilizationModelFull());
            cloudlet.setSubmissionDelay(submissionDelay);
            list.add(cloudlet);
        }
        return list;
    }

    private List<Vm> createVms(){
        final List<Vm> list = new ArrayList<>(VMS);
        //todo random长度的Vm 100-300
        java.util.Random R=new java.util.Random(20);

        for (int i = 0; i < VMS; i++) {
            Vm vm =
                new VmSimple( i,R.nextInt(200)+100, 1) //100-300
                    //.setRam(512).setBw(1000).setSize(10000)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }



            //batchMeanTime = brokertemp.meanTime;
            //System.out.printf("batch %d 's mean time is %f",n,batchMeanTime);


            //对比其他算法
//            DatacenterBrokerQlearn brokertemp= new DatacenterBrokerQlearn()simulation);
//            brokers.add(brokertemp);
//            brokertemp.submitVmList(vmList);
//            brokertemp.submitCloudletList(cloudletList);
//            //batchMeanTime = brokertemp.meanTime;
//            //System.out.printf("batch %d 's mean time is %f",n,batchMeanTime);
//            n++;
//            totalCloudletList.addAll(cloudletList);//增加一批到总表




    /**
     * Creates a list of VMs with decreasing number of PEs.
     * The IDs of the VMs aren't defined and will be set when
     * they are submitted to the broker.
     */


    private Host createHost(int id) {
        List<Pe> peList = new ArrayList<>();
        long mips = 4000;
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));
        }
        long ram = 2048; // host memory (Megabyte)
        long storage = 1000000; // host storage (Megabyte)
        long bw = 10000; //Megabits/s

        return new HostSimple(peList)
            //.setRamProvisioner(new ResourceProvisionerSimple())
            //.setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerSpaceShared());

    }


    private DatacenterSimple createDatacenter() {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost(i);
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        DatacenterSimple dc0 = new DatacenterSimple(simulation, hostList,new VmAllocationPolicyFirstFit());
        dc0.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc0;
    }
    private void printResults() {
            for (DatacenterBrokerQlearn broker:brokers) {
                final List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
                finishedCloudlets.sort(Comparator.comparingLong(Cloudlet::getId));
                new CloudletsTableBuilder(finishedCloudlets).build();
                //System.out.println(String.valueOf(broker.meanTime));
                for (int i = 0; i < 3; i++) {
                    System.out.println(String.valueOf(getMeanTime(cloudletList)[i]));
                }


        }
    }

    private double[] getMeanTime(List<Cloudlet> cloudletlist){
        List<Double> t = new ArrayList<Double>();
        for (Cloudlet cl:cloudletlist) {
            t.add(cl.getActualCpuTime());
        }
        double mean = t.stream().reduce(Double::sum).orElse(Double.valueOf(0))/cloudletList.size();
        double max =t.stream().reduce(Double::max).orElse(Double.valueOf(0));
        double min =t.stream().reduce(Double::min).orElse(Double.valueOf(0));
        double[] r=new double[]{mean,max,min};
        return  r;
    }
}
