package org.cloudsimplus.examples.brokers.Qlearning;

import com.csvreader.CsvReader;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * java读取目录下所有csv文件数据，存入二维数组并返回
 * 用 JavaCSV的API 说明文档：http://javacsv.sourceforge.net/
 * JavaCSV官网：https://sourceforge.net/projects/javacsv/
 * @param: totalCloudlets  data中指定某列的全部负载时间序列
 * @args：
 */

public class ReadCsv {
    private static String filePath = "D:\\Backup\\Downloads\\data_new1.csv";
    public static ArrayList<Integer> totalCloudlets=new ArrayList<>();

    public static void main(String[] args) {
        //读取0数据中心的负载，利用构造函数
        ReadCsv readCsv=new ReadCsv(0);
        readCsv.getCloudletList();

    }

    /**
     * 构造函数
     * 读取csv文件中的某列的每一行str，再转换成int
     * @param column
     *
     */
    public ReadCsv(int column){
        try {
            // 创建CSV读对象
            CsvReader csvReader = new CsvReader(filePath);

            // 不读表头，注释掉
            //csvReader.readHeaders();
            while (csvReader.readRecord()) {
                // 读取一行原始数据
                //System.out.println(csvReader.getRawRecord());
                //csvReader.getRawRecord();
                // 读该行的某一列,打印出来，不是必要的，用于调试
                //System.out.println(csvReader.get(column));
                int temp = -1;
                try {
                    //get函数是取得该colum的当前行，通过while循环readRecord方法继续读取该列下一行，
                    // 再将str类型的转换成integer类
                    temp = Integer.valueOf(csvReader.get(column)).intValue();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                //intenger方法add添加int值
                totalCloudlets.add(temp);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 按照模拟时间执行总负载的提取
     * 将List<intenger>转换成int[]
     * 参考：https://www.cnblogs.com/chcha1/p/10883068.html
     *  @return 返回当前时刻总的负载数
     */
    public int[] getCloudletList(){
        //将List<intenger>转换成int[]
        int[] tmp = totalCloudlets.stream().mapToInt(Integer::valueOf).toArray();
        System.out.println(Arrays.toString(tmp) );
        return tmp;
    }
}
