import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class BankServer {

    private static final Map<String, String[]> balanceFile = new HashMap<>();

    public static PublicKey loadPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        String publicKeyPEM = new String(keyBytes)
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decodedBytes = Base64.getDecoder().decode(publicKeyPEM);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        String privateKeyPEM = new String(keyBytes)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decodedBytes = Base64.getDecoder().decode(privateKeyPEM);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        if (port < 1024 || port > 65535) {
            System.err.println("•  Invalid port number. Port must be between 1024 and 65535.");
            System.exit(1);
        }

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started. Waiting for ATM...");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("    ATM has been successfully connected." + '\n');

                String publicKeyFile = "public_key.pem";
                String privateKeyFile = "private_key.pem";

                PublicKey publicKey = loadPublicKey(publicKeyFile);
                PrivateKey privateKey = loadPrivateKey(privateKeyFile);

                ObjectOutputStream outToClient = new ObjectOutputStream(clientSocket.getOutputStream());
                outToClient.writeObject(publicKey);
                outToClient.flush();

                Thread clientThread = new Thread(() -> {
                    try {
                        while (true) {
                            loadBalanceFile();
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                            String encryptedData = in.readLine();
                            String[] encrypted = encryptedData.split(" ");
                            byte[] decryptedSymmetricKey = decryptingSymmetricKey(encrypted[0], privateKey);
                            SecretKey symmetricKey = new SecretKeySpec(decryptedSymmetricKey, 0,
                                    decryptedSymmetricKey.length, "AES");
                            String decryptedIDPassword = decryptingIDPassword(symmetricKey, encrypted[1]);

                            String userData[] = decryptedIDPassword.split(" ");

                            if (infoExist(userData[0], userData[1])) {
                                out.println("1");
                                System.out.println("    User has been login successfully.\n");
                                boolean op = true;

                                while (op) {
                                    String option = in.readLine();

                                    if (option.equals("1")) {
                                        String data = in.readLine();
                                        String[] splitData = data.split(" ");
                                        if (balanceFile.containsKey(splitData[0])) {
                                            boolean val = transferMoney(userData[0], splitData[0], splitData[1],
                                                    splitData[2]);
                                            out.println(val);
                                        } else {
                                            out.println("   User doesn't exist.");
                                        }
                                    } else if (option.equals("2")) {
                                        String balance = checkBalance(userData[0]);
                                        out.println(balance);
                                    } else {
                                        clientSocket.close();
                                    }
                                }
                            } else {
                                out.println("0");
                                System.out.println("    Failed to login.\n");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("    ATM closed ABRUPTLY\n");
                    }
                });
                clientThread.start();
            } catch (IOException e) {
                System.out.println("    ATM closed ABRUPTLY\n");
            }
        }
    }

    private static byte[] decryptingSymmetricKey(String encryptedString, Key privateKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encryptedSymmetricKey = Base64.getDecoder().decode(encryptedString);
        byte[] decryptedSymmetricKey = rsaCipher.doFinal(encryptedSymmetricKey);
        return decryptedSymmetricKey;
    }

    private static String decryptingIDPassword(SecretKey symmetricKey, String encryptedIDPasswordStr) throws Exception {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, symmetricKey);
        byte[] encryptedIDPassword = Base64.getDecoder().decode(encryptedIDPasswordStr);
        byte[] decryptedIDPassword = aesCipher.doFinal(encryptedIDPassword);
        return new String(decryptedIDPassword, "UTF8");
    }

    static boolean infoExist(String id, String password) throws IOException {
        try (FileReader reader = new FileReader("password"); Scanner myReader = new Scanner(reader)) {
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] splited = data.split(" ");
                if (splited[0].equals(id) && splited[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String checkBalance(String userId) throws IOException {
        if (balanceFile.containsKey(userId)) {
            String[] userBalances = balanceFile.get(userId);
            String balance = userBalances[0] + " " + userBalances[1];
            return balance;
        }
        return null;
    }

    private static boolean transferMoney(String userId, String recipientId, String transferAmount, String choice) {
        String[] userBalances = balanceFile.get(userId);
        String[] recipientbalance = balanceFile.get(recipientId);
        if (choice.equals("1") && (Integer.parseInt(transferAmount) <= Integer.parseInt(userBalances[0]))) {
            userBalances[0] = String.valueOf(Integer.parseInt(userBalances[0]) - Integer.parseInt(transferAmount));
            recipientbalance[0] = String.valueOf(Integer.parseInt(recipientbalance[0]) + Integer.parseInt(transferAmount));
            balanceFile.put(userId, userBalances);
            balanceFile.put(recipientId, recipientbalance);
            writeBalanceFileData(balanceFile, "balance");
            return true;
        } else if (choice.equals("2") && (Integer.parseInt(transferAmount) <= Integer.parseInt(userBalances[1]))) {
            userBalances[1] = String.valueOf(Integer.parseInt(userBalances[1]) - Integer.parseInt(transferAmount));
            recipientbalance[1] = String.valueOf(Integer.parseInt(recipientbalance[1]) + Integer.parseInt(transferAmount));
            balanceFile.put(userId, userBalances);
            balanceFile.put(recipientId, recipientbalance);
            writeBalanceFileData(balanceFile, "balance");
            return true;
        }
        return false;
    }

    private static void writeBalanceFileData(Map<String, String[]> balanceFileData, String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (Map.Entry<String, String[]> entry : balanceFileData.entrySet()) {
                String userId = entry.getKey();
                String[] balances = entry.getValue();
                writer.println(userId + " " + balances[0] + " " + balances[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void loadBalanceFile() {
        try {
            File file = new File("balance");
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\\s+"); 

                if (parts.length >= 3) {
                    String userId = parts[0];
                    String[] balances = new String[]{parts[1], parts[2]};
                    balanceFile.put(userId, balances);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
