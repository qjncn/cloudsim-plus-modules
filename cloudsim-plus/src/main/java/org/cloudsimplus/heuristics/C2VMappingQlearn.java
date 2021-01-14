/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.heuristics;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerQlearn;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
//import java.util.List;

import org.python.core.*;
import org.python.util.PythonInterpreter;


/**
 *实现 Qlearn算法，返回
 *
 */
public class C2VMappingQlearn
{


    /** @see #getVmList() */
    private List<Vm> vmList;        //不同broker对象调用的算法vmList值不同

    /** @see #getCloudletList() */  //不同broker对象调用的算法cloudletList值不同
    private List<Cloudlet> cloudletList;

    private int NumOfCloudlet;      //不同broker对象调用的算法NumOfCloudlet值不同
    private int NumOfVM;            //不同broker对象调用的算法NumOfVM值不同
    private double[][] Q;           //不同broker对象调用的算法Q值不同
    Map Q2;
    private double[][] graph;       //不同broker对象调用的算法graph值不同
    private double epsilon = 0.6;     //贪婪因子

    private final double alpha = 0.1;       //学习率 权衡这次和上次学习结果
    private final double gamma = 0.9;       //衰减因子 考虑未来奖励,越大后面权重越高
    private final double  threshold = 0.0001; //收敛停止阀值
    private final int MAX_EPISODES = 10000000; // 一般都通过设置最大迭代次数来控制训练轮数
    private  Map map =  new HashMap();
    private  DatacenterBrokerQlearn broker;

    public C2VMappingQlearn(List<Cloudlet> cloudletList, List<Vm> vmList, DatacenterBrokerQlearn broker) {
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.NumOfCloudlet = cloudletList.size();
        this.NumOfVM = vmList.size();
        this.broker = broker;

        /**
         * 状态空间由C和V组成，C是300秒内所有cloudlet的有序集合，V是300秒内所有VM的有序集合
         *
         */
        /**
         * 构造Q矩阵，默认是0
         */

        this.Q = new double[NumOfCloudlet][NumOfVM];
        //this.Q[NumOfCloudlet] = new double[]{0}; //防止最后一行状态没有办法更新Q值，多一行0值用来更新最后一行Q值
        //随机初始化Q表，最后一行除外,作用不大，迭代次数太多的原因
//        java.util.Random r1=new java.util.Random(1);
//        for (int i = 0; i < NumOfCloudlet; i++) {
//            for (int j = 0; j < NumOfVM; j++) {
//                this.Q[i][j] = r1.nextInt(100) ;
//            }
//        }
        /**
         * 构造Q2矩阵，map字典类型，默认初始为空
         */
        this.Q2=new HashMap();
        /**
         * reward图 先考虑执行时间的负数（或倒数）
         * 执行时间 t定义包括：job产生时刻到处理完之间的时间，其中包括了的处理时间和队列等待时间。
         * 这里为了突出算法，只考虑处理时间。
         * 所以一个任务的执行时间 t = len(cloudlet)/mips(VM)
         * graph[i][j] 表示将第i个cloudlet分配给第j个VM的 执行时间矩阵
         *
         */
        this.graph = new double[NumOfCloudlet][NumOfVM];
        for (int i = 0; i < NumOfCloudlet; i++) {
            for (int j = 0; j < NumOfVM; j++) {
                this.graph[i][j] = this.cloudletList.get(i).getLength() / this.vmList.get(j).getMips();
            }
        }


        /**
         * 设置超参数
         * @param epsilon： 贪婪因子，概率选择 Q规则，或者随机选择动作，防止局部最优？
         * @param alpha：学习率
         * @param gamma：衰减因子
         * @param MAX_EPISODES：最大迭代次数
         */

        //构造double[]变量
        List processList = new ArrayList();
        List QvalueDeltaList = new ArrayList();
        for (int episode = 0; episode < MAX_EPISODES; episode++) {
            double process = (double) 100* episode/MAX_EPISODES;
            //赋值double[]
            processList.add(process);

            System.out.println("第" + episode + "轮训练.....>>>>>>>>>" + process +"%" );
            //新设计了函数epsilon为收敛
            //函数1 开始慢逐渐快速下降，探索面积比较大
            //epsilon = 0.2 * Math.cos(Math.PI/(2*MAX_EPISODES) * episode);
            //函数2 首尾慢，中间快 探索面积比较小
            epsilon = 0.1 * Math.cos(Math.PI/(MAX_EPISODES) * episode) + 0.1;
            System.out.println("本轮贪婪因子衰减为"+epsilon);

            List<Integer> chosenVmID = new ArrayList<Integer>();
            List<Integer> chosenCloudletID = new ArrayList<Integer>();

//            //随机遍历，彻底解决for中的max筛选次序问题
//            Set<Integer> num = new LinkedHashSet<>();
//            Random r2 = new Random();
//            while (num.size() < NumOfCloudlet) {
//                // 生成0-10的数组，观看有无重复值
//                num.add(r2.nextInt(NumOfCloudlet));
//            }
//            Integer[] strs = new Integer[num.size()];
//            num.toArray(strs);
            double QvalueDelta = 0;
            double sum = 0;
            // 遍历集合每一行cloudlet
            for (int i = 0; i < cloudletList.size(); i++) {
                // 到达目标状态，结束循环，进行下一轮训练
                int VmID;
                int CloudID = i;
                //增加行列的存储变量，应对随机行选取，以求map按对应顺序保存字典
                chosenCloudletID.add(i);

                //if (Math.random() < (1 - epsilon))

                if (Math.random() < (1 -epsilon )) {
                    VmID = max(Q[CloudID], chosenVmID); // 通过 Q 表选择动作，即选出Q表中CloudID行中的最大值，返回列号
                }
                else VmID = randomNext(Q[CloudID], chosenVmID); // 随机选择可行动作
                // 更新排除列表状态
                chosenVmID.add(VmID);
                //todo：奖励函数需要设计，倒数太简单，负数不对，负数上面代码VmID不能用max函数选，应该用min
                sum = sum + graph[CloudID][VmID]; // 奖励，是执行时间的倒数，改成累积执行时间和的倒数
                double reward = 1000/sum;
                //reward = 1000/graph[CloudID][VmID]
                //double reward = 50*Math.exp(graph[CloudID][VmID]/100);
                //更新Q表,注意最后那行只能和初始行循环更新
                double QOldValue = Q[CloudID][VmID];
                if (i + 1 < cloudletList.size())
                    Q[CloudID][VmID] = (1 - alpha) * Q[CloudID][VmID] + alpha * (reward + gamma * maxNextQ(Q[i + 1], chosenVmID));
                else Q[CloudID][VmID] = (1 - alpha) * Q[CloudID][VmID] + alpha * (reward + 0);
                //求出各状态和上一次求得状态的最大差值
                QvalueDelta = Math.max(QvalueDelta, Math.abs(QOldValue - Q[CloudID][VmID] ));

            }
            for (int i=0 ;i<NumOfCloudlet;i++){
                map.put(chosenCloudletID.get(i), chosenVmID.get(i));//更新map结果,只要迭代episode最后一次的？
            }
            System.out.println(Arrays.deepToString(Q));
            System.out.println("本轮CloudID和VMID匹配为"+map+"总消耗时间是"+sum);
//            reward2=-sum; //奖励2的函数，总时间的负数或倒数
//            Q2=(1 - alpha) * Q2.get(map) + alpha * (reward + gamma * maxNextQ(Q[i + 1], chosenVmID));
//            Q2.put(map,reward2);//更新Q表
            System.out.println("本轮最大差值是"+QvalueDelta);

            //todo:增加停止阀值
            //求出各状态和上一次求得状态的最大差值
            if (QvalueDelta < threshold) {
                System.out.println("第" + episode + "轮后,所得结果已经收敛,运算结束");
                break;
            }
            //构造double[]赋值
            QvalueDeltaList.add(QvalueDelta);

        }
        //画图
        //draw2D(QvalueDeltaList);
    }

    /**
     * 蓄水池抽样，等概率选择流式数据  改成普通等概率
     * @param is Q表中 ‘CloudID’ 所在行向量Q[CloudID]
     * @return 随机的列号
     */
    private static int randomNext(double[] is,List<Integer> chosenVmID) { //
        int columns  = 0, n = 1;
        for(int i = 0; i < is.length; ++i) {
            if(chosenVmID.contains(i)) continue;
            else if(is[i] >= 0 && Math.random() < 1.0/n++) columns  = i;
        }
        return columns ;
    }
//    private static int randomNext(double[] is,List<Integer> chosenVmID) { //
//        Random r3 = new Random();
//        while(true) {
//            int columns = r3.nextInt(is.length);      //随机生成一个0~length的整数columns
//            if (chosenVmID.contains(columns)) {        //排除已选择的
//                continue;
//            }
//            else return columns;
//        }
//    }



    /**
     * 找出is向量的最大值，并保证候选的 VmID 不属于 已经被选过的 chosenVmID
     * @param is Q表中 ‘CloudID’ 所在行向量 Q[CloudID]
     * @param chosenVmID 已经被选过的 VM列表，无法再被选，用序号表示，int型，范围是 0到 (NumOfVM-1)
     * @return 该行最大的 Q值对应的列号，即 VmID
     */
    private int max(double[] is,List<Integer> chosenVmID) {

        double ismax = -99999;
        int maxid=-99999;
        for(int i = 0; i < is.length; i++) {
            //排除已经被选过的VmID
            if(chosenVmID.contains(i)) {
                continue;
            }
            else if(is[i] > ismax) {
                ismax = is[i];
                maxid=i;
            }
        }
        //return maxid==-2?0:maxid;
        return maxid;
    }

    /**
     * 找出is向量的最大值，写法有待改进
     * @param is Q表中 ‘CloudID’ 所在行向量Q[CloudID]
     * @return Q值
     */
    private  double maxNextQ(double[] is, List<Integer> chosenVmID ) {
        int VmID = max(is, chosenVmID);//下一个动作的最大Q值的VmID
        return is[VmID];
    }



    public List<Vm> getVmList() {
        return vmList;
    }


    public void setVmList(List<Vm> vmList) {
        this.vmList = vmList;
    }


    public List<Cloudlet> getCloudletList() {
        return cloudletList;
    }


    public void setCloudletList(final List<Cloudlet> cloudletList) {
        this.cloudletList = cloudletList;
    }

    /**
     *
     * @param cloudlet
     * @return 返回Q算法中对应的最大Q值的列序号，就是和该行cloudlet匹配的Vm
     *
     */
    public Vm getMappedVm(Cloudlet cloudlet) {
        int cloudId= (int) cloudlet.getId();
        //broker.;
        int vmid = (int) this.map.get(cloudId);
        return vmList.get(vmid);
    }

    /**
     * 取得当前批次经过Q算法优化之后的平均时间
     * @return mean
     */
    public double getMeanTime() {
        List<Double> t = new ArrayList<Double>();
        for (Cloudlet e:cloudletList) {

            t.add(e.getLength()/getMappedVm(e).getMips());
        }
        double mean = t.stream().reduce(Double::sum).orElse(Double.valueOf(0))/cloudletList.size();
        return mean;
    }
//    /**
//     * 画图 https://github.com/qjncn/jmathplot
//     *
//     */
//    public void draw2D(JFrame frame, double[] x, double[] y){
//        // create your PlotPanel (you can use it as a JPanel)
//        Plot2DPanel plot = new Plot2DPanel("b plot panel");
//        frame.setContentPane(plot);
//        // add a line plot to the PlotPanel
//        plot.addLinePlot("my plot", x, y);
//    }

    /**
     * 画图 用Jython调用python https://www.cnblogs.com/wuwuyong/p/10600749.html
     *
     */

    public void draw2D(List a) {
        //添加第三方库matplotlib https://blog.csdn.net/ztf312/article/details/51338060
        PySystemState sys = Py.getSystemState();
        System.out.println(sys.path.toString()); // previous
        sys.path.add("d:\\programdata\\anaconda3\\lib\\site-packages\\");
        System.out.println(sys.path.toString());

        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.execfile("D:\\drawJava.py");

        // 第一个参数为期望获得的函数（变量）的名字，第二个参数为期望返回的对象类型
        PyFunction pyFunction = interpreter.get("drawJava", PyFunction.class);
        //调用函数，如果函数需要参数，在Java中必须先将参数转化为对应的“Python类型”
        pyFunction.__call__(new PyList(a));

    }
    /**
     * 画图 https://github.com/jfree/jfreechart
     * *
     */




}

