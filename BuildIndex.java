import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildIndex {
    public static void main(String[] args) {
        String corpusFilePath = args[0];

        // Extract file name from the full path
        String fileName = new File(corpusFilePath).getName();

        // Append .ser to the file name
        String outputFileName = fileName.replace(".txt", ".ser");
       
        try {
            FileReader fileReader = new FileReader(corpusFilePath);
            BufferedReader bufferReader = new BufferedReader(fileReader);
            String line = null;
            int count = 0;
            List<List<String>> docs = new ArrayList<>();
            
            List<String> doc = new ArrayList<>();
            while ((line = bufferReader.readLine()) != null) {
                String processed = line.replaceAll("[^a-zA-Z]", " ").toLowerCase();
                doc.addAll(Arrays.asList(processed.trim().split("\\s+")));
                count++;
                if (count == 5) {
                    docs.add(doc);
                    doc = new ArrayList<>();
                    count = 0;
                }
            }
            bufferReader.close();

            Indexer idx = new Indexer();
            idx.docs = docs;
            FileOutputStream fos = new FileOutputStream(outputFileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(idx);
            
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
