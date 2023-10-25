package lucenex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.tests.analysis.TokenStreamToDot;
import org.junit.Test;

/**
 * Trivial tests for indexing and search in Lucene
 */
public class SamplesTest {


    static CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di", "a", "da", "con", "su", "per", "tra", "fra"), true);


    public static void main(String args[]) throws Exception {
        SamplesTest utilsMethod = new SamplesTest();
        Path path = Paths.get("target/idx5");
        System.out.println("Inserisci prima la parola-chiave tra [titolo, contenuto] + spazio, seguito da una serie di termini che vuoi includere nella tua query per trovare il documento che contiene quei termini.\n" +
                "Esempio: titolo intelligenza \n" +
                "Esempio: titolo \"intelligenza artificiale\" \n" +
                "Inserisci 'chat casuale' per avere un riassunto fatto da un LLM di uno dei documenti presenti nella cartella\n" +
                "Digita 'esc' per uscire");
        Directory directory = FSDirectory.open(path);
        utilsMethod.indexDocs(directory, new SimpleTextCodec(), new File("file/"));

        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        while (true) {
            List<TermQuery> termQueriesList = new ArrayList<>();
            List<String> phraseQueries = new ArrayList<>();
            List<PhraseQuery> phraseQueryList = new ArrayList<>();
            Scanner scanner = new Scanner(System.in);
            String stringa = scanner.nextLine();

            if (stringa.equals("esc")) break;

            String[] fieldAndQuery = stringa.split(" ", 2);
            //controllare se la lunghezza dell'array è > 1 altrimeni Index 1 out of bounds for length
            if (fieldAndQuery.length > 1) {
                String field = fieldAndQuery[0];
                String queryInput = fieldAndQuery[1];

                if ("chat".equals(field) && "casuale".equals(queryInput)) {
                    System.out.println(LLMAPI.chatLLM(utilsMethod.readRandomFileTxt(new File("file/"))));
                } else {
                    //Query phrase?
                    Pattern pattern = Pattern.compile("\"([^\"]*)\"");
                    Matcher matcher = pattern.matcher(queryInput);

                    while (matcher.find()) {
                        String frase = matcher.group(1);
                        phraseQueries.add(frase);
                    }
                    for (String phraseQuery : phraseQueries) {
                        PhraseQuery.Builder phraseQueryBuilder = new PhraseQuery.Builder();
                        for (String valore : phraseQuery.split(" ")) {
                            if (!stopWords.contains(valore)) {
                                phraseQueryBuilder.add(new Term(field, valore.toLowerCase()));
                            }
                        }
                        phraseQueryList.add(phraseQueryBuilder.setSlop(2).build());
                    }

                    //Term query
                    String inputSenzaDobleDot = queryInput.replaceAll("\"([^\"]*)\"", "");
                    String[] singleInput = inputSenzaDobleDot.split("\\s+");

                    for (int i = 0; i < singleInput.length; i++) {
                        if (!singleInput[i].equals("") && !stopWords.contains(singleInput[i])) {
                            termQueriesList.add(new TermQuery(new Term(field, singleInput[i].toLowerCase())));
                        }
                    }
                    BooleanQuery.Builder query = new BooleanQuery.Builder();

                    for (PhraseQuery phraseQuery : phraseQueryList) {
                        query.add(new BooleanClause(phraseQuery, BooleanClause.Occur.MUST));
                    }
                    for (TermQuery termQuery : termQueriesList) {
                        query.add(new BooleanClause(termQuery, BooleanClause.Occur.MUST));
                    }
                    BooleanQuery booleanQuery = query.build();
                    utilsMethod.runQuery(searcher, booleanQuery);

                }
            } else System.out.println("L'input inserito non è valido. Riprova!");

        }
        directory.close();
    }

    @Test
    public void testIndexStatistics() throws Exception {
        Path path = Paths.get("target/idx0");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Collection<String> indexedFields = FieldInfos.getIndexedFields(reader);
                for (String field : indexedFields) {
                    System.out.println(searcher.collectionStatistics(field));
                }
            } finally {
                directory.close();
            }

        }
    }


    @Test
    public void testIndexingAndSearchAll() throws Exception {
        Path path = Paths.get("target/idx3");

        Query query = new MatchAllDocsQuery();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchTQ() throws Exception {
        Path path = Paths.get("target/idx2");

        Query query = new TermQuery(new Term("contenuto", "machine"));

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchPQ() throws Exception {
        Path path = Paths.get("target/idx4");

        PhraseQuery query = new PhraseQuery.Builder()
                .add(new Term("contenuto", "nell'uso"))
                .add(new Term("contenuto", "chatgpt")).setSlop(1)
                .build();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchBQ() throws Exception {
        Path path = Paths.get("target/idx5");

        PhraseQuery phraseQuery = new PhraseQuery.Builder()
                .add(new Term("contenuto", "machine"))
                .add(new Term("contenuto", "learning"))
                .build();

        TermQuery termQuery = new TermQuery(new Term("titolo", "intelligenza"));

        BooleanQuery query = new BooleanQuery.Builder()
                .add(new BooleanClause(termQuery, BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(phraseQuery, BooleanClause.Occur.SHOULD))
                .build();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchQP() throws Exception {
        Path path = Paths.get("target/idx1");

        QueryParser parser = new QueryParser("contenuto", new WhitespaceAnalyzer());
        Query query = parser.parse("+unsupervised +learning");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testRankingWithDifferentSimilarities() throws Exception {
        Path path = Paths.get(Files.createTempDirectory("target").toUri());
        Directory directory = FSDirectory.open(path);

        QueryParser parser = new MultiFieldQueryParser(new String[]{"contenuto", "titolo"}, new WhitespaceAnalyzer());
        Query query = parser.parse("+machine +learning");
        try {
            indexDocs(directory, null, new File("file/"));
            Collection<Similarity> similarities = Arrays.asList(new ClassicSimilarity(), new BM25Similarity(2.5f, 0.2f),
                    new LMJelinekMercerSimilarity(0.1f));
            for (Similarity similarity : similarities) {
                try (IndexReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    searcher.setSimilarity(similarity);
                    System.err.println("Using " + similarity);
                    runQuery(searcher, query, true);
                }
            }

        } finally {
            directory.close();
        }
    }

    @Test
    public void testIndexingAndSearchAllWithCodec() throws Exception {
        Path path = Paths.get("target/idx6");

        Query query = new MatchAllDocsQuery();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec(), new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    private void runQuery(IndexSearcher searcher, Query query) throws IOException {
        runQuery(searcher, query, false);
    }

    private void runQuery(IndexSearcher searcher, Query query, boolean explain) throws IOException {
        TopDocs hits = searcher.search(query, 10);
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println("doc" + scoreDoc.doc + ":" + doc.get("titolo") + " (" + scoreDoc.score + ")");
            if (explain) {
                Explanation explanation = searcher.explain(query, scoreDoc.doc);
                System.out.println(explanation);
            }
        }
    }

    private void indexDocs(Directory directory, Codec codec, File file) throws Exception {
        Analyzer defaultAnalyzer = new StandardAnalyzer();
        //Creo una lista di stopword, CharArraySet propria di Lucene
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        //qui definisco quali analyzer utilizzare per i vari field
        perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
        perFieldAnalyzers.put("titolo", new SimpleAnalyzer());

        //Il primo viene usato come default se non specifico il campo da interrogare
        Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);

        //Definizione Indice di scrittura
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        //Se voglio sapere tutto quello che viene scritto nell'indice uso new SimpleTextCodec()
        if (codec != null) {
            config.setCodec(codec);
        }
        IndexWriter writer = new IndexWriter(directory, config);

        //Mi serve perchè faccio vari test
        writer.deleteAll();

        //Ora devo leggere leggere i file contenuti nella mia cartella
        Map<String, String> mappa = readAllFileTxt(file);

        long startTime = System.currentTimeMillis();
        for (String chiave : mappa.keySet()) {
            Document doc = new Document();
            doc.add(new TextField("titolo", chiave, Field.Store.YES));
            doc.add(new TextField("contenuto", mappa.get(chiave), Field.Store.YES));
            writer.addDocument(doc);
        }
        long endTime = System.currentTimeMillis();
        // Stampa il tempo di indicizzazione
        System.out.println("Tempo di indicizzazione: " + (endTime - startTime) + " millisecondi");
        //Salvo tutto
        writer.commit();
        //Chiudo tutto
        writer.close();
    }

    @Test
    public void testAnalyzer() throws Exception {
        CharArraySet stopWords = new CharArraySet(Arrays.asList("di", "a", "da", "dei", "il", "la"), true);
        Analyzer a = new StandardAnalyzer(stopWords);
        TokenStream ts = a.tokenStream(null, "Come diventare un ingegnere dei dati,");
        StringWriter w = new StringWriter();
        new TokenStreamToDot(null, ts, new PrintWriter(w)).toDot();
        System.out.println(w);
    }

    private Map<String, String> readAllFileTxt(File folder) throws Exception {
        Map<String, String> allFiles = new HashMap<>();
        for (File file : folder.listFiles()) {
            String fileName = file.getName();
            // Extract the extension from the file name
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                String name = fileName.substring(0, index);
                String extension = fileName.substring(index + 1);
                if (extension.equals("txt")) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String st;
                    StringBuilder str = new StringBuilder();
                    while ((st = br.readLine()) != null) {
                        str.append(st);
                    }
                    allFiles.put(name, str.toString());
                }
            }

        }
        return allFiles;
    }

    private String readRandomFileTxt(File folder) throws Exception {
        // Ottieni una lista di file e mescolala
        File[] files = folder.listFiles();
        Collections.shuffle(Arrays.asList(files));

        // Crea uno stream dalla lista mescolata e trova qualsiasi file
        Optional<File> anyFile = Arrays.stream(files).findAny();
        if (anyFile.isPresent()) {
            File file = anyFile.get();
            String fileName = file.getName();
            // Extract the extension from the file name
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                String name = fileName.substring(0, index);
                String extension = fileName.substring(index + 1);
                if (extension.equals("txt")) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String st;
                    StringBuilder str = new StringBuilder();
                    while ((st = br.readLine()) != null) {
                        str.append(st);
                    }
                    return str.toString();
                }
            }

        }
        return null;
    }


}
