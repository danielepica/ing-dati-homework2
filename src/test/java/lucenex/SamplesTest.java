package lucenex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
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

    @Test
    public void testIndexStatistics() throws Exception {
        Path path = Paths.get("target/idx0");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec());
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
            indexDocs(directory, null);
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

        Query query = new TermQuery(new Term("titolo", "Ingegneria"));

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchTQOnStringField() throws Exception {
        Path path = Paths.get("target/idx7");

        Query query = new TermQuery(new Term("data", "12 ottobre 2016"));

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
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
                .add(new Term("contenuto", "data"))
                .add(new Term("contenuto", "scientist"))
                .build();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchPQ2() throws Exception {
        Path path = Paths.get("target/idx4");

        PhraseQuery query = new PhraseQuery(1, "contenuto", "data", "scientist");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

    @Test
    public void testIndexingAndSearchPQWithSlop() throws Exception {
        Path path = Paths.get("target/idx4");

        /*In Lucene, slop è un parametro utilizzato nelle query di tipo PhraseQuery per indicare la massima distanza
        (o "slop") consentita tra i termini della frase.
        La distanza è misurata in numero di posizioni (token) all'interno dell'indice.
        La classe PhraseQuery viene utilizzata per cercare frasi esatte, ovvero frasi in cui i termini compaiono
        nell'ordine specificato nella query e con una determinata distanza massima tra di essi.
        Quindi con slop = 2 significa che tra ogni termine ci possono essere fino a 2 parole diverse */

        PhraseQuery query = new PhraseQuery(2, "contenuto", "laurea", "ingegneria");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
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
                .add(new Term("contenuto", "data"))
                .add(new Term("contenuto", "scientist"))
                .build();

        TermQuery termQuery = new TermQuery(new Term("titolo", "Ingegneria"));

        BooleanQuery query = new BooleanQuery.Builder()
                .add(new BooleanClause(termQuery, BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(phraseQuery, BooleanClause.Occur.SHOULD))
                .build();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
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
        Query query = parser.parse("+ingegneria dei +dati");

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null);
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
        Query query = parser.parse("ingegneria dati data scientist");
        try {
            indexDocs(directory, null);
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
            indexDocs(directory, new SimpleTextCodec());
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

    private void indexDocs(Directory directory, Codec codec) throws IOException {
        Analyzer defaultAnalyzer = new StandardAnalyzer();
        //Creo una lista di stopword, CharArraySet propria di Lucene
        CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di"), true);
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        //qui definisco quali analyzer utilizzare per i vari field
        perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
        perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer());

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
        Document doc1 = new Document();
        doc1.add(new TextField("titolo", "Come diventare un ingegnere dei dati, Data Engineer?", Field.Store.YES));
        doc1.add(new TextField("contenuto", "Sembra che oggigiorno tutti vogliano diventare un Data Scientist  ...", Field.Store.YES));
        doc1.add(new StringField("data", "12 ottobre 2016", Field.Store.YES));

        Document doc2 = new Document();
        doc2.add(new TextField("titolo", "Curriculum Ingegneria dei Dati - Sezione di Informatica e Automazione", Field.Store.YES));
        doc2.add(new TextField("contenuto", "Curriculum. Ingegneria dei Dati. Laurea Magistrale in Ingegneria Informatica ...", Field.Store.YES));

        writer.addDocument(doc1);
        writer.addDocument(doc2);
        //Salvo tutto
        writer.commit();
        //Chiudo tutto
        writer.close();
    }

    private void indexDocs(Directory directory, Codec codec, File file) throws Exception {
        Analyzer defaultAnalyzer = new StandardAnalyzer();
        //Creo una lista di stopword, CharArraySet propria di Lucene
        CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di"), true);
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        //qui definisco quali analyzer utilizzare per i vari field
        perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
        perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer());

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

        for (String chiave : mappa.keySet()) {
            Document doc = new Document();
            doc.add(new TextField("titolo", chiave, Field.Store.YES));
            doc.add(new TextField("contenuto", mappa.get(chiave), Field.Store.YES));
            writer.addDocument(doc);
        }
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

    public Map<String, String> readAllFileTxt(File folder) throws Exception {
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
        /*for(String chiave : allFiles.keySet()){
            System.out.println(chiave);
            System.out.println(allFiles.get(chiave));
        }*/
        return allFiles;
    }

    @Test
    public void testReaderFile() throws Exception {
        Map<String, String> mappa = readAllFileTxt(new File("file/"));
    }

    @Test
    public void testHomework2() throws Exception {
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
    public void testIndexingAndSearchAllHomework2() throws Exception {
        Path path = Paths.get("target/idx3");

        Query query = new MatchAllDocsQuery();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, null, new File("file/"));
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
            } finally {
                directory.close();
            }

        }
    }

}