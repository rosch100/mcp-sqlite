public class TestOS {
    public static void main(String[] args) {
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Name lower: " + System.getProperty("os.name", "").toLowerCase());
        System.out.println("Contains mac: " + System.getProperty("os.name", "").toLowerCase().contains("mac"));
        System.out.println("Contains darwin: " + System.getProperty("os.name", "").toLowerCase().contains("darwin"));
    }
}

