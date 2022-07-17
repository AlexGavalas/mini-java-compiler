class test {
    public static void main(String[] args) {
        System.out.println(new A().foo());
    }
}

class A {

    public int foo() {
        int[] b;
        b = new int[10];
        b[0] = 4;
        return b[0];
    }
}