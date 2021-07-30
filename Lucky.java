import java.util.Random;
import java.util.Scanner;

public class Lucky {
    static String name = null;
    static String pass = null;
    static int card = -1;
    static boolean flag = false;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Random random = new Random();
        int userSelect = input.nextInt();
        String userInput = null;//全局变量（成员变量）不用赋默认值，局部变量必须赋默认值。

        do{
            //打印菜单
            menu();

            //用户选择功能
            switch (userSelect) {
                //注册功能
                case 1:
                    signUp(random, input);
                    break;
                //登录功能
                case 2:
                    logIn(input);
                    break;
                //抽奖功能
               /* case 3:
                   luckyDraw(random);
                    break;*/
                default:
                    System.out.println("输入错误！");
                    break;
            }
            System.out.print("继续吗？(Y/N)");
            userInput = input.next();
        }while(("y").equalsIgnoreCase(userInput));
        input.close();
    }

    //菜单
    public static void menu() {
        System.out.println("**********************");
        System.out.println("1、注册");
        System.out.println("2、登录");
        System.out.println("3、抽奖");
        System.out.println("**********************");
        System.out.print("请选择菜单：");
    }

    // 注册
    public static void signUp(Random random,Scanner input) {
        System.out.println("[系统 > 注册]");
        card = random.nextInt(10000);
        String[] name =new String[100];
        String[] pass=new String[100];
        for (int i = 0; i < name.length; i++) {
            System.out.println("输入用户名：");
            name[i]=input.next();
            for (int j = 0; j < pass.length; j++) {
                System.out.println("输入密码：");
                pass[j]=input.next();
            }
        }



        System.out.println("注册成功！");
        System.out.println("用户名\t密码\t卡号");
        System.out.println(name + "\t" + pass + "\t" + card);

    }


     
   
    //denglulu
    // 登录
    public static void logIn(Scanner input) {
        System.out.println("[系统 > 登录]");
        for (int i = 0; i < 3; i++) {
            System.out.print("用户名: ");
            String userName = input.next();
            System.out.print("密码: ");
            String password = input.next();
            if (userName.equals(name) && password.equals(pass)) {
                // 登录成功
                System.out.println("欢迎：" + userName);
                flag = true;
                break;
            } else {
                // 登录失败
                System.out.println("登录失败！还有" + (2 - i) + "次机会");
            }
        }
        // 成功 ｜｜ 三次都失败
        if (!flag) {
            System.out.println("错误三次，退出系统！");
            System.exit(0);
        }
    }

    // 抽奖
    /*
    public static void luckyDraw(Random random) {
        System.out.println("[系统 > 抽奖]");
        int[] card=new int[100];
        card[100] = random.nextInt(10000);
        if (flag) {
            System.out.println("你的卡号是：" + card);
                        int card1 = random.nextInt(10);  
                        int card2 = random.nextInt(10);  
                        int card3 = random.nextInt(10);  
                        int card4 = random.nextInt(10);  
                        int card5 = random.nextInt(10); 
                        System.out.println("今日幸运数字是：" + card1 + "\t" + card2 + "\t" + card3 + "\t" + card4  + "\t" + card5);
                        if(card == card1 || card == card2 || card == card3 ||card == card4 ||card == card5){
                            System.out.println("恭喜你！");
                        }else{
                            System.out.println("sorry！");
                        }
                    }else{
                        System.out.println("请先登录！");
                    }*/

}
