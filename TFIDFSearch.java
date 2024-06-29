import java.io.*;
import java.util.*;

public class TFIDFSearch {
    static List<Trie> tries = new ArrayList<>();
    static TotalTrie total_trie = new TotalTrie();

    public static void main(String[] args) {
        String corpusFile = args[0] + ".ser";
        String testCaseFilePath = args[1];

        File outputFile = new File("output.txt");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        try {
            FileInputStream fis = new FileInputStream(corpusFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<List<String>> docs = ((Indexer) ois.readObject()).docs;
            ois.close();
            fis.close();

            Trie trie = new Trie();
            for (int docID = 0; docID < docs.size(); docID++) {
                for (String word : docs.get(docID)) {
                    if (trie.insert(word) == true) {
                        total_trie.insert(word, docID);
                    }
                }
                tries.add(trie);
                trie = new Trie();
            }
            FileReader fileReader = new FileReader(testCaseFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            int n = Integer.parseInt(bufferedReader.readLine().trim());
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                runQuery(n, line);
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
    }

    public static void runQuery(int n, String queryWords) {
        QueryOperation operation = queryWords.contains("AND") ? QueryOperation.AND : QueryOperation.OR;
        queryWords = queryWords.replace("AND", "").replace("OR", "");
        List<String> words = Arrays.asList(queryWords.split("\\s+"));
        Set<Integer> relatedDocIDs = DocFilter.filter(operation, words, total_trie);
        HashMap<Integer, Double> answerHashmap = new HashMap<>();

        for (Integer relatedID : relatedDocIDs) {
            Double queryWordTfIdfTotal = 0.0;
            for (String word : words) {
                queryWordTfIdfTotal += Tool.tfIdfCalculate(word, tries.get(relatedID), tries, total_trie);
            }
            answerHashmap.put(relatedID, queryWordTfIdfTotal);
        }

        List<Map.Entry<Integer, Double>> list = new ArrayList<>(answerHashmap.entrySet());

        list.sort(Map.Entry.<Integer, Double>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        
        String answerString = "";
        // for(int i=0;i< list.size();i++){
        //     System.out.println(list.get(i));
        // }
        for (int i = 0; i < n; i++) {
            answerString += String.valueOf(i < list.size() ? list.get(i).getKey() : -1) + " ";
        }
        Tool.WriteFile(answerString + "\n");
    }
}

enum QueryOperation {
    AND, OR;
}

class DocFilter {
    public static Set<Integer> filter(QueryOperation operation, List<String> words, TotalTrie totalTrie) {

        Set<Integer> relatedDocIDs = new HashSet<>();

        if (operation == QueryOperation.OR) {
            for (String word : words) {
                relatedDocIDs.addAll(totalTrie.getDocIDs(word));
            }
        } else {
            relatedDocIDs.addAll(totalTrie.getDocIDs(words.get(0)));
            for (String word : words) {
                relatedDocIDs.retainAll(totalTrie.getDocIDs(word));
            }
        }

        return relatedDocIDs;
    }
}

class TotalTrieNode {
    Set<Integer> wordInDocIDs = new HashSet<Integer>();
    TotalTrieNode[] children = new TotalTrieNode[26];
    int count = 0;
}

class TotalTrie {
    TotalTrieNode root = new TotalTrieNode();

    public void insert(String word, Integer wordInDocID) {
        TotalTrieNode node = root;
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) {
                node.children[c - 'a'] = new TotalTrieNode();
            }
            node = node.children[c - 'a'];
        }
        node.count++;
        node.wordInDocIDs.add(wordInDocID);
    }

    public int countWord(String word) {
        TotalTrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return 0;
            }
            node = node.children[index];
        }
        return node.count;
    }

    public Set<Integer> getDocIDs(String word) {
        TotalTrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return new HashSet<>();
            }
            node = node.children[index];
        }
        return node.wordInDocIDs;
    }
}

class TrieNode {
    TrieNode[] children = new TrieNode[26];
    int count = 0;
}

class Trie {
    TrieNode root = new TrieNode();
    private int totalWordsInserted = 0;

    // 插入一個單詞到 Trie
    public boolean insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) {
                node.children[c - 'a'] = new TrieNode();
            }
            node = node.children[c - 'a'];
        }
        totalWordsInserted++;
        node.count++;
        if (node.count == 1) {
            return true; // 該字串沒被insert過，回傳true
        }
        return false; // 該字串已被insert過
    }

    public int totalWordsInserted() {
        return totalWordsInserted;
    }

    public int countWord(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return 0;
            }
            node = node.children[index];
        }
        return node.count;
    }
}

class Tool {
    public static void WriteFile(String term) {

        try {

            File file = new File("./output.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(term);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static double tf(Trie trie, String word) {
        int number_term_in_doc = trie.countWord(word);
        int total = trie.totalWordsInserted();
        return (double) number_term_in_doc / total;
    }

    public static double idf(List<Trie> tries, TotalTrie total_trie, String word) {
        int number_doc_contain_term = total_trie.countWord(word);
        if(number_doc_contain_term == 0) return 0;
        return Math.log((double) tries.size() / number_doc_contain_term);
    }

    public static double tfIdfCalculate(String word, Trie docTrie, List<Trie> tries, TotalTrie total_trie) {
        return tf(docTrie, word) * idf(tries, total_trie, word);
    }
}
